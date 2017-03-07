package org.inchain.consensus;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.core.DataSynchronizeHandler;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.crypto.Sha256Hash;
import org.inchain.filter.InventoryFilter;
import org.inchain.kits.PeerKit;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryMessage;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.business.RegConsensusTransaction;
import org.inchain.transaction.business.RemConsensusTransaction;
import org.inchain.utils.ByteArrayTool;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 信用共识会议
 * @author ln
 *
 */
@Service
public class CarditConsensusMeeting implements ConsensusMeeting {

	private static final Logger log = LoggerFactory.getLogger(CarditConsensusMeeting.class);
	
	/** 会议状态 **/
	/** 等待就绪 **/
	public final static int MEETING_STATUS_WAIT_READY = 1;
	/** 就绪完成，等待开始 **/
	public final static int MEETING_STATUS_WAIT_BEGIN = 2;
	/** 共识中 **/
	public final static int MEETING_STATUS_CONSENSUS = 3;
	/** 共识中，接受下一轮选举 **/
	public final static int MEETING_STATUS_CONSENSUS_WAIT_NEXT = 4;
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private Mining mining;
	@Autowired
	private InventoryFilter filter;
	@Autowired
	private ConsensusPool consensusPool;
	@Autowired
	private DataSynchronizeHandler dataSynchronizeHandler;
	@Autowired
	private BlockStoreProvider blockStoreProvider;

	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	
	//所有消息队列
	private static Map<Sha256Hash, ConsensusMessage> messages = new HashMap<Sha256Hash, ConsensusMessage>();
		
	private Lock messageLocker = new ReentrantLock();

	//当前共识账号
	private Account account;
	//当前轮共识信息
	private MeetingItem currentMetting;
	//上一轮共识信息
	private MeetingItem previousMetting;

	private boolean packageing;
	//共识调度器状态，0等待初始化，1初始化中，2初始化成功，共识中，3初始化失败
	private int meetingStatus = 0;

	@Override
	public void startSyn() {
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					meeting();
				} catch (Exception e) {
					log.error("共识会议出错", e);
				}
			}
		}, 1, 1, TimeUnit.SECONDS);		
	}


	int count = 0;
	
	/**
	 * 共识会议
	 */
	protected void meeting() {
		count++;
		if(count % 10 == 0) {
			String hash160 = null;
			if(account != null) {
				hash160 = Hex.encode(account.getAddress().getHash160());
			}
			log.info("bestHeight {}, meetingStatus : {}, hash160 : {}, currentMetting : {}", getLocalBestBlockHeight(), meetingStatus, hash160, currentMetting);
		}
		if(meetingStatus == 0 || meetingStatus == 3) {
			if(meetingStatus == 0) {
				meetingStatus = 1;
				init();
			}
			return;
		} else if(meetingStatus != 2 || currentMetting == null) {
			if(meetingStatus == 1 && currentMetting != null) {
				meetingStatus = 2;
			}
			return;
		}
		
		doMeeting();
	}

	/**
	 * 初始化，拉取当前的共识状态
	 */
	protected void init() {
		
		//分析当前共识状态
		BlockHeader bestBlockHeader = blockStoreProvider.getBestBlockHeader().getBlockHeader();
		
		//获取当前的共识段开始点
		long startPoint = bestBlockHeader.getPeriodStartPoint();
		
		//获取开始时的共识列表
		//利用当前的快照倒推
		List<ConsensusAccount> consensusList = consensusPool.listSnapshots();
		analysisSnapshotsByStartPoint(startPoint, bestBlockHeader, consensusList);
		
		currentMetting = new MeetingItem(this, startPoint, consensusList);
		
		BlockHeaderStore startBlockHeader = blockStoreProvider.getHeaderByHeight(startPoint - 1);
		if(startBlockHeader == null) {
			return;
		}
		log.info(" start block {} {} {} {}", startBlockHeader.getBlockHeader().getHeight(), startBlockHeader.getBlockHeader().getPeriodCount(), startBlockHeader.getBlockHeader().getTimePeriod(), startBlockHeader.getBlockHeader().getTime());
		currentMetting.startConsensus(startBlockHeader.getBlockHeader().getTime() + Configure.BLOCK_GEN__MILLISECOND_TIME * (startBlockHeader.getBlockHeader().getPeriodCount() - startBlockHeader.getBlockHeader().getTimePeriod() - 1));
		
//		//拉取一次共识状态，拉取后的信息会通过consensusMeeting.receiveMeetingMessage接收
//		long height = getLocalBestBlockHeight();
//		
//		//content格式第一位为type,1为拉取共识状态信息
//		byte[] content = new byte[] { 1 };
//		
//		ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), height, content);
//		//签名共识消息
//		message.sign(account);
//		broadcastMessage(message);
//		log.info(" send message {} ", message);
	}

	/**
	 * 分析这个时间段的共识进出
	 * @param startPoint
	 * @param endBlock
	 * @param consensusList 
	 */
	public void analysisSnapshotsByStartPoint(long startPoint, BlockHeader endBlock, List<ConsensusAccount> consensusList) {
		
		//
		byte[] endBlockHashBytes = endBlock.getHash().getBytes();
		while(true) {
			BlockStore blockStore = blockStoreProvider.getBlock(endBlockHashBytes);
			if(blockStore == null || blockStore.getBlock() == null) {
				break;
			}
			Block block = blockStore.getBlock();
			
			List<Transaction> txList = block.getTxs();
			for (Transaction transaction : txList) {
				//共识的注册与退出
				if(transaction.getType() == Definition.TYPE_REG_CONSENSUS) {
					//注册新的，那么删除掉
					RegConsensusTransaction regTx = (RegConsensusTransaction) transaction;
					Iterator<ConsensusAccount> it = consensusList.iterator();
					while(it.hasNext()) {
						ConsensusAccount consensusAccount = it.next();
						if(Arrays.equals(regTx.getHash160(), consensusAccount.getHash160())) {
							it.remove();
							break;
						}
					}
				} else if(transaction.getType() == Definition.TYPE_REM_CONSENSUS) {
					//删除掉的，新增进去
					RemConsensusTransaction remTx = (RemConsensusTransaction) transaction;
					if(remTx.isCertAccount()) {
						//认证账户 TODO
						
					} else {
						consensusList.add(new ConsensusAccount(remTx.getHash160(), new byte[][] {remTx.getPubkey()}));
					}
				}
			}
			
			if(block.getHeight() == startPoint) {
				break;
			} else {
				endBlockHashBytes = block.getPreHash().getBytes();
			}
			
		}
		//排序
		consensusList.sort(new Comparator<ConsensusAccount>() {
			@Override
			public int compare(ConsensusAccount o1, ConsensusAccount o2) {
				return o1.getHash160Hex().compareTo(o2.getHash160Hex());
			}
		});
	}

	/*
	 * 打包数据
	 */
	private void doMeeting() {
		if(currentMetting.canPackage()) {
			//当前轮到我打包，异步打包数据
			new Thread() {
				public void run() {
					doPackage();
				}
			}.start();
		} else {
			//是否可以结束了
			//先检查高度
			BlockHeaderStore bestBlockHeaderStore = network.getBestBlockHeader();
			
			if(bestBlockHeaderStore == null || bestBlockHeaderStore.getBlockHeader() == null ||
					bestBlockHeaderStore.getBlockHeader().getTimePeriod() < currentMetting.getConsensusList().size()) {
				if(!currentMetting.canEnd()) {
					//再检查时间
					return;
				}
			}
			
			try {
				previousMetting = currentMetting.clone();
			} catch (CloneNotSupportedException e) {
				log.error("备份当前轮次信息失败" , e);
			}
			
			long newStartTime = bestBlockHeaderStore.getBlockHeader().getTime();
			int diff = previousMetting.getConsensusList().size() - bestBlockHeaderStore.getBlockHeader().getTimePeriod() -1;
			if(diff > 0) {
				newStartTime += diff * Configure.BLOCK_GEN__MILLISECOND_TIME;
			}
			
			currentMetting = new MeetingItem(this, bestBlockHeaderStore.getBlockHeader().getHeight() + 1, consensusPool.listSnapshots());
			
			if((TimeService.currentTimeMillis() - newStartTime) / Configure.BLOCK_GEN__MILLISECOND_TIME >= currentMetting.getConsensusList().size()) {
				currentMetting.startConsensus();
			} else {
				currentMetting.startConsensus(newStartTime);
			}
			
			log.info("一轮结束，切换新一轮共识 , 开始时间 {} , 结束时间 {} , 我的时间 {}", DateUtil.convertDate(new Date(currentMetting.getStartTime())), DateUtil.convertDate(new Date(currentMetting.getEndTime())), DateUtil.convertDate(new Date(currentMetting.getMyPackageTime())));
			
			//当前不该我打包，但是我应该监控当前的状态，如果超时或者出现分叉，应及时处理
			//currentMetting.monitor(localBestBlockHeight);
		}
	}
	
	/**
	 * 打包数据出新块
	 */
	public void doPackage() {
		if(packageing) {
			return;
		}
		packageing = true;
		
		try {
			mining.mining(currentMetting.getTimePeriod(), currentMetting.getConsensusList().size());
		} catch (Exception e) {
			log.error("mining err", e);
		}
		
		packageing = false;
	}
	
	/**
	 * 广播会议消息
	 */
	@Override
	public void broadcastMessage(ConsensusMessage consensusMessage) {
		//除拉取共识状态之外的消息都使用inv广播
		byte[] content = consensusMessage.getContent();
		if(content[0] == 1 || content[0] == 2) {
			peerKit.broadcastMessage(consensusMessage);
		} else {
			Sha256Hash id = consensusMessage.getId();
			if(messages.get(id) == null) {
				receiveMeetingMessage(id, consensusMessage);
				//过滤该条消息的inv，因为本地发出的，没必要再下载
				filter.insert(id.getBytes());
			}
			InventoryMessage message = new InventoryMessage(network, new InventoryItem(InventoryItem.Type.Consensus, id));
			peerKit.broadcastMessage(message);
		}
	}
	
	/**
	 * 接收会议消息
	 */
	@Override
	public void receiveMeetingMessage(Sha256Hash msid, ConsensusMessage message) {
		if(msid == null) {
			return;
		}
		//如果已经接收，则不处理
		if(messageHasReceived(msid)) {
			return;
		}
		if(log.isDebugEnabled()) {
			log.debug("receive consensus message : {}", message);
		}
		
		messages.put(msid, message);

		//如果是准备就绪消息，则放入准备就绪队列
		byte[] bodys = message.getContent();
		byte type = bodys[0];
		
		if(type == 1) {
			//拉取最新共识状态
			processSendMeetingStatus(message);
		} else if(type == 2) {
			//接收最新共识状态回应
			processReceiveMeetingStatus(message);
		}
	}
	
	/*
	 * 其它节点需要拉取最新共识状态，把自己的最新状态回应过去
	 */
	private void processSendMeetingStatus(ConsensusMessage message) {
		messageLocker.lock();
		try {
			//当前是否最新
			if(!dataSynchronizeHandler.hasComplete()) {
				return;
			}
			
			//本地最新高度
			long localBestBlockHeight = getLocalBestBlockHeight();
			
			ByteArrayTool byteArray = new ByteArrayTool();
			byteArray.append(2);
			
			if(currentMetting == null) {
				//当前轮还没有开始
				currentMetting = new MeetingItem(this, localBestBlockHeight, consensusPool.listSnapshots());
				currentMetting.setStatus(MEETING_STATUS_WAIT_READY);
				
			}
			byteArray.append(currentMetting.serialize());
			
			ConsensusMessage consensusMessage = new ConsensusMessage(message.getNetwork(), account.getAddress().getHash160(), localBestBlockHeight, byteArray.toArray());
			consensusMessage.sign(account);
			
			message.getPeer().sendMessage(consensusMessage);
		} finally {
			messageLocker.unlock();
		}
	}
	
	/*
	 * 发送拉取最新共识状态消息之后，这里接收相应的回应，处理好之后把共识状态置为准备就绪状态
	 */
	private void processReceiveMeetingStatus(ConsensusMessage message) {
		
		messageLocker.lock();
		try {
//			if(meetingStatus == 2) {
//				return;
//			}
			
			byte[] content = message.getContent();
			if(currentMetting == null) {
				currentMetting = new MeetingItem(this, getLocalBestBlockHeight(), consensusPool.listSnapshots());
			}
			currentMetting.parse(content, 1);
			
			if(meetingStatus == 1) {
				currentMetting.startConsensus();
				meetingStatus = 2;
			}
			
		} finally {
			messageLocker.unlock();
		}
	}

	
	/**
	 * 获取本地区块最新高度
	 */
	public long getLocalBestBlockHeight() {
		return network.getBestBlockHeight();
	}
	
	/**
	 * 获取本轮和上一轮超时的账号
	 */
	public Set<ConsensusAccount> getTimeoutList() {
		
		Set<ConsensusAccount> timeoutList = new HashSet<ConsensusAccount>();
		
		if(previousMetting != null) {
			//上一轮的是否已处理
			if(previousMetting.getTimeoutList() == null) {
				
				List<ConsensusAccount> list = previousMetting.getConsensusList();
				int allCount = list.size();
				
				long startHeight = previousMetting.getStartHeight();

				BlockHeader lastHeader = null;
				
				for (int i = 0; i < allCount; i++) {
					ConsensusAccount cus = list.get(i);
					
					if(lastHeader == null || lastHeader.getTimePeriod() < i) {
						BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeaderByHeight(startHeight);
						if(blockHeaderStore == null) {
							lastHeader = null;
						} else {
							lastHeader = blockHeaderStore.getBlockHeader();
						}
					}
					
					if(lastHeader == null || lastHeader.getTimePeriod() != i) {
						//没出
						timeoutList.add(cus);
					} else {
						startHeight ++;
					}
				}
			}
		}
		
		//分析当前轮的超时情况

		//我的时段
		int myTimePeriod = currentMetting.getTimePeriod();
		
		long startHeight = currentMetting.getStartHeight();
		BlockHeader lastHeader = null;
		
		List<ConsensusAccount> list = currentMetting.getConsensusList();
		
		for (int i = 0; i < myTimePeriod; i++) {
			ConsensusAccount cus = list.get(i);
			
			if(lastHeader == null || lastHeader.getTimePeriod() < i) {
				BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeaderByHeight(startHeight);
				if(blockHeaderStore == null) {
					lastHeader = null;
				} else {
					lastHeader = blockHeaderStore.getBlockHeader();
				}
			}
			
			if(lastHeader == null || lastHeader.getTimePeriod() != i) {
				//没出
				timeoutList.add(cus);
			} else {
				startHeight ++;
			}
		}
		
		return timeoutList;
	}
	
	
	@Override
	public void stop() {
		executor.shutdownNow();
	}

	@Override
	public void setAccount(Account account) {
		this.account = account;
	}
	
	public Account getAccount() {
		return account;
	}

	public NetworkParams getNetwork() {
		return network;
	}

	@Override
	public ConsensusMessage getMeetingMessage(Sha256Hash msid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean messageHasReceived(Sha256Hash msid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ConsensusInfos getCurrentConsensusInfos(int timePeriod) {
		if(currentMetting == null) {
			//可能原因，还没有同步完？
			return ConsensusInfos.UNCERTAIN;
		}
		return currentMetting.getCurrentConsensusInfos(timePeriod);
	}

	/**
	 * 获取本轮共识的开始点，也就是开始高度
	 * @return long
	 */
	@Override
	public long getPeriodStartPoint() {
		return currentMetting.getStartHeight();
	}
	
}
