package org.inchain.consensus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.message.ConsensusMessage;
import org.inchain.network.NetworkParams;
import org.inchain.signers.ConsensusSigner;
import org.inchain.store.BlockStoreProvider;
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
	
	//所以消息队列
	private static Map<Sha256Hash, ConsensusMessage> messages = new HashMap<Sha256Hash, ConsensusMessage>();
	//准备就绪队列
	private static Map<Long, List<ConsensusMessage>> readys = new HashMap<Long, List<ConsensusMessage>>();
	//每轮共识达成发言顺序
	private static Map<Long, List<ConsensusMessage>> sequences = new HashMap<Long, List<ConsensusMessage>>();
	
	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private Mining mining;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
	//会议状态，0待开始，1准备就绪，2顺序共识中，3共识打包成功中，4一轮结束
	private int meetingStatus = 0;
	//当前最新高度，
	private long bestblockHeight;
	//当前轮共识开始高度
	private long currentMeetingHeight;
	//下一轮共识高度
	private long nextMeetingHeight;
	//当前共识账号
	private Account account;
	//当前是否正在打包
	private boolean packageing;
	
	@Override
	public void sendMeetingMessage(ConsensusMessage message) {
		peerKit.broadcastMessage(message);
	}

	@Override
	public void receiveMeetingMessage(Sha256Hash msid, ConsensusMessage message) {
		if(msid == null) {
			return;
		}
		//如果已经接收，则不处理
		if(messageHasReceive(msid)) {
			return;
		}
		messages.put(msid, message);
		//如果是准备就绪消息，则放入准备就绪队列
		byte[] bodys = message.getContent();
		byte type = bodys[0];
		if(type == 1) {
			//有可能恶意节点欺诈，所以需要排除重复的节点
			List<ConsensusMessage> readyList = readys.get(message.getHeight());
			if(readyList == null) {
				readyList = new ArrayList<ConsensusMessage>();
				readyList.add(message);
				readys.put(message.getHeight(), readyList);
			} else {
				//过滤重复节点
				boolean exist = false;
				for (ConsensusMessage ready : readyList) {
					if(Arrays.equals(ready.getHash160(), message.getHash160())) {
						exist = true;
						break;
					}
				}
				if(!exist) {
					readyList.add(message);
				}
			}
			//自己是否准备就绪
			if(account != null && Arrays.equals(account.getAddress().getHash160(), message.getHash160())) {
				meetingStatus = 1;
			}
		} else if(type == 2) {
			List<ConsensusMessage> sequencesList = sequences.get(message.getHeight());
			if(sequencesList == null) {
				sequencesList = new ArrayList<ConsensusMessage>();
				sequencesList.add(message);
				sequences.put(message.getHeight(), sequencesList);
			} else {
				//过滤重复节点
				boolean exist = false;
				for (ConsensusMessage sequences : sequencesList) {
					if(Arrays.equals(sequences.getHash160(), message.getHash160())) {
						exist = true;
						sequences.setContent(message.getContent());
						sequences.setNonce(message.getNonce());
						sequences.setTime(message.getTime());
						sequences.setSign(message.getSign());
						break;
					}
				}
				if(!exist) {
					sequencesList.add(message);
				}
			}
		}
	}
	
	/**
	 * 消息是否接收
	 * @param msid
	 * @return boolean
	 */
	@Override
	public boolean messageHasReceive(Sha256Hash msid) {
		return messages.containsKey(msid);
	}

	@Override
	public boolean nextIsMy() {
		return false;
	}

	@Override
	public void newBlockBroadcast() {
		
	}

	/**
	 * 异步启动共识服务
	 */
	@Override
	public void startSyn(Account account) {
		this.account = account;
		
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

	/*
	 * 执行共识流程
	 */
	private void meeting() {
		log.info("========================="+System.currentTimeMillis()+"   status: "+meetingStatus+"    bestblockHeight: "+bestblockHeight+"    nextblockHeigth:"+nextMeetingHeight+"     currentMeetingHeight:"+currentMeetingHeight+"  readys: "+readys.get(currentMeetingHeight)+"   sequences: "+sequences.get(currentMeetingHeight));
		
		bestblockHeight = blockStoreProvider.getBestBlockHeader().getHeight();
		
//		int i = 0;
//		for (Entry<Sha256Hash, ConsensusMessage> entry : messages.entrySet()) {
//			i++;
//			log.debug("消息{}:{} - {}", i, entry.getKey(), entry.getValue());
//		}
		
		//大于多少个共识节点后开始共识产块
		if(meetingStatus == 1) {
			List<ConsensusMessage> list = readys.get(bestblockHeight);
			if(list == null || list.size() < 2) {
				return;
			}
			meetingStatus = 2;
			//顺序共识主动上报顺序
			sequenceConsensus(list);
		} else if(meetingStatus == 2) {
			//顺序共识中
			List<ConsensusMessage> readylist = readys.get(bestblockHeight);
			List<ConsensusMessage> sequencelist = sequences.get(bestblockHeight);
			if(readylist.size() == sequencelist.size()) {
				//是否一致
				
				byte[] content = null;
				for (ConsensusMessage consensusMessage : sequencelist) {
					if(content == null) {
						content = consensusMessage.getContent();
					} else if(!Arrays.equals(content, consensusMessage.getContent())) {
						return;
					}
				}
				
				//达成一致，开始共识
				meetingStatus = 3;
				currentMeetingHeight = bestblockHeight;
				nextMeetingHeight = bestblockHeight + sequencelist.size();
			}
		} else if(meetingStatus == 3) {
			//共识打包数据中，当前是否轮到我打包
			if(nextMeetingHeight == bestblockHeight) {
				meetingStatus = 1;
				sendReadyMessage();
				return;
			}
			//当前打包的人
			List<ConsensusMessage> sequencelist = sequences.get(currentMeetingHeight);
			int index = (int) (nextMeetingHeight - bestblockHeight);
			int currentIndex = sequencelist.size() - index;
			
			ConsensusMessage consensusMessage = sequencelist.get(0);
			
			byte[] currentHash160 = new byte[Address.LENGTH];
			System.arraycopy(consensusMessage.getContent(), 1 + currentIndex * Address.LENGTH, currentHash160, 0, Address.LENGTH);
			
			if(!packageing && Arrays.equals(currentHash160, account.getAddress().getHash160())) {
				packageData();
			}
		}
	}

	/*
	 * 打包数据
	 */
	private void packageData() {
		packageing = true;
		
		try {
			mining.mining();
		} catch (Exception e) {
			log.error("mining err", e);
		}

		packageing = false;
	}

	/*
	 * 发送准备就绪消息
	 */
	private void sendReadyMessage() {
		//content格式第一位为type,1为拉取共识状态信息
		byte[] content = new byte[]{ 1 };
		
		ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), bestblockHeight, content);
		//签名共识消息
		ConsensusSigner.sign(message, ECKey.fromPrivate(new BigInteger(account.getPriSeed())));
		sendMeetingMessage(message);
	}

	/*
	 * 上报共识顺序，需要所有的准备就绪的节点达成一致，否则分析具体原因并做相应处理
	 * 在达成共识顺序过程中，有可能还会继续接收到准备就绪的消息，所以需要不断重试最终达成一致
	 */
	private void sequenceConsensus(List<ConsensusMessage> list) {
		
		//确定顺序，按照msgid来
		list.sort(new Comparator<ConsensusMessage>() {
			@Override
			public int compare(ConsensusMessage o1, ConsensusMessage o2) {
				if(o1.getId() == null) {
					o1.setId(Sha256Hash.twiceOf(o1.baseSerialize()));
				}
				if(o2.getId() == null) {
					o2.setId(Sha256Hash.twiceOf(o2.baseSerialize()));
				}
				return o1.getId().compareTo(o2.getId());
			}
		});
		
		//content格式第一位为type,2为共识顺序
		byte[] content = new byte[list.size() * Address.LENGTH + 1];
		content[0] = 2;
		
		int index = 0;
		for (ConsensusMessage consensusMessage : list) {
			byte[] hash160 = consensusMessage.getHash160();
			System.arraycopy(hash160, 0, content, index*Address.LENGTH + 1, hash160.length);
			index++;
		}
		
		ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), bestblockHeight, content);
		//签名共识消息
		ConsensusSigner.sign(message, ECKey.fromPrivate(new BigInteger(account.getPriSeed())));
		
		sendMeetingMessage(message);
	}
}
