package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.message.Message;
import org.inchain.message.PingMessage;
import org.inchain.message.PongMessage;
import org.slf4j.LoggerFactory;

public class PingMessageProcess implements MessageProcess {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PingMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		log.info("{} {}", peer.getAddress(), message);
		peer.sendMessage(new PongMessage(((PingMessage)message).getNonce()));
		return null;
	}
}
