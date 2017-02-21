package org.inchain.msgprocess;

import java.net.UnknownHostException;

import org.inchain.core.Peer;
import org.inchain.core.exception.ProtocolException;
import org.inchain.message.BlockHeader;
import org.inchain.message.Message;
import org.inchain.message.VerackMessage;
import org.inchain.message.VersionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 接收版本回应
 * @author ln
 *
 */
@Service
public class VerackMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(VerackMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {

		VerackMessage verackMessage = (VerackMessage) message;
		
        if (peer.isHandshake()) {
            throw new ProtocolException("got more than one version ack");
        }

        if(log.isDebugEnabled()) {
	        log.debug("Got verack host={}, time={}, nonce={}",
	        		peer.getAddress(),
	                verackMessage.getTime(),
	                verackMessage.getNonce());
        }
        
        //没有握手完成，则发送自己的版本信息，代表同意握手
        if(!peer.isHandshake()) {
        	//回应自己的版本信息
            BlockHeader bestBlockHeader = peer.getNetwork().getBestBlockHeader().getBlockHeader();
            
        	VersionMessage replyMessage;
			try {
				replyMessage = new VersionMessage(peer.getNetwork(), bestBlockHeader.getHeight(), bestBlockHeader.getHash(), peer.getPeerAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			}
        	peer.setHandshake(true);
        	
        	return new MessageProcessResult(null, true, replyMessage);
        }
      		
		return null;
	}
}
