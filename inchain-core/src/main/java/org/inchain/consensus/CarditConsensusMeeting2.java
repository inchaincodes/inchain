//package org.inchain.consensus;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
//import javax.annotation.PostConstruct;
//
//import org.inchain.account.Account;
//import org.inchain.account.Address;
//import org.inchain.core.TimeService;
//import org.inchain.crypto.Sha256Hash;
//import org.inchain.kits.PeerKit;
//import org.inchain.message.ConsensusMessage;
//import org.inchain.message.InventoryItem;
//import org.inchain.message.InventoryMessage;
//import org.inchain.network.NetworkParams;
//import org.inchain.utils.Utils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//
///**
// * 信用共识会议
// * @author ln
// *
// */
//public class CarditConsensusMeeting2 implements ConsensusMeeting {
//
//	private static final Logger log = LoggerFactory.getLogger(CarditConsensusMeeting2.class);
//	
//	private Lock sequenceLocker = new ReentrantLock();
//	private Lock readerMessageLocker = new ReentrantLock();
//	
//	//所有消息队列
//	private static Map<Sha256Hash, ConsensusMessage> messages = new HashMap<Sha256Hash, ConsensusMessage>();
//	//准备就绪队列
//	private static Map<Long, List<ConsensusMessage>> readys = new HashMap<Long, List<ConsensusMessage>>();
//	//每轮共识达成发言顺序
//	private static Map<Long, List<ConsensusMessage>> sequences = new HashMap<Long, List<ConsensusMessage>>();
//
//	//顺序共识中，出现异常共识节点，则把异常共识节点加入黑名单，一旦进入黑名单，意味着本轮的任何消息不再被接收，而且本轮会被信用惩罚以及移除共识列表
//	private static Map<Long, List<List<ConsensusMessage>>> sequenceBlacklist = new HashMap<Long, List<List<ConsensusMessage>>>();
//	
//	//顺序共识中，出现异常共识节点，则顺序共识消息会被更新，之前的队列会备份起来
//	private static Map<Long, List<List<ConsensusMessage>>> sequenceBackupList = new HashMap<Long, List<List<ConsensusMessage>>>();
//		
//	//任务调度器
//	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
//	
//	@Autowired
//	private NetworkParams network;
//	@Autowired
//	private PeerKit peerKit;
//	@Autowired
//	private Mining mining;
//	
//	//会议状态，0待开始，1准备就绪，2顺序共识中，3共识打包进行中
//	private int meetingStatus = 0;
//	//当前最新高度，
//	private long bestblockHeight;
//	//当前轮共识开始高度
//	private long currentMeetingHeight;
//	//下一轮共识高度
//	private long nextMeetingHeight;
//	//当前共识账号
//	private Account account;
//	//当前是否正在打包
//	private boolean packageing;
//	//顺序共识过程中，发出的顺序共识消息，包含的就绪共识节点数量
//	private int currentSendSequenceCount;
//	//本轮中，接收顺序共识消息数量，用于异常情况判断超时处理
//	private int currentReceiveSequenceCount;
//	//本论中，最后扫描接收顺序共识消息数量变化的时间，用于异常情况超时处理
//	private long currentScanningReceiveSequenceTime;
//	//就绪消息缓存，避免一轮中发送的消息不一致
//	private ConsensusMessage readyMessageCacher;
//	
//	@PostConstruct
//	public void init() {
//		bestblockHeight = network.getBestBlockHeight();
//		currentMeetingHeight = bestblockHeight;
//	}
//	
//	@Override
//	public void broadcastMessage(ConsensusMessage consensusMessage) {
//		
//		//除拉取共识状态之外的消息都使用inv广播
//		byte[] content = consensusMessage.getContent();
//		if(content[0] == 1 || content[0] == 2) {
//			peerKit.broadcastMessage(consensusMessage);
//		} else {
//			Sha256Hash id = consensusMessage.getId();
//			if(messages.get(id) == null) {
//				receiveMeetingMessage(id, consensusMessage);
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
//		messages.put(msid, message);
//		//如果是准备就绪消息，则放入准备就绪队列
//		byte[] bodys = message.getContent();
//		byte type = bodys[0];
//		if(type == 1) {
//			//拉取最新共识状态
//			processSendMeetingStatus(message);
//		} else if(type == 2) {
//			//接收最新共识状态回应
//			processReceiveMeetingStatus(message);
//		} else if(type == 3) {
//			//准备就绪消息
//			processReadyMessage(message);
//		} else if(type == 4) {
//			//顺序共识消息
//			processSequenceMessage(message);
//		}
//	}
//
//	/*
//	 * 发送拉取最新共识状态消息之后，这里接收相应的回应，处理好之后把共识状态置为准备就绪状态
//	 */
//	private void processReceiveMeetingStatus(ConsensusMessage message) {
//		
//		
//		log.info("==============");
//		log.info("{}", message);
//		log.info("==============");
//		
//		message.getHeight();
//		
//		byte[] content = message.getContent();
//		
//		int index = 1;
//		//当前共识状态
//		meetingStatus = content[index];
//		index++;
//		//当前轮共识高度
//		currentMeetingHeight = Utils.readUint32(content, index);
//		index += 4;
//		//下一轮共识高度
//		nextMeetingHeight = Utils.readUint32(content, index);
//		index += 4;
//		
//		//当前轮的就绪消息
//		long readCount = Utils.readUint32(content, index);
//		index += 4;
//		for (int i = 0; i < readCount; i++) {
//			ConsensusMessage consensusMessage = new ConsensusMessage(network, content, index);
//			index += consensusMessage.getLength();
//			processReadyMessage(consensusMessage);
//			//如果是自己的，则设置缓存
//			if(Arrays.equals(account.getAddress().getHash160(), consensusMessage.getHash160())) {
//				readyMessageCacher = consensusMessage;
//			}
//		}
//		//当前轮的就绪消息
//		long sequenceCount = Utils.readUint32(content, index);
//		index += 4;
//		for (int i = 0; i < sequenceCount; i++) {
//			ConsensusMessage consensusMessage = new ConsensusMessage(network, content, index);
//			index += consensusMessage.getLength();
//			processSequenceMessage(consensusMessage);
//		}
//		
//		if(bestblockHeight == currentMeetingHeight) {
//			sendReadyMessage();
//		} else {
//			sendReadyMessage(nextMeetingHeight);
//			currentMeetingHeight = nextMeetingHeight;
//		}
//	}
//
//	/*
//	 * 其它节点需要拉取最新共识状态，把自己的最新状态回应过去
//	 */
//	private void processSendMeetingStatus(ConsensusMessage message) {
//		
//		List<byte[]> contentList = new ArrayList<byte[]>();
//		
//		//类型，2回应共识状态
//		contentList.add(new byte[]{ 2 });
//		
//		//当前共识状态
//		contentList.add(new byte[]{ (byte) meetingStatus });
//		
//		//当前轮共识开始高度
//		byte[] cmh = new byte[4];
//		//下一轮共识高度
//		byte[] nmh = new byte[4];
//		
//		Utils.uint32ToByteArrayLE(currentMeetingHeight, cmh, 0);
//		Utils.uint32ToByteArrayLE(nextMeetingHeight, nmh, 0);
//		
//		contentList.add(cmh);
//		contentList.add(nmh);
//		
//		//当前轮的就绪消息
//		List<ConsensusMessage> readyList = readys.get(currentMeetingHeight);
//		//就绪消息数量
//		byte[] readyCounts = new byte[4];
//		Utils.uint32ToByteArrayLE(readyList == null? 0:readyList.size(), readyCounts, 0);
//		contentList.add(readyCounts);
//		
//		if(readyList != null) {
//			for (ConsensusMessage consensusMessage : readyList) {
//				byte[] bs = consensusMessage.baseSerialize();
//				contentList.add(bs);
//			}
//		}
//		
//		//当前轮的顺序共识消息
//		List<ConsensusMessage> sequenceList = sequences.get(currentMeetingHeight);
//		//顺序共识消息数量
//		byte[] sequenceCounts = new byte[4];
//		Utils.uint32ToByteArrayLE(sequenceList == null ? 0:sequenceList.size(), sequenceCounts, 0);
//		contentList.add(sequenceCounts);
//		if(sequenceList != null) {
//			for (ConsensusMessage consensusMessage : sequenceList) {
//				byte[] bs = consensusMessage.baseSerialize();
//				contentList.add(bs);
//			}
//		}
//		
//		int length = 0;
//		for (byte[] b : contentList) {
//			length += b.length;
//		}
//		
//		byte[] content = new byte[length];
//		
//		int index = 0;
//		for (byte[] b : contentList) {
//			for (byte c : b) {
//				content[index] = c;
//				index++;
//			}
//		}
//		
//		ConsensusMessage consensusMessage = new ConsensusMessage(message.getNetwork(), account.getAddress().getHash160(), bestblockHeight, content);
//		consensusMessage.sign(account);
//		
//		message.getPeer().sendMessage(consensusMessage);
//	}
//
//	/*
//	 * 共识节点准备就绪消息
//	 */
//	private void processReadyMessage(ConsensusMessage message) {
////		if(meetingStatus == 3 && message.getHeight() != nextMeetingHeight) {
////			//当共识过程中，只接收下一轮的消息
////			log.warn("错误的就绪消息高度 {}", message.getHeight());
////			return;
////		} else if((meetingStatus == 1 || meetingStatus == 2) && message.getHeight() != bestblockHeight) {
////			//就绪和顺序共识过程中，只接收当前轮的消息
////			log.warn("错误的就绪消息高度 {}, bestblockHeight {}", message.getHeight(), bestblockHeight);
////			return;
////		}
//		readerMessageLocker.lock();
//		try {
//			//有可能恶意节点欺诈，所以需要排除重复的节点
//			List<ConsensusMessage> readyList = readys.get(message.getHeight());
//			if(readyList == null) {
//				readyList = new ArrayList<ConsensusMessage>();
//				readyList.add(message);
//				readys.put(message.getHeight(), readyList);
//			} else {
//				//过滤重复节点
//				boolean exist = false;
//				for (ConsensusMessage ready : readyList) {
//					if(Arrays.equals(ready.getHash160(), message.getHash160())) {
//						exist = true;
//						break;
//					}
//				}
//				if(!exist) {
//					readyList.add(message);
//				}
//			}
//		} finally {
//			readerMessageLocker.unlock();
//		}
//		//自己是否准备就绪
//		if(account != null && Arrays.equals(account.getAddress().getHash160(), message.getHash160())) {
//			meetingStatus = 1;
//		}
//	}
//	
//	/*
//	 * 处理接收到的顺序共识消息
//	 */
//	private void processSequenceMessage(ConsensusMessage message) {
//		
//		sequenceLocker.lock();
//		
//		try {
//	//		if(meetingStatus == 3 && message.getHeight() != nextMeetingHeight) {
//	//			//当共识过程中，只接收下一轮的消息
//	//			log.warn("错误的顺序共识高度 {}", message.getHeight());
//	//			return;
//	//		} else if((meetingStatus == 1 || meetingStatus == 2) && message.getHeight() != bestblockHeight) {
//	//			//就绪和顺序共识过程中，只接收当前轮的消息
//	//			log.warn("错误的顺序共识高度 {}", message.getHeight());
//	//			return;
//	//		}
//			
//			List<ConsensusMessage> sequencesList = sequences.get(message.getHeight());
//			if(sequencesList == null) {
//				sequencesList = new ArrayList<ConsensusMessage>();
//				sequencesList.add(message);
//				sequences.put(message.getHeight(), sequencesList);
//			} else {
//				//过滤重复节点
//				boolean exist = false;
//				for (ConsensusMessage sequences : sequencesList) {
//					if(Arrays.equals(sequences.getHash160(), message.getHash160())) {
//						exist = true;
//						sequences.setContent(message.getContent());
//						sequences.setNonce(message.getNonce());
//						sequences.setTime(message.getTime());
//						sequences.setSigns(message.getSigns());
//						break;
//					}
//				}
//				if(!exist) {
//					sequencesList.add(message);
//				}
//			}
//		} finally {
//			sequenceLocker.unlock();
//		}
//	}
//
//	/**
//	 * 消息是否接收
//	 * @param msid
//	 * @return boolean
//	 */
//	@Override
//	public boolean messageHasReceived(Sha256Hash msid) {
//		return messages.containsKey(msid);
//	}
//
//	/**
//	 * 获取一个已接收的共识消息，如果没有则会返回null
//	 * @param msid
//	 * @return ConsensusMessage
//	 */
//	@Override
//	public ConsensusMessage getMeetingMessage(Sha256Hash msid) {
//		return messages.get(msid);
//	}
//
//	/**
//	 * 异步启动共识服务
//	 */
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
//	long waitTime,waitedTime;
//	AtomicInteger counter = new AtomicInteger();
//	/*
//	 * 执行共识流程
//	 */
//	private void meeting() {
//		if(counter.getAndIncrement() % 10 == 0) {
//			log.info("========================="+System.currentTimeMillis()+"   status: "+meetingStatus+"    bestblockHeight: "+bestblockHeight+"    nextblockHeigth:"+nextMeetingHeight+"     currentMeetingHeight:"+currentMeetingHeight+"  readys: "+readys.get(currentMeetingHeight)+"   sequences: "+sequences.get(currentMeetingHeight));
//		}
//		bestblockHeight = network.getBestBlockHeight();
//		
//		//大于多少个共识节点后开始共识产块
//		if(meetingStatus == 0) {
//			if(waitTime == 0l) {
//				waitTime = TimeService.currentTimeMillis();
//			}
//			//等待10秒还没就绪，就强制就绪
//			if(TimeService.currentTimeMillis() - waitedTime > 10000l) {
//				sendReadyMessage();
//				meetingStatus = 1;
//			}
//		} if(meetingStatus == 1) {
//			List<ConsensusMessage> list = readys.get(currentMeetingHeight);
//			
//			if(waitedTime == 0l) {
//				waitedTime = TimeService.currentTimeMillis();
//			}
//			
//			if(list == null || list.size() < 3) {
//				waitTime = TimeService.currentTimeMillis();
//				
//				if(TimeService.currentTimeMillis() - waitedTime > 5000l) {
//					sendReadyMessage();
//				}
//				return;
//			}
//			if(TimeService.currentTimeMillis() - waitTime < 3000l) {
//				return;
//			}
//			meetingStatus = 2;
//			//顺序共识主动上报顺序
//			sequenceConsensus(list);
//			
//			currentScanningReceiveSequenceTime = TimeService.currentTimeMillis();
//		} else if(meetingStatus == 2) {
//			//顺序共识
//			sequenceMeeting();
//		} else if(meetingStatus == 3) {
//			//共识打包数据中，当前是否轮到我打包
//			if(nextMeetingHeight == bestblockHeight) {
//				newConsensus();
//				return;
//			}
//			//如果当前正在打包，则忽略
//			if(packageing) {
//				return;
//			}
//			//当前打包的人
//			List<ConsensusMessage> sequencelist = sequences.get(currentMeetingHeight);
//			int index = (int) (nextMeetingHeight - bestblockHeight);
//			int currentIndex = sequencelist.size() - index;
//			
//			ConsensusMessage consensusMessage = sequencelist.get(0);
//			
//			byte[] currentHash160 = new byte[Address.LENGTH];
//			System.arraycopy(consensusMessage.getContent(), 1 + currentIndex * Address.LENGTH, currentHash160, 0, Address.LENGTH);
//			
//			if(Arrays.equals(currentHash160, account.getAddress().getHash160())) {
//				packageData();
//			}
//		}
//	}
//
//	/*
//	 * 新一轮共识开始
//	 */
//	private void newConsensus() {
//		waitedTime = 0l;
//		meetingStatus = 1;
//		currentMeetingHeight = nextMeetingHeight;
//		readyMessageCacher = null;
//		sendReadyMessage();
//	}
//
//	/*
//	 * 达成顺序共识
//	 */
//	private void sequenceMeeting() {
//		//顺序共识中
//		List<ConsensusMessage> readylist = readys.get(currentMeetingHeight);
//		List<ConsensusMessage> sequencelist = sequences.get(currentMeetingHeight);
//		
//		//避免启动时没有来得及初始化出错
//		if(readylist == null || sequencelist == null) {
//			return;
//		}
//		
//		int newestReadyCount = readylist.size();
//		int newestSequenceCount = sequencelist.size();
//		
//		//是否一致
//		if(newestReadyCount == newestSequenceCount) {
//			//数量达成一致，但是需要最终达成完全一致，才进行出块处理，也就是共识顺序需要达成一致
//			byte[] content = null;
//			for (ConsensusMessage consensusMessage : sequencelist) {
//				if(content == null) {
//					content = consensusMessage.getContent();
//				} else if(!Arrays.equals(content, consensusMessage.getContent())) {
//					//出现不一致的情况，那么具体分析
////					analysisSequenceConsensusMessage();
//					return;
//				}
//			}
//			
//			int count = (content.length - 1 ) / Address.LENGTH;
//			if((content.length - 1 ) % Address.LENGTH != 0 || newestSequenceCount != count) {
//				//数量不一致，重新共识
//				return;
//			}
//			
//			//达成一致，开始共识
//			meetingStatus = 3;
//			nextMeetingHeight = currentMeetingHeight + newestSequenceCount;
//		} else {
//			//检查就绪队列是否有更新
//			if(currentSendSequenceCount != newestReadyCount) {
//				//更新顺序共识
//				sequenceConsensus(readylist);
//				return;
//			}
//			//如果有共识节点down机或者断网，或者恶意节点故意破坏，迟迟不能达成顺序共识，那么进行超时处理
//			if(1==1)return;
//			//数量有变化，则更新变化时间和最新数量
//			if(currentReceiveSequenceCount != newestSequenceCount) {
//				currentReceiveSequenceCount = newestSequenceCount;
//				currentScanningReceiveSequenceTime = TimeService.currentTimeMillis();
//			}
//
//			//如果10s没有变化，那么排除没有发送的节点，重新进行顺序共识
//			if(currentReceiveSequenceCount == newestSequenceCount && TimeService.currentTimeMillis() - currentScanningReceiveSequenceTime >= 10000l) {
//				//先找出来是哪些节点没有发送消息
//				//把readylist和sequencelist对比就能发现异常节点
//				List<ConsensusMessage> diffList = new ArrayList<ConsensusMessage>();
//				for (ConsensusMessage readyMessage : readylist) {
//					boolean exist = false;
//					for (ConsensusMessage sequenceMessage : sequencelist) {
//						if(Arrays.equals(readyMessage.getHash160(), sequenceMessage.getHash160())) {
//							exist = true;
//							break;
//						}
//					}
//					if(!exist) {
//						diffList.add(readyMessage);
//					}
//				}
//				//把这些异常节点加入黑名单，本轮不再接受任何这些节点发送的消息
//				List<List<ConsensusMessage>> blacklist = sequenceBlacklist.get(currentMeetingHeight);
//				if(blacklist == null) {
//					blacklist = new ArrayList<List<ConsensusMessage>>();
//					sequenceBlacklist.put(currentMeetingHeight, blacklist);
//				}
//				blacklist.add(diffList);
//				
//				//备份顺序共识消息
//				List<List<ConsensusMessage>> backuplist = sequenceBackupList.get(currentMeetingHeight);
//				if(backuplist == null) {
//					backuplist = new ArrayList<List<ConsensusMessage>>();
//					sequenceBackupList.put(currentMeetingHeight, backuplist);
//				}
//				backuplist.add(sequencelist);
//				
//				//备份之后，清空重置队列
//				sequences.put(currentMeetingHeight, new ArrayList<ConsensusMessage>());
//				
//				//移除
//				readylist.removeAll(diffList);
//				
//				//发送新的顺序共识消息
//				sequenceConsensus(readylist);
//			}
//		}
//	}
//
//	/*
//	 * 当顺序共识不能达成一致时，则具体分析原因，并做出相应的处理
//	 */
//	private void analysisSequenceConsensusMessage() {
//
//		List<ConsensusMessage> readylist = readys.get(currentMeetingHeight);
//		List<ConsensusMessage> sequenceList = sequences.get(currentMeetingHeight);
//		
//		//异常名单
//		List<ConsensusMessage> exceptionList = new ArrayList<ConsensusMessage>();
//		
//		//数量低于共识节点的列表
//		List<ConsensusMessage> countLessList = new ArrayList<ConsensusMessage>();
//		
//		//发送的消息，不在就绪队列里，或者顺序不正确，则直接排除
//		for (ConsensusMessage sequenceMessage : sequenceList) {
//			//共识顺序列表
//			byte[] content = sequenceMessage.getContent();
//			byte[] sequence = new byte[content.length - 1];
//			System.arraycopy(content, 1, sequence, 0, sequence.length);
//			
//			if(sequence.length % Address.LENGTH != 0) {
//				//数量不正确，加入异常名单
//				exceptionList.add(sequenceMessage);
//				continue;
//			}
//			List<byte[]> consensusNodes = new ArrayList<byte[]>();
//			
//			//解析所有节点
//			for (int i = 0; i < sequence.length; i+= Address.LENGTH) {
//				byte[] consensusNode = new byte[Address.LENGTH];
//				System.arraycopy(sequence, i, consensusNode, 0, Address.LENGTH);
//				consensusNodes.add(consensusNode);
//			}
//			
//			//数量相符不
//			if(consensusNodes.size() != sequenceList.size()) {
//				countLessList.add(sequenceMessage);
//			}
//			
//			//临时就绪消息队列，用于验证顺序
//			List<ConsensusMessage> tempConsensusMessageList = new ArrayList<ConsensusMessage>();
//			//是否都在就绪队列里
//			//是否有异常
//			boolean hasException = false;
//			for (byte[] hash160 : consensusNodes) {
//				boolean existInReadys = false;
//				for (ConsensusMessage readyMessage : readylist) {
//					if(Arrays.equals(hash160, readyMessage.getHash160())) {
//						existInReadys = true;
//						tempConsensusMessageList.add(readyMessage);
//						break;
//					}
//				}
//				if(!existInReadys) {
//					//不在就绪队列里，异常
//					hasException = true;
//					break;
//				}
//			}
//			//是否有异常
//			if(hasException) {
//				//加入异常名单
//				exceptionList.add(sequenceMessage);
//				break;
//			}
//			
//			//验证顺序
//			tempConsensusMessageList.sort(new Comparator<ConsensusMessage>() {
//				@Override
//				public int compare(ConsensusMessage o1, ConsensusMessage o2) {
//					return o1.getId().compareTo(o2.getId());
//				}
//			});
//			for (int i = 0; i < consensusNodes.size(); i++) {
//				if(!Arrays.equals(consensusNodes.get(i), tempConsensusMessageList.get(i).getHash160())) {
//					//顺序异常，加入异常名单
//					exceptionList.add(sequenceMessage);
//					break;
//				}
//			}
//		}
//		
//		//异常名单已出来，做相应的处理
//		if(exceptionList.size() > 0) {
//			refreshSequenceConsensus(readylist, sequenceList, exceptionList);
//			return;
//		}
//		
//		//执行到这里，说明是某些节点的共识数量不一致
//		//诚信节点会再次更新顺序共识消息，以达成一致，如果是恶意节点，或者是网络环境极差的诚信节点，那么有可能一直不会更新
//		//那么对于迟迟不更新的节点，做超时处理
//		//因为诚信节点数量都会与就绪队列一致，出现这种情况，原因有2个，第1个别共识节点性能或者网络环境差，导致更新很慢（不考虑消息丢失情况，因为每个消息会在全网重复广播，正常连接多个节点，是一定会收到消息的），
//		//第2个原因，就是恶意节点故意破坏，所以出现这种情况，直接把共识顺序数量少于就绪数量的超时排除掉就行
//		
//		//如果5s没有变化，那么排除没有发送的节点，重新进行顺序共识
//		if(countLessList.size() > 0) {
//			if(TimeService.currentTimeMillis() - currentScanningReceiveSequenceTime >= 5000l) {
//				refreshSequenceConsensus(readylist, sequenceList, countLessList);
//				return;
//			}
//		}
//		
//		//如果到这里，那么说明系统异常了
//		log.error("共识异常，时间：{}   status：{}    bestblockHeight: {}    nextblockHeigth：{}     currentMeetingHeight：{}    readys：{}   sequences：{}", System.currentTimeMillis(), meetingStatus, bestblockHeight, nextMeetingHeight, currentMeetingHeight, readys.get(currentMeetingHeight), sequences.get(currentMeetingHeight));
//	}
//
//	/*
//	 * 刷新顺序共识消息
//	 */
//	private void refreshSequenceConsensus(List<ConsensusMessage> readylist, List<ConsensusMessage> sequenceList,
//			List<ConsensusMessage> exceptionList) {
//		//把这些异常节点加入黑名单，本轮不再接受任何这些节点发送的消息
//		List<List<ConsensusMessage>> blacklist = sequenceBlacklist.get(currentMeetingHeight);
//		if(blacklist == null) {
//			blacklist = new ArrayList<List<ConsensusMessage>>();
//			sequenceBlacklist.put(currentMeetingHeight, blacklist);
//		}
//		blacklist.add(exceptionList);
//		
//		//备份顺序共识消息
//		List<List<ConsensusMessage>> backuplist = sequenceBackupList.get(currentMeetingHeight);
//		if(backuplist == null) {
//			backuplist = new ArrayList<List<ConsensusMessage>>();
//			sequenceBackupList.put(currentMeetingHeight, backuplist);
//		}
//		backuplist.add(sequenceList);
//		
//		//备份之后，清空重置队列
//		sequences.put(currentMeetingHeight, new ArrayList<ConsensusMessage>());
//		
//		//移除
//		List<ConsensusMessage> diffList = new ArrayList<ConsensusMessage>();
//		for (ConsensusMessage exceptionNode : exceptionList) {
//			for (ConsensusMessage readyMessage : readylist) {
//				if(Arrays.equals(readyMessage.getHash160(), exceptionNode.getHash160())) {
//					diffList.add(readyMessage);
//					break;
//				}
//			}
//		}
//		readylist.removeAll(diffList);
//		
//		//发送新的顺序共识消息
//		sequenceConsensus(readylist);
//	}
//
//	/*
//	 * 打包数据
//	 */
//	private void packageData() {
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
//	 * 发送准备就绪消息
//	 */
//	private void sendReadyMessage() {
//		sendReadyMessage(currentMeetingHeight);
//	}
//	
//	/*
//	 * 发送准备就绪消息
//	 */
//	private void sendReadyMessage(long height) {
//		
//		if(readyMessageCacher != null && currentMeetingHeight == height) {
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
//	 * 上报共识顺序，需要所有的准备就绪的节点达成一致，否则分析具体原因并做相应处理
//	 * 在达成共识顺序过程中，有可能还会继续接收到准备就绪的消息，所以需要不断重试最终达成一致
//	 */
//	private void sequenceConsensus(List<ConsensusMessage> list) {
//		readerMessageLocker.lock();
//		try {
//			//确定顺序，按照msgid来
//			list.sort(new Comparator<ConsensusMessage>() {
//				@Override
//				public int compare(ConsensusMessage o1, ConsensusMessage o2) {
//					return o1.getId().compareTo(o2.getId());
//				}
//			});
//			
//			//content格式第一位为type,4为共识顺序
//			byte[] content = new byte[list.size() * Address.LENGTH + 1];
//			content[0] = 4;
//			
//			int index = 0;
//			for (ConsensusMessage consensusMessage : list) {
//				byte[] hash160 = consensusMessage.getHash160();
//				System.arraycopy(hash160, 0, content, index*Address.LENGTH + 1, hash160.length);
//				index++;
//			}
//			
//			ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), currentMeetingHeight, content);
//			//签名共识消息
//			message.sign(account);
//			
//			broadcastMessage(message);
//			
//			currentSendSequenceCount = list.size();
//			
//			if(log.isDebugEnabled()) {
//				log.debug("高度{}轮的共识，已发送顺序共识消息，共识节点一共{}个", nextMeetingHeight, currentSendSequenceCount);
//			}
//		} catch (Exception e) {
//			log.error("发送顺序共识消息出错", e);
//		} finally {
//			readerMessageLocker.unlock();
//		}
//	}
//
//	@Override
//	public void setAccount(Account account) {
//		this.account = account;
//	}
//
//	@Override
//	public void stop() {
//		executor.shutdownNow();
//	}
//
//	@Override
//	public ConsensusInfos getCurrentConsensusInfos(long height) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//}
