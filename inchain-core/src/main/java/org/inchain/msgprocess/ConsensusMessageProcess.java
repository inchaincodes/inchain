package org.inchain.msgprocess;

import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.Peer;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.Message;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 共识消息，整个系统的共识流程步骤，都在此体现，所有节点可见
 * @author ln
 *
 */
@Service
public class ConsensusMessageProcess implements MessageProcess {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ConsensusPool consensusPool;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	
	/**
	 * 接收到共识消息，在此做2个验证，第一签名是否正确，第二是否是共识节点发出的消息，验证通过之后，就放到共识会议记录里面去，然后转发该条消息
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		ConsensusMessage consensusMessage = (ConsensusMessage) message;
		
		MessageProcessResult result = new MessageProcessResult(consensusMessage.getId(), false);
		
		//验证签名是否正确
		//所有节点参与共识，必须以书面协议形式进行签名，确保恶意节点能被追踪处理
		if(consensusMessage.getSigns() == null) {
			log.warn("缺少签名的共识消息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return result;
		}
		
		if(!consensusPool.contains(consensusMessage.getHash160())) {
			log.warn("非共识节点违规共识消息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return result;
		}
		
		//判断是否已经接收过的消息
		//TODO 这里可以用布隆过滤器实现
		
		Sha256Hash msid = consensusMessage.getId();
		if(consensusMeeting.messageHasReceived(msid)) {
			log.info("共识消息{},已经处理过", msid.toString());
			if(log.isDebugEnabled()) {
				log.debug("共识消息{},已经处理过", msid.toString());
			}
			return result;
		}
		
		//验证签名
		byte[][] pubkeys = consensusPool.getPubkey(consensusMessage.getHash160());
		if(pubkeys == null) {
			log.warn("该共识节点缺少公钥信息，hash160: {}", Hex.encode(consensusMessage.getHash160()));
			return result;
		}
		try {
			consensusMessage.verfify(pubkeys);
		} catch (Exception e) {
			log.info("共识消息{},验证出错：{}", msid.toString(), e.getMessage());
			if(log.isDebugEnabled()) {
				log.debug("共识消息{},验证出错：{}", msid.toString(), e.getMessage());
			}
			return result;
		}
		
		if(log.isDebugEnabled()) {
			log.debug("共识节点消息验证通过：{}", consensusMessage);
		}
		
		//验证通过
		//除拉取共识状态之外的消息都转发
		byte[] content = consensusMessage.getContent();
		if(content[0] == 1 || content[0] == 2) {
			//拉取和回应共识状态消息，不转发
			consensusMessage.setPeer(peer);
			//加入共识会议记录器
			consensusMeeting.receiveMeetingMessage(msid, consensusMessage);
		} else {
			//加入共识会议记录器
			consensusMeeting.receiveMeetingMessage(msid, consensusMessage);
			//转发消息
			consensusMeeting.broadcastMessage(consensusMessage);
		}
		
		return new MessageProcessResult(consensusMessage.getId(), true);
	}

}
