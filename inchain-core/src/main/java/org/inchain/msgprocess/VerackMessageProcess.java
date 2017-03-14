package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.core.TimeService;
import org.inchain.core.exception.ProtocolException;
import org.inchain.kits.PeerKit;
import org.inchain.message.Message;
import org.inchain.message.VerackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 接收版本回应
 * @author ln
 *
 */
@Service
public class VerackMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(VerackMessageProcess.class);
	
	@Autowired
	private PeerKit peerKit;
	
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
        	
        	//处理网络时间
        	long localTime = TimeService.currentTimeMillis();
        	long time = verackMessage.getTime();
        	//设置时间偏移
        	long timeOffset = (time + peer.getSendVersionMessageTime()) / 2 - localTime;
        	peer.setTimeOffset(timeOffset);
        	
        	//处理本地时间,如果已经处理过了，就不再处理
        	if(!TimeService.netTimeHasInit()) {
        		peerKit.processTimeOffset(time, timeOffset);
        	}
        	
        	peer.setHandshake(true);
        	
        	log.info("时间偏差 {} 毫秒, {}", peer.getTimeOffset(), peer.getAddress().getSocketAddress());
        	
        	return new MessageProcessResult(null, true);
        }
      		
		return null;
	}
}
