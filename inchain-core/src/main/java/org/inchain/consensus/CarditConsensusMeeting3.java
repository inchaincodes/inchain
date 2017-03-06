//package org.inchain.consensus;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
//import org.inchain.Configure;
//import org.inchain.account.Account;
//import org.inchain.core.TimeService;
//import org.inchain.crypto.Sha256Hash;
//import org.inchain.filter.InventoryFilter;
//import org.inchain.kits.PeerKit;
//import org.inchain.message.ConsensusMessage;
//import org.inchain.message.InventoryItem;
//import org.inchain.message.InventoryMessage;
//import org.inchain.network.NetworkParams;
//import org.inchain.utils.ByteArrayTool;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
///**
// * 信用共识会议
// * @author ln
// *
// */
//@Service
//public class CarditConsensusMeeting3 implements ConsensusMeeting {
//
//	private static final Logger log = LoggerFactory.getLogger(CarditConsensusMeeting3.class);
//	
//	/** 会议状态 **/
//	/** 等待就绪 **/
//	public final static int MEETING_STATUS_WAIT_READY = 1;
//	/** 就绪完成，等待开始 **/
//	public final static int MEETING_STATUS_WAIT_BEGIN = 2;
//	/** 共识中 **/
//	public final static int MEETING_STATUS_CONSENSUS = 3;
//	/** 共识中，接受下一轮选举 **/
//	public final static int MEETING_STATUS_CONSENSUS_WAIT_NEXT = 4;
//	
//	@Autowired
//	private NetworkParams network;
//	@Autowired
//	private PeerKit peerKit;
//	@Autowired
//	private Mining mining;
//	@Autowired
//	private InventoryFilter filter;
//
//	//任务调度器
//	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
//	
//	//所有消息队列
//	private static Map<Sha256Hash, ConsensusMessage> messages = new HashMap<Sha256Hash, ConsensusMessage>();
//		
//	private Lock messageLocker = new ReentrantLock();
//
//	//当前共识账号
//	private Account account;
//	
//	//当前轮共识信息
//	private MeetingItem currentMetting;
//	//下一轮轮共识信息
//	private MeetingItem nextMetting;
//
//	//就绪消息缓存，避免一轮中发送的消息不一致
//	private ConsensusMessage readyMessageCacher;
//	//缓存共识顺序一致消息
//	private ConsensusMessage consensusMessageCacher;
//	
//	//最后一次收到共识就绪消息的时间，这里指的是新共识就绪，重复的不算
//	private long lastReceiveMessageTime = TimeService.currentTimeMillis();
//
//	private boolean packageing;
//	//共识调度器状态，0等待初始化，1初始化成功，共识中，2拉取状态
//	private int meetingStatus = 0;
//	
//	/**
//	 * 获取区块高度对应的打包人信息，只适用于新区快的验证
//	 * @param height 区块高度
//	 * @return ConsensusInfos
//	 */
//	public ConsensusInfos getCurrentConsensusInfos(long height) {
//		if(currentMetting == null || meetingStatus != 1) {
//			return ConsensusInfos.UNCERTAIN;
//		}
//		return currentMetting.getCurrentConsensusInfos(height);
//	}
//	
//	@Override
//	public void broadcastMessage(ConsensusMessage consensusMessage) {
//		//除拉取共识状态之外的消息都使用inv广播
//		byte[] content = consensusMessage.getContent();
//		if(content[0] == 1 || content[0] == 2) {
//			peerKit.broadcastMessage(consensusMessage);
//		} else {
//			Sha256Hash id = consensusMessage.getId();
//			if(messages.get(id) == null) {
//				receiveMeetingMessage(id, consensusMessage);
//				//过滤该条消息的inv，因为本地发出的，没必要再下载
//				filter.insert(id.getBytes());
//			}
//			InventoryMessage message = new InventoryMessage(network, new InventoryItem(InventoryItem.Type.Consensus, id));
//			peerKit.broadcastMessage(message);
//		}
//	}
//
//	@Override
//	public void receiveMeetingMessage(Sha256Hash msid, ConsensusMessage message) {
//		if(msid == null) {
//			return;
//		}
//		//如果已经接收，则不处理
//		if(messageHasReceived(msid)) {
//			return;
//		}
//		if(log.isDebugEnabled()) {
//			log.debug("receive consensus message : {}", message);
//		}
//		
//		messages.put(msid, message);
//
//		//如果是准备就绪消息，则放入准备就绪队列
//		byte[] bodys = message.getContent();
//		byte type = bodys[0];
//		
//		if(account == null) {
//			if(type == 3 || type == 4) {
//				processMessage(message);
//			}
//			return;
//		}
//		if(type == 1) {
//			//拉取最新共识状态
//			processSendMeetingStatus(message);
//		} else if(type == 2) {
//			//接收最新共识状态回应
//			processReceiveMeetingStatus(message);
//		} else if(type == 3 || type == 4) {
//			processMessage(message);
//		}
//	}
//	
//	/*
//	 * 共识消息
//	 */
//	private void processMessage(ConsensusMessage message) {
////		messageLocker.lock();
////		try {
////			if(currentMetting == null) {
////				return;
////			}
////			//收到的是否是新消息
////			boolean isNewMessage = false;
////			int currentStatus = currentMetting.getStatus();
////			if(currentStatus == MEETING_STATUS_WAIT_READY && message.getHeight() == currentMetting.getStartHeight()) {
////				isNewMessage = currentMetting.addMessage(message);
////			} else if(currentStatus == MEETING_STATUS_CONSENSUS_WAIT_NEXT && message.getHeight() == currentMetting.getEndHeight()) {
////				//当收到下一轮的共识消息之后，开始设置下一轮的会议
////				if(nextMetting == null) {
////					nextMetting = new MeetingItem(this);
////					nextMetting.setStatus(MEETING_STATUS_WAIT_READY);
////					nextMetting.setStartHeight(message.getHeight());
////					nextMetting.setHeight(message.getHeight());
////				}
////				isNewMessage = nextMetting.addMessage(message);
////			} else {
////				//错误，不参与
////				return;
////			}
////			if(isNewMessage) {
////				lastReceiveMessageTime = TimeService.currentTimeMillis();
////			}
////		} finally {
////			messageLocker.unlock();
////		}
//	}
//	
//	/*
//	 * 其它节点需要拉取最新共识状态，把自己的最新状态回应过去
//	 */
//	private void processSendMeetingStatus(ConsensusMessage message) {
////		//本地最新高度
////		long localBestBlockHeight = getLocalBestBlockHeight();
////		
////		ByteArrayTool byteArray = new ByteArrayTool();
////		byteArray.append(2);
////		
////		if(currentMetting == null) {
////			//当前轮还没有开始
////			currentMetting = new MeetingItem(this);
////			currentMetting.setStatus(MEETING_STATUS_WAIT_READY);
////			
////			//当前是否最新
////			if(network.blockIsNewestStatus()) {
////				currentMetting.setStartHeight(localBestBlockHeight);
////				currentMetting.setHeight(localBestBlockHeight);
////			} else {
////				currentMetting.setStartHeight(0l);
////				currentMetting.setHeight(0l);
////			}
////		}
////		byteArray.append(currentMetting.serialize());
////		
////		if(nextMetting != null) {
////			byteArray.append(nextMetting.serialize());
////		}
////		
////		ConsensusMessage consensusMessage = new ConsensusMessage(message.getNetwork(), account.getAddress().getHash160(), localBestBlockHeight, byteArray.toArray());
////		consensusMessage.sign(account);
////		
////		message.getPeer().sendMessage(consensusMessage);
//	}
//	
//	/*
//	 * 发送拉取最新共识状态消息之后，这里接收相应的回应，处理好之后把共识状态置为准备就绪状态
//	 */
//	private void processReceiveMeetingStatus(ConsensusMessage message) {
//		
//		messageLocker.lock();
////		try {
////			if(meetingStatus == 1) {
////				return;
////			}
////			
////			byte[] content = message.getContent();
////			if(currentMetting == null) {
////				currentMetting = new MeetingItem(this);
////			}
////			currentMetting.parse(content, 1);
////			
////			int length = currentMetting.serialize().length;
////			
////			//下一轮是否存在
////			if(length + 1 < content.length) {
////				if(nextMetting == null) {
////					nextMetting = new MeetingItem(this);
////				}
////				nextMetting.parse(content, length + 1);
////			}
////			meetingStatus = 1;
////		} finally {
////			messageLocker.unlock();
////		}
//	}
//
//	@Override
//	public void startSyn() {
//		executor.scheduleWithFixedDelay(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					meeting();
//				} catch (Exception e) {
//					log.error("共识会议出错", e);
//				}
//			}
//		}, 1, 1, TimeUnit.SECONDS);		
//	}
//	
//	int count = 0;
//
//	/**
//	 * 进行共识
//	 */
//	protected void meeting() {
//		
//		count ++;
//		if(count % 10 == 0) {
//			log.info("{} ========= {}", currentMetting, nextMetting);
//		}
//		//当前论还没开始，则跳过
//		if(currentMetting == null) {
//			return;
//		}
//		
//		//会议状态是否应该转换
//		//当会议为待就绪时，超过单个区块的出块时间没有收到新的就绪消息，则变更为就绪完成，待开始
//		//当会议为就绪完成待开始时，则变更为共识中
//		if((TimeService.currentTimeMillis() - lastReceiveMessageTime) >= Configure.BLOCK_GEN__MILLISECOND_TIME) {
//			int cStatus = currentMetting.getStatus();
//			if(cStatus == MEETING_STATUS_WAIT_READY && currentMetting.canStartConsensus()) {
//				currentMetting.setStatus(MEETING_STATUS_WAIT_BEGIN);
//			} else if(cStatus == MEETING_STATUS_WAIT_BEGIN) {
//				currentMetting.startConsensus();
//				currentMetting.setStatus(MEETING_STATUS_CONSENSUS);
//			} else if(nextMetting != null) {
//				int nStatus = nextMetting.getStatus();
//				if(nStatus == MEETING_STATUS_WAIT_READY) {
//					nextMetting.setStatus(MEETING_STATUS_WAIT_BEGIN);
//					lastReceiveMessageTime = TimeService.currentTimeMillis();
//				} else if(nStatus == MEETING_STATUS_WAIT_BEGIN) {
//					nextMetting.startConsensus();
//					nextMetting.setStatus(MEETING_STATUS_CONSENSUS);
//				}
//			}
//		}
//
//		//本地最新高度
//		long localBestBlockHeight = getLocalBestBlockHeight();
//		//会议状态转换，当最新高度达到本轮会议的结束高度时，把下一轮设置为当前轮
//		if(currentMetting.getEndHeight() != 0l && currentMetting.getEndHeight() <= localBestBlockHeight && 
//				nextMetting != null && nextMetting.getStatus() == MEETING_STATUS_CONSENSUS) {
//			currentMetting = nextMetting;
//			currentMetting.updateInfos();
//			nextMetting = null;
//		}
//
//		//判断当前的状态
//		int currentStatus = currentMetting.getStatus();
//		
//		if(currentStatus == MEETING_STATUS_WAIT_BEGIN) {
//			//等待开始状态，发送共识顺序消息
//			sendConsensusMessage(currentMetting.getStartHeight());
//		} else if(currentStatus == MEETING_STATUS_CONSENSUS || 
//				currentStatus == MEETING_STATUS_CONSENSUS_WAIT_NEXT) {
//			//共识中，马上切换为下一轮可开始，才能打包
//			currentMetting.setStatus(MEETING_STATUS_CONSENSUS_WAIT_NEXT);
//			currentMetting.updateInfos();
//			//打包
//			doMeeting(localBestBlockHeight);
//		}
//		
//		if(currentStatus == MEETING_STATUS_WAIT_READY || currentStatus == MEETING_STATUS_CONSENSUS_WAIT_NEXT) {
//			//当前轮或者下一轮可以参与
//			//下一轮的状态是什么
//			if(currentStatus == MEETING_STATUS_WAIT_READY || nextMetting == null || nextMetting.getStatus() == MEETING_STATUS_WAIT_READY) {
//				//就绪
//				doJoin(currentStatus, localBestBlockHeight);
//			} else if(nextMetting.getStatus() == MEETING_STATUS_WAIT_BEGIN){
//				//下一轮的共识顺序
//				sendConsensusMessage(nextMetting.getStartHeight());
//			}
//		}
//	}
//
//	/*
//	 * 打包数据
//	 */
//	private void doMeeting(long localBestBlockHeight) {
//		if(currentMetting.canPackage(localBestBlockHeight)) {
//			//当前轮到我打包，异步打包数据
//			new Thread() {
//				public void run() {
//					doPackage();
//				}
//			}.start();
//		} else {
//			//当前不该我打包，但是我应该监控当前的状态，如果超时或者出现分叉，应及时处理
//			currentMetting.monitor(localBestBlockHeight);
//		}
//	}
//	
//	/**
//	 * 打包数据出新块
//	 */
//	public void doPackage() {
//		if(packageing) {
//			return;
//		}
//		packageing = true;
//		
//		try {
//			mining.mining();
//		} catch (Exception e) {
//			log.error("mining err", e);
//		}
//		
//		packageing = false;
//	}
//	
//	/*
//	 * 发送共识顺序消息
//	 */
//	private void sendConsensusMessage(long height) {
//		if(consensusMessageCacher != null && consensusMessageCacher.getHeight() == height) {
//			return;
//		}
//		//content格式第一位为type,4为达成一致共识顺序消息
//		byte[] content = new byte[33];
//		content[0] = 4;
//		//签名顺序
//		byte[] orderSign = null;
//		if(nextMetting != null && height == nextMetting.getStartHeight()) {
//			orderSign = nextMetting.signConsensusOrder();
//		} else if(height == currentMetting.getStartHeight()) {
//			orderSign = currentMetting.signConsensusOrder();
//		} else {
//			log.error("发送共识消息出错，错误的高度 {} {}", currentMetting.getStartHeight(), height);
//			return;
//		}
//		System.arraycopy(orderSign, 0, content, 1, orderSign.length);
//		
//		ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), height, content);
//		//签名共识消息
//		message.sign(account);
//		
//		broadcastMessage(message);
//		
//		consensusMessageCacher = message;
//	}
//	
//	/*
//	 * 参与当前论或者下一轮的共识，发送就绪消息
//	 */
//	private void doJoin(int currentStatus, long localBestBlockHeight) {
//		//本次参与共识的高度
//		long blockHeight = 0l;
//		
//		if(currentMetting.getStartHeight() == 0l) {
//			currentMetting.setStartHeight(localBestBlockHeight);
//			currentMetting.setHeight(localBestBlockHeight);
//		}
//		
//		if(currentStatus == MEETING_STATUS_WAIT_READY && localBestBlockHeight == currentMetting.getStartHeight()) {
//			blockHeight = currentMetting.getStartHeight();
//		} else if(currentStatus == MEETING_STATUS_CONSENSUS_WAIT_NEXT) {
//			blockHeight = currentMetting.getEndHeight();
//		} else {
//			//错误，不参与
//			return;
//		}
//		
//		//发送就绪消息
//		//我是否已经在本轮就绪列表里面
//		boolean hasSend = false;
//		
//		if(currentStatus == MEETING_STATUS_WAIT_READY) {
//			hasSend = currentMetting.hasJoinReadys(account.getAddress().getHash160());
//		} else {
//			hasSend = nextMetting != null && nextMetting.hasJoinReadys(account.getAddress().getHash160());
//		}
//		if(!hasSend) {
//			sendReadyMessage(blockHeight);
//		}
//	}
//
//	/*
//	 * 发送准备就绪消息
//	 */
//	private void sendReadyMessage(long height) {
//		
//		if(readyMessageCacher != null && readyMessageCacher.getHeight() == height) {
//			broadcastMessage(readyMessageCacher);
//			return;
//		}
//		//content格式第一位为type,3为准备就绪消息
//		byte[] content = new byte[]{ 3 };
//		
//		ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), height, content);
//		//签名共识消息
//		message.sign(account);
//		
//		broadcastMessage(message);
//		
//		readyMessageCacher = message;
//		if(log.isDebugEnabled()) {
//			log.debug("高度{}轮的共识，已发送就绪消息", height);
//		}
//	}
//	
//	/*
//	 * 获取本地区块最新高度
//	 */
//	public long getLocalBestBlockHeight() {
//		return network.getBestBlockHeight();
//	}
//	
//	@Override
//	public void stop() {
//		executor.shutdownNow();
//	}
//
//	@Override
//	public void setAccount(Account account) {
//		this.account = account;
//	}
//	
//	public Account getAccount() {
//		return account;
//	}
//
//	@Override
//	public ConsensusMessage getMeetingMessage(Sha256Hash msid) {
//		return messages.get(msid);
//	}
//
//	@Override
//	public boolean messageHasReceived(Sha256Hash msid) {
//		return messages.containsKey(msid);
//	}
//	
//	public NetworkParams getNetwork() {
//		return network;
//	}
//	
//}
