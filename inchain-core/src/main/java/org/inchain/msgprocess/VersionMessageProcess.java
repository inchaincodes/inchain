package org.inchain.msgprocess;

import java.util.Locale;

import org.inchain.SpringContextUtils;
import org.inchain.core.DownloadHandler;
import org.inchain.core.Peer;
import org.inchain.core.exception.ProtocolException;
import org.inchain.kits.PeerKit;
import org.inchain.message.Message;
import org.inchain.message.VerackMessage;
import org.inchain.message.VersionMessage;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 收到一个版本消息，必须回应一个包含自己版本的信息
 * @author ln
 *
 */
@Service
public class VersionMessageProcess implements MessageProcess {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(VersionMessageProcess.class);
	
	@Autowired
	private PeerKit peerKit;
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		VersionMessage versionMessage = (VersionMessage) message;
		
		if (peer.getPeerVersionMessage() != null)
            throw new ProtocolException("Got two version messages from peer");
		peer.setPeerVersionMessage(versionMessage);
		
        // Switch to the new protocol version.
        long peerTime = versionMessage.time * 1000;
        log.info("Got host={}, version={}, subVer='{}', services=0x{}, time={}, blocks={}",
        		peer.getAddress(),
                versionMessage.clientVersion,
                versionMessage.subVer,
                versionMessage.localServices,
                String.format(Locale.getDefault(), "%tF %tT", peerTime, peerTime),
                versionMessage.bestHeight);
        
        if (versionMessage.bestHeight < 0) {
            // In this case, it's a protocol violation.
            throw new ProtocolException("Peer reports invalid best height: " + versionMessage.bestHeight);
        }
        
        //回应自己的版本信息
		peer.sendMessage(new VerackMessage(peer.getNetwork(), peer.getNetwork().getBestBlockHeight(), 
				peer.getPeerAddress()));
		
		//区块下载同步器
		DownloadHandler downloadHandler = SpringContextUtils.getBean(DownloadHandler.class);
		downloadHandler.newPeer(peer);
		
		return null;
	}
}
