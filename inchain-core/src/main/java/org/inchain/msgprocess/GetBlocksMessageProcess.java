package org.inchain.msgprocess;

import javax.xml.ws.WebFault;

import org.inchain.SpringContextUtils;
import org.inchain.core.Peer;
import org.inchain.message.BlockMessage;
import org.inchain.message.GetBlocksMessage;
import org.inchain.message.Message;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 区块信息获取消息处理器
 * @author ln
 *
 */
@Service
public class GetBlocksMessageProcess implements MessageProcess {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public GetBlocksMessageProcess() {
		
	}
	
	/**
	 * 接收到区块消息，进行区块合法性验证，如果验证通过，则收录，然后转发区块
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive get block message : {}", message);
		}
		
		GetBlocksMessage getBlockMessage = (GetBlocksMessage) message;
		//要获取的区块，从哪里开始
		long startBlockHeight = getBlockMessage.getStartBlockHeight();
		//要获取的数量
		long count = getBlockMessage.getCount();
		
		//得到区块提供器
		BlockStoreProvider blockStore = SpringContextUtils.getBean(BlockStoreProvider.class);
		
		for (int i = 1; i <= count; i++) {
			//查询
			BlockStore block = blockStore.getBlockByHeight(startBlockHeight + i);
			
			
			BlockMessage blockMessage = new BlockMessage(peer.getNetwork(), block);
			
			peer.sendMessage(blockMessage);
			
		}
		
		return null;
	}
}
