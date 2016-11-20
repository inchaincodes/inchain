package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.message.Message;
import org.slf4j.LoggerFactory;

public class PongMessageProcess implements MessageProcess {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PongMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		log.info("{} {}", peer.getAddress(), message);
        
		return null;
	}
}
