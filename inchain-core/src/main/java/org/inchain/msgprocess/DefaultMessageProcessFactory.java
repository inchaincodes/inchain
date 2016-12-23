package org.inchain.msgprocess;

import org.inchain.message.*;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;

public class DefaultMessageProcessFactory implements MessageProcessFactory {

	private static final MessageProcessFactory INSTANCE = new DefaultMessageProcessFactory();

	private DefaultMessageProcessFactory() {
	}
	
	public static MessageProcessFactory getInstance() {
		return INSTANCE;
	}
	
	@Override
	public MessageProcess getFactory(Message message) {
		if(message instanceof PingMessage) {
			return new PingMessageProcess();
		} else if(message instanceof PongMessage) {
			return new PongMessageProcess();
		} else if(message instanceof VerackMessage) {
			return new VerackMessageProcess();
		} else if(message instanceof VersionMessage) {
			return new VersionMessageProcess();
		} else if(message instanceof RegConsensusTransaction) {
			//注册成为共识节点消息
			return new RegConsensusMessageProcess();
		}  else if(message instanceof Transaction) {
			return new TransactionMessageProcess();
		} else if(message instanceof BlockMessage) {
			return new BlockMessageProcess();
		} else if(message instanceof GetBlockMessage) {
			return new GetBlockMessageProcess();
		} else if(message instanceof ConsensusMessage) {
			return new ConsensusMessageProcess();
		} else {
			return new UnknownMessageProcess();
		}
	}
}
