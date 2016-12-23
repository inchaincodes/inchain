package org.inchain.msgprocess;

import org.inchain.SpringContextUtils;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.Peer;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.Message;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共识消息，整个系统的共识流程步骤，都在此体现，所有节点可见
 * @author ln
 *
 */
public class ConsensusMessageProcess implements MessageProcess {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * 接收到共识消息，在此做2个验证，第一签名是否正确，第二是否是共识节点发出的消息，验证通过之后，就放到共识会议记录里面去，然后转发该条消息
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		ConsensusMessage consensusMessage = (ConsensusMessage) message;
		
		//验证签名是否正确
		//所有节点参与共识，必须以书面协议形式进行签名，确保恶意节点能被追踪处理
		if(consensusMessage.getSign() == null) {
			log.warn("缺少签名的共识消息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return null;
		}
		
		ConsensusPool consensusPool = SpringContextUtils.getBean(ConsensusPool.class);
		if(!consensusPool.contains(consensusMessage.getHash160())) {
			log.warn("非共识节点违规共识消息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return null;
		}
		
		//判断是否已经接收过的消息
		//TODO 这里可以用布隆过滤器实现
		ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
		
		Sha256Hash msid = Sha256Hash.twiceOf(consensusMessage.baseSerialize());
		if(consensusMeeting.messageHasReceive(msid)) {
			if(log.isDebugEnabled()) {
				log.debug("共识消息{},已经处理过", msid.toString());
			}
			return null;
		}
		
		//验证签名
		byte[] pubkey = consensusPool.getPubkey(consensusMessage.getHash160());
		
		if(pubkey == null) {
			log.warn("该共识节点缺少公钥信息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return null;
		}
		ECKey key = ECKey.fromPublicOnly(pubkey);
		if(!key.verify(Sha256Hash.twiceOf(consensusMessage.getBodyBytes()).getBytes(), consensusMessage.getSign())) {
			log.warn("错误的共识消息签名信息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return null;
		}
		
		if(log.isDebugEnabled()) {
			log.debug("共识节点消息验证通过：{}", consensusMessage);
		}
		
		//验证通过
		//加入共识会议记录器
		consensusMeeting.receiveMeetingMessage(msid, consensusMessage);
		
		//转发消息
		//TODO
		PeerKit peerKit = SpringContextUtils.getBean(PeerKit.class);
		peerKit.broadcastMessage(consensusMessage);
		
		return null;
	}

}
