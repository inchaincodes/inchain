package org.inchain.msgprocess;

import java.util.Locale;

import org.inchain.core.Peer;
import org.inchain.core.exception.ProtocolException;
import org.inchain.message.Message;
import org.inchain.message.VersionMessage;
import org.slf4j.LoggerFactory;

public class VerackMessageProcess implements MessageProcess {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(VerackMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if (peer.getPeerVersionMessage() != null) {
            throw new ProtocolException("got a version ack before version");
        }
		VersionMessage versionMessage = (VersionMessage) message;
		
		peer.setPeerVersionMessage(versionMessage);
        if (peer.isHandshake()) {
            throw new ProtocolException("got more than one version ack");
        }

        long peerTime = versionMessage.time * 1000;
        log.info("Connect success host={}, version={}, subVer='{}', services=0x{}, time={}, blocks={}",
        		peer.getAddress(),
                versionMessage.clientVersion,
                versionMessage.subVer,
                versionMessage.localServices,
                String.format(Locale.getDefault(), "%tF %tT", peerTime, peerTime),
                versionMessage.bestHeight);
        
        peer.setHandshake(true);
        
		return null;
	}
}
