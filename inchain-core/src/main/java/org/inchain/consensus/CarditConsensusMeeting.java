package org.inchain.consensus;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.core.DataSynchronizeHandler;
import org.inchain.core.Definition;
import org.inchain.core.Peer;
import org.inchain.core.TimeService;
import org.inchain.crypto.Sha256Hash;
import org.inchain.filter.InventoryFilter;
import org.inchain.kits.PeerKit;
import org.inchain.listener.ConnectionChangedListener;
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
import org.inchain.transaction.business.ViolationTransaction;
import org.inchain.utils.ByteArrayTool;
import org.inchain.utils.DateUtil;
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
	//上几轮的共识信息
	private List<MeetingItem> oldMettings = new ArrayList<MeetingItem>();

	//是否允许打包
	private boolean canPackage;
	//是否正在打包中
	private boolean packageing;
	//共识调度器状态，0等待初始化，1初始化中，2初始化成功，共识中，3初始化失败
	private int meetingStatus = 0;
	//共识轮数
	private AtomicInteger meetingRound = new AtomicInteger();

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
		}, 1000, 1000, TimeUnit.MILLISECONDS);
		
		//监控节点的情况
		startPeerMonitor();
	}

	int count = 0;
	
	/**
	 * 共识会议
	 */
	protected void meeting() {
		
		count++;
		if(count % 100 == 0) {
			String address = null;
			if(account != null) {
				address = new Address(network, account.getAddress().getHash160()).getBase58();
			}
			
			log.info("bestHeight {}, meetingStatus : {}, address : {}, currentMetting : {}", getLocalBestBlockHeight(), meetingStatus, address, currentMetting);
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

	/*
	 * 打包数据
	 */
	private void doMeeting() {
		if(currentMetting.canPackage()) {
			//当前轮到我打包，异步打包数据
			doPackage();
		} else {
			//检查时间是否可以结束了
			if(currentMetting.canEnd()) {
				newMeetingRound();
			} else {
				//检查高度是否达到结束条件
				BlockHeader bestBlockHeader = network.getBestBlockHeader();
				if(bestBlockHeader.getPeriodStartTime() == currentMetting.getPeriodStartTime() && bestBlockHeader.getTimePeriod() == currentMetting.getPeriodCount() - 1) {
					newMeetingRound();
				}
			}
		}
	}

	/*
	 * 开始新的一轮共识
	 */
	private void newMeetingRound() {
		//如果当前正在打包，则等待
		while(packageing) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				if(log.isDebugEnabled()) {
					log.debug("{}", e.getMessage());
				}
			}
		}
		MeetingItem previousMetting = null;
		try {
			previousMetting = currentMetting.clone();
			oldMettings.add(0, previousMetting);
			
			if(oldMettings.size() > 5) {
				int count = oldMettings.size();
				for (int i = 0; i < count - 5; i++) {
					oldMettings.remove(5);
				}
			}
		} catch (CloneNotSupportedException e) {
			log.error("备份当前轮次信息失败" , e);
		}
		
		//开始新一轮会议
		currentMetting = new MeetingItem(this, previousMetting.getPeriodEndTime(), consensusPool.listSnapshots());
		initNewMeetingRound();
		
		//判断最新的块，是否已是上一轮的最后一个，如果不是，则延迟设置新一轮的信息，如果是，则马上设置
		BlockHeader bestBlockHeader = network.getBestBlockHeader();
		
		//如果已经是最新的，马上开始新一轮共识
		if(previousMetting.isLastBlock(bestBlockHeader)) {
			return;
		}
		//不确定当前最新的块是否已是最新的，所以等待最新的块，直到超时
		long nowTime = TimeService.currentTimeMillis();
		
		//由于时间和开始高度会延迟设置，这里判断如果我是第一个，则马上开始打包
		if(currentMetting.getTimePeriod() == 0) {
			doPackage();
		}
		
		while(true) {
			//是否超时，超时时间为区块生成间隔的一半加上1s
			boolean hasTimeout = (TimeService.currentTimeMillis() - nowTime) > (Configure.BLOCK_GEN__MILLISECOND_TIME / 2 + 1000);
			if(hasTimeout) {
				//超时结束
				break;
			}
			//最新区块
			bestBlockHeader = network.getBestBlockHeader();
			//如果已是最新区块，则开始
			if(previousMetting.isLastBlock(bestBlockHeader)) {
				//有变化
				currentMetting = new MeetingItem(this, previousMetting.getPeriodEndTime(), consensusPool.listSnapshots());
				
				//判断我是否应该停止共识
				if(packageing && currentMetting.getTimePeriod() != 0) {
					stopPackageNow();
				}
				initNewMeetingRound();
				break;
			}
			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				if(log.isDebugEnabled()) {
					log.debug("{}", e.getMessage());
				}
			}
		}
	}

	/*
	 * 初始化新一轮共识
	 */
	private void initNewMeetingRound() {
		
		currentMetting.startConsensus();
		meetingRound.incrementAndGet();
		
		log.info("一轮结束，切换新一轮共识 , 开始时间 {} , 结束时间 {} , 我的时间 {}", DateUtil.convertDate(new Date(currentMetting.getPeriodStartTime() * 1000)), DateUtil.convertDate(new Date(currentMetting.getPeriodEndTime() * 1000)), DateUtil.convertDate(new Date(currentMetting.getMyPackageTime() * 1000)));
	}

	/**
	 * 初始化，拉取当前的共识状态
	 */
	protected void init() {
		
		//分析当前共识状态
		BlockHeaderStore bestBlockStore = blockStoreProvider.getBestBlockHeader();
		if(bestBlockStore == null) {
			return;
		}
		BlockHeader bestBlockHeader = bestBlockStore.getBlockHeader();
		
		//获取当前的共识段开始点
		long periodStartTime = bestBlockHeader.getPeriodStartTime();
		//创世块的时间或者整个网络停止很长的时间之后，这里应该设置为最新时间
		if(bestBlockHeader.getHeight() < 10) {
			//以当前时间为开始时间
			periodStartTime = TimeService.currentTimeSeconds();
		}
		log.info("开始时间点: {}", DateUtil.convertDate(new Date(periodStartTime * 1000)));
		
		//获取开始时的共识列表
		//利用当前的快照倒推
		List<ConsensusAccount> consensusList = analysisConsensusSnapshots(periodStartTime);
		
		currentMetting = new MeetingItem(this, periodStartTime, consensusList);
		initNewMeetingRound();
		
		log.info("start metting : {}", currentMetting);
		
		oldMettings.clear();
		
		//加载之前5轮的共识信息
		for (int i = 0; i < 5; i++) {
			long startTime = bestBlockHeader.getPeriodStartTime();
			
			while(true) {
				Sha256Hash hash = bestBlockHeader.getPreHash();
				if(Sha256Hash.ZERO_HASH.equals(hash)) {
					break;
				}
				BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeader(hash.getBytes());
				if(blockHeaderStore == null) {
					break;
				}
				bestBlockHeader = blockHeaderStore.getBlockHeader();
				
				if(bestBlockHeader.getPeriodStartTime() != startTime) {
					break;
				}
			}
			if(startTime == bestBlockHeader.getPeriodStartTime()) {
				break;
			}
			
			List<ConsensusAccount> oldConsensusList = analysisConsensusSnapshots(bestBlockHeader.getPeriodStartTime());
			MeetingItem metting = new MeetingItem(this, bestBlockHeader.getPeriodStartTime(), oldConsensusList);
			metting.startConsensus();
			oldMettings.add(metting);
			
			//consensusListCacher
		}
		log.info("old metting size : {}", oldMettings.size());
	}
	
	/**
	 * 打包数据出新块
	 */
	public void doPackage() {
		if(packageing || !canPackage) {
			return;
		}
		packageing = true;
		
		new Thread() {
			public void run() {
				try {
					mining.mining();
				} catch (Exception e) {
					log.error("mining err", e);
				}
				packageing = false;
			}
		}.start();
	}
	
	/**
	 * 立刻停止打包
	 */
	public void stopPackageNow() {
		packageing = false;
		mining.stopMining();
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
	 * 获取某论的共识快照，从最新的内存映射倒推
	 * TODO 可以考虑做一个内存映射，每次分析计算量太大 ？
	 * @param periodStartTime
	 * @return List<ConsensusAccount>
	 */
	public List<ConsensusAccount> analysisConsensusSnapshots(long periodStartTime) {
		
		//判断是否当前轮
		if(currentMetting != null && currentMetting.getPeriodStartTime() == periodStartTime) {
			return currentMetting.getConsensusList();
		}
		
		//不是当前轮，那么去历史轮里查找
		List<ConsensusAccount> consensusList = null;
		
		for (MeetingItem meetingItem : oldMettings) {
			if(meetingItem.getPeriodStartTime() == periodStartTime) {
				consensusList = meetingItem.getConsensusList();
				break;
			}
		}
		//内存没找到，那么只有去查询了
		if(consensusList == null) {
			consensusList = consensusPool.listSnapshots();
			
			byte[] endBlockHashBytes = blockStoreProvider.getBestBlockHeader().getBlockHeader().getHash().getBytes();
			while(true) {
				BlockStore blockStore = blockStoreProvider.getBlock(endBlockHashBytes);
				if(blockStore == null || blockStore.getBlock() == null) {
					break;
				}
				
				Block block = blockStore.getBlock();
				if(block.getHeight() == 0l || block.getPeriodStartTime() < periodStartTime) {
					break;
				}
				
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
					} else if(transaction.getType() == Definition.TYPE_REM_CONSENSUS || transaction.getType() == Definition.TYPE_VIOLATION) {
						//删除掉的，新增进去
						byte[] hash160 = null;
						
						if(transaction.getType() == Definition.TYPE_REM_CONSENSUS) {
							//主动退出共识
							RemConsensusTransaction remTx = (RemConsensusTransaction) transaction;
							hash160 = remTx.getHash160();
						} else {
							//违规惩罚交易
							ViolationTransaction vtx = (ViolationTransaction) transaction;
							hash160 = vtx.getViolationEvidence().getAudienceHash160();
						}
						//这里面用不到公钥，所以不用设置
						consensusList.add(new ConsensusAccount(hash160, null));
					}
				}
				
				if(block.getPeriodStartTime() < periodStartTime) {
					break;
				} else {
					endBlockHashBytes = block.getPreHash().getBytes();
				}
				
			}
			//排序
			consensusList.sort(new Comparator<ConsensusAccount>() {
				@Override
				public int compare(ConsensusAccount o1, ConsensusAccount o2) {
					if(o1.getSortValue() == null) {
						o1.setSortValue(Sha256Hash.twiceOf((periodStartTime + o1.getHash160Hex()).getBytes()));
					}
					if(o2.getSortValue() == null) {
						o2.setSortValue(Sha256Hash.twiceOf((periodStartTime + o2.getHash160Hex()).getBytes()));
					}
					return o1.getSortValue().toString().compareTo(o2.getSortValue().toString());
				}
			});
		}
		
		return consensusList;
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
			
			try {
				message.getPeer().sendMessage(consensusMessage);
			} catch (NotYetConnectedException | IOException e) {
				e.printStackTrace();
			}
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
	 * 获取传入的共识轮和上一轮超时的账号
	 * @return Set<TimeoutConsensusViolation>
	 */
	public Set<TimeoutConsensusViolation> getTimeoutList() {
		//最终处理结果，取下面两个set的交集
		Set<TimeoutConsensusViolation> timeoutList = new HashSet<TimeoutConsensusViolation>();
		
		//如果当前只有一轮的信息，则不做任何处理
		if(oldMettings.size() == 0) {
			return timeoutList;
		}
		
		//如果当前轮还没有出块，则不处理
		BlockHeader lastHeader = network.getBestBlockHeader();
		if(lastHeader.getPeriodStartTime() != currentMetting.getPeriodStartTime()) {
			return timeoutList;
		}
		
		//当前轮未出块的节点
		Set<TimeoutConsensusViolation> currentList = new HashSet<TimeoutConsensusViolation>();
		//上一轮未出块的节点
		Set<TimeoutConsensusViolation> previousList = new HashSet<TimeoutConsensusViolation>();
		
		long periodStartTime = currentMetting.getPeriodStartTime();
		//分析当前轮的超时情况
		//我的时段
		int myTimePeriod = currentMetting.getTimePeriod();

		//每轮的第一个和最后一个人，不处理违规情况
		//这样做的好处是更能有效的避免分叉情况的发生
		if(myTimePeriod == 0 || myTimePeriod == currentMetting.getPeriodCount() -1) {
			return timeoutList;
		}
		
		List<ConsensusAccount> list = currentMetting.getConsensusList();
		
		//取得当前轮的最后一个块
		while(true) {
			if(lastHeader.getPeriodStartTime() <= periodStartTime || Sha256Hash.ZERO_HASH.equals(lastHeader.getPreHash())) {
				break;
			}
			BlockHeaderStore lastHeaderStore = blockStoreProvider.getHeader(lastHeader.getPreHash().getBytes());
			if(lastHeaderStore != null) {
				lastHeader = lastHeaderStore.getBlockHeader();
			}
		}
		
		//轮数不够判断
		if(Sha256Hash.ZERO_HASH.equals(lastHeader.getPreHash())) {
			return timeoutList;
		}
		
		//从我当前位置，的前面1个人开始，查找是否存在该时段的块，如果不存在，则说明没有打包
		for (int i = myTimePeriod - 1; i >= 0 ; i--) {
			if(i > list.size() - 1 || i < 0) {
				continue;
			}
			//最后搜索的块，大于要判断的时段，那么则继续向前搜索
			if(lastHeader.getTimePeriod() >= i && lastHeader.getPeriodStartTime() == periodStartTime) {
				BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeader(lastHeader.getPreHash().getBytes());
				//如果没有找到，或者找到了创世块，那么结束
				if(blockHeaderStore == null || Sha256Hash.ZERO_HASH.equals(blockHeaderStore.getBlockHeader().getPreHash())) {
					break;
				} else {
					lastHeader = blockHeaderStore.getBlockHeader();
				}
			} else if(lastHeader.getTimePeriod() < i || lastHeader.getPeriodStartTime() != periodStartTime) {
				//到这里代表没有找到，如果找到，那么相等则跳过
				ConsensusAccount cus = list.get(i);
				//如果已经不在共识列表里面，就不处理了，避免重复处理
				if(!consensusPool.contains(cus.getHash160())) {
					continue;
				}
				TimeoutConsensusViolation violation = new TimeoutConsensusViolation(cus.getHash160(), currentMetting.getPeriodStartTime(), currentMetting.getPeriodStartTime());
				currentList.add(violation);
			}
		}
		
		MeetingItem previousMetting = oldMettings.get(0);
		
		//同样的方法，去处理上一个轮次超时的人
		periodStartTime = previousMetting.getPeriodStartTime();
		
		//取得当前轮的最后一个块
		while(true) {
			if(lastHeader.getPeriodStartTime() <= periodStartTime || Sha256Hash.ZERO_HASH.equals(lastHeader.getPreHash())) {
				break;
			}
			BlockHeaderStore lastHeaderStore = blockStoreProvider.getHeader(lastHeader.getPreHash().getBytes());
			if(lastHeaderStore != null) {
				lastHeader = lastHeaderStore.getBlockHeader();
			}
		}
		
		List<ConsensusAccount> preConsensusList = previousMetting.getConsensusList();
		
		//查找是否存在该时段的块，如果不存在，则说明没有打包
		for (int i = preConsensusList.size() - 1; i >= 0 ; i--) {
			//最后搜索的块，大于要判断的时段，那么则继续向前搜索
			if(lastHeader.getTimePeriod() >= i && lastHeader.getPeriodStartTime() == periodStartTime) {
				BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeader(lastHeader.getPreHash().getBytes());
				//如果没有找到，或者找到了创世块，那么结束
				if(blockHeaderStore == null || Sha256Hash.ZERO_HASH.equals(blockHeaderStore.getBlockHeader().getPreHash())) {
					break;
				} else {
					lastHeader = blockHeaderStore.getBlockHeader();
				}
			} else if(lastHeader.getTimePeriod() < i || lastHeader.getPeriodStartTime() != periodStartTime) {
				//到这里代表没有找到，如果找到，那么相等则跳过
				ConsensusAccount cus = preConsensusList.get(i);
				//如果已经不在共识列表里面，就不处理了，避免重复处理
				if(!consensusPool.contains(cus.getHash160())) {
					continue;
				}
				TimeoutConsensusViolation violation = new TimeoutConsensusViolation(cus.getHash160(), previousMetting.getPeriodStartTime(), previousMetting.getPeriodStartTime());
				previousList.add(violation);
			}
		}
		
		//取交集
		//连续2轮不出块，则处理
		for (TimeoutConsensusViolation previousViolation : previousList) {
			for (TimeoutConsensusViolation currentViolation : currentList) {
				if(Arrays.equals(currentViolation.getHash160(), previousViolation.getHash160())) {
					timeoutList.add(new TimeoutConsensusViolation(currentViolation.getHash160(), currentViolation.getCurrentPeriodStartTime(), previousViolation.getPreviousPeriodStartTime()));
					break;
				}
			}
		}
		
		return timeoutList;
	}
	
	/**
	 * 停止服务
	 */
	@Override
	public void stop() {
		executor.shutdownNow();
	}
	
	/**
	 * 从下一轮开始共识
	 * @param block 从该块开始
	 */
	public void startConsensusOnNextRound(Block block) {
		//判断该块是否是最后一个块，如果是，则检查我有没有加进共识
		long bestBlockHeight = network.getBestHeight();
		//传入的块要是网络最新的块，则做相应的处理
		if(bestBlockHeight == block.getHeight() && block.getPeriodCount() - 1 == block.getTimePeriod()) {
			if(currentMetting.getPeriodStartTime() != block.getPeriodStartTime() && currentMetting.getTimePeriod() == -1) {
				if(account == null) {
					//异步监控超时处理
					waitAccountNotNull(block);
				} else {
					resetMeeting(block);
				}
			}
		}
	}

	/*
	 * 等待启动共识，设置共识账户，超时时间为一个区块时段
	 */
	private void waitAccountNotNull(Block block) {
		new Thread() {
			public void run() {
				//超时处理
				long nowTime = TimeService.currentTimeMillis();
				while(true) {
					if(TimeService.currentTimeMillis() - nowTime > Configure.BLOCK_GEN__MILLISECOND_TIME) {
						break;
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						if(log.isDebugEnabled()) {
							log.debug("{}", e.getMessage());
						}
					}
				}
				resetMeeting(block);
			}
		}.start();
	}
	
	/*
	 * 重新设置会议
	 */
	private void resetMeeting(Block block) {
		currentMetting = new MeetingItem(CarditConsensusMeeting.this, oldMettings.get(0).getPeriodEndTime(), consensusPool.listSnapshots());
		initNewMeetingRound();
		meetingRound.set(meetingRound.get() - 1);
	}
	
	/**
	 * 从下一轮开始停止共识
	 * @param block 从该块开始
	 */
	public void stopConsensusOnNextRound(Block block) {
		
		//判断该块是否是最后一个块，如果是，则检查我有没有加进共识
		long bestBlockHeight = network.getBestHeight();
		//传入的块要是网络最新的块，则做相应的处理
		if(bestBlockHeight == block.getHeight() && block.getPeriodCount() - 1 == block.getTimePeriod()
				&& currentMetting.getPeriodStartTime() != block.getPeriodStartTime() && currentMetting.getTimePeriod() != -1) {
			setAccount(null);
			mining.reset(true);
			initNewMeetingRound();
			meetingRound.set(meetingRound.get() - 1);
		} else {
			setAccount(null);
			mining.reset(false);
		}
	}
	
	@Override
	public void setAccount(Account account) {
		this.account = account;
		
		waitMeeting();
		
		//开始共识的时候，重新记轮次
		meetingRound = new AtomicInteger();
		currentMetting.setMyPackageTime(0);
		currentMetting.setMyPackageTimeEnd(0);
		
		currentMetting.startConsensus();
	}
	
	public Account getAccount() {
		return account;
	}

	public NetworkParams getNetwork() {
		return network;
	}

	@Override
	public ConsensusMessage getMeetingMessage(Sha256Hash msid) {
		return null;
	}

	@Override
	public boolean messageHasReceived(Sha256Hash msid) {
		return false;
	}

	/**
	 * 获取区块高度对应的打包人信息，只适用于新区快的验证
	 * @param periodStartTime 一轮开始的秒数
	 * @param timePeriod 区块时段
	 * @return ConsensusInfos
	 */
	@Override
	public ConsensusInfos getCurrentConsensusInfos(long periodStartTime, int timePeriod) {
		if(currentMetting == null) {
			//可能原因，还没有同步完？
			return ConsensusInfos.UNCERTAIN;
		}
		ConsensusInfos result = getConsensusInfos(periodStartTime, timePeriod);
		if(result == null) {
			//错误的区，如果接收，会引起灾难
			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				if(log.isDebugEnabled()) {
					log.debug("{}", e.getMessage());
				}
			}
			return getConsensusInfos(periodStartTime, timePeriod);
		} else {
			return result;
		}
	}

	private ConsensusInfos getConsensusInfos(long periodStartTime, int timePeriod) {
		if(currentMetting.getPeriodStartTime() == periodStartTime) {
			//如果当前轮没有初始化完成，则等待
			while(!currentMetting.isInit()) {
				try {
					Thread.sleep(10l);
				} catch (InterruptedException e) {
					if(log.isDebugEnabled()) {
						log.debug("{}", e.getMessage());
					}
				}
			}
			return currentMetting.getCurrentConsensusInfos(timePeriod);
		} else {
			for (MeetingItem meetingItem : oldMettings) {
				if(meetingItem.getPeriodStartTime() == periodStartTime) {
					return meetingItem.getCurrentConsensusInfos(timePeriod);
				}
			}
		}
		return null;
	}

	/**
	 * 获取本轮共识的开始时间点，单位秒
	 * @return long
	 */
	@Override
	public long getPeriodStartTime() {
		return currentMetting.getPeriodStartTime();
	}

	/**
	 * 打包信息，轮到我打包时，根据共识会议，获取我的打包信息
	 * @return MiningInfos
	 */
	@Override
	public MiningInfos getMineMiningInfos() {
		//判断是否已经被切换
		MiningInfos infos = new MiningInfos();
		infos.setHash160(currentMetting.getMyHash160());
		infos.setTimePeriod(currentMetting.getTimePeriod());
		infos.setPeriodCount(currentMetting.getPeriodCount());
		infos.setBeginTime(currentMetting.getMyPackageTime());
		infos.setEndTime(currentMetting.getMyPackageTimeEnd());
		infos.setPeriodStartTime(currentMetting.getPeriodStartTime());
		infos.setPeriodEndTime(currentMetting.getPeriodEndTime());
		return infos;
	}

	/**
	 * 获取以 periodStartTime 开始的会议详情
	 * @return MeetingItem
	 */
	@Override
	public MeetingItem getMeetingItem(long periodStartTime) {
		if(currentMetting == null) {
			return null;
		}
		if(currentMetting.getPeriodStartTime()== periodStartTime) {
			return currentMetting;
		}
		for (MeetingItem meetingItem : oldMettings) {
			if(meetingItem.getPeriodStartTime() == periodStartTime) {
				return meetingItem;
			}
		}
		return null;
	}

	/**
	 * 共识会议是否进行中，所有的节点都会参加会议，有权限的（共识节点）才会发言
	 * 所以外部接受新块，必须保证顺利加入会议，否则接收新块会出错
	 * 当调用此方法时，会阻塞，直到meetingStatus = 2 ，也就是初始化完成
	 */
	@Override
	public boolean waitMeeting() {
		while(meetingStatus != 2) {
			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				if(log.isDebugEnabled()) {
					log.debug("{}", e.getMessage());
				}
			}
		}
		return true;
	}
	
	/**
	 * 等待参与会议，即申请了共识，等待打包
	 * 申请共识时，调用此方法，可知道是否成功进入到了共识模式
	 * 阻塞的方法，知道成功共识或者超时
	 */
	@Override
	public boolean waitMining() {
		long now = TimeService.currentTimeMillis();
		while(account == null) {
			if(TimeService.currentTimeMillis() - now >= 60000l) {
				return false;
			}
			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				if(log.isDebugEnabled()) {
					log.debug("{}", e.getMessage());
				}
			}
		}
		return true;
	}
	
	/**
	 * 获取当前会议参与人数
	 * @return int
	 */
	public int getCurrentMeetingPeriodCount() {
		return currentMetting.getPeriodCount();
	}

	/**
	 * 重置当前轮共识会议，置为最新区块的状态
	 * 一般发生在处理分叉链成功之后，当前最新区块发生了变化，相应的会议也会发生变化
	 * 如果我在当前轮共识中，那么需要立即停止，并重新检测，重置之后的会议，如果错过了，那么则本轮不打包
	 * @return boolean
	 */
	@Override
	public boolean resetCurrentMeetingItem() {
		
		//判断当前是否正在共识中
		if(currentMetting != null && currentMetting.getTimePeriod() != -1 && packageing) {
			//停止
			stopPackageNow();
		}
		
		init();
		return true;
	}
	
	/*
	 * 监控节点的情况，如果发现所有连接都断开了，可能是本地断网掉线了，这时如果在共识列表中，应该立即停止，避免单机运行下去，就和网络分叉了
	 * 停止之后也会继续监控状态，如果恢复，则继续（如果我还在共识列表中的话）
	 * 
	 */
	private void startPeerMonitor() {
		//初始化是允许打包的，当连接的节点为0时，则不继续打包，否则就本地分叉了
		canPackage = true;
		//监控节点变化
		peerKit.addConnectionChangedListener(new ConnectionChangedListener() {
			@Override
			public void onChanged(int inCount, int outCount, CopyOnWriteArrayList<Peer> inPeers,
					CopyOnWriteArrayList<Peer> outPeers) {
				//当连接的节点数量为0时，停止
				if(inCount + outCount == 0) {
					//判断当前是否正在共识中
					if(currentMetting != null && currentMetting.getTimePeriod() != -1) {
						canPackage = false;
					}
				} else {
					//重新上线，继续共识
					canPackage = true;
				}
			}
		});
	}
}
