package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.message.Message;
import org.inchain.message.PingMessage;
import org.inchain.message.PongMessage;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PingMessageProcess implements MessageProcess {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PingMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		if(log.isDebugEnabled()) {
			log.debug("{} {}", peer.getAddress(), message);
		}
		return new MessageProcessResult(null, true, new PongMessage(((PingMessage)message).getNonce()));
	}
}
