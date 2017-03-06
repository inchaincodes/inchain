package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.message.DataNotFoundMessage;
import org.inchain.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 数据没有找到处理
 * @author ln
 *
 */
@Service
public class DataNotFoundMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(DataNotFoundMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive DataNotFoundMessage message: {}", message);
		}
		
		DataNotFoundMessage dataNotFoundMessage = (DataNotFoundMessage) message;
		
		
		return new MessageProcessResult(dataNotFoundMessage.getHash(), false);
	}
}
