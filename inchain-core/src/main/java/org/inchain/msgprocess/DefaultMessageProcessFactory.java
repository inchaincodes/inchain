package org.inchain.msgprocess;

import org.inchain.message.*;
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
		} else if(message instanceof Transaction) {
			return new TransactionMessageProcess(message.getNetwork());
		} else if(message instanceof BlockMessage) {
			return new BlockMessageProcess(message.getNetwork());
		} else {
			return new UnknownMessageProcess();
		}
	}
}
