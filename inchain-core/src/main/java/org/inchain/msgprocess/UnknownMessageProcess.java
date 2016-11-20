package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.message.Message;
import org.slf4j.LoggerFactory;

public class UnknownMessageProcess implements MessageProcess {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(UnknownMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		log.warn("receive unknown message {}", message);
		return null;
	}
}
