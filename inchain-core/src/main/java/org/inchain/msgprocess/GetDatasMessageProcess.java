package org.inchain.msgprocess;

import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.core.Peer;
import org.inchain.message.GetDatasMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.Message;
import org.inchain.message.NewBlockMessage;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 清单数据下载处理器
 * @author ln
 *
 */
@Service
public class GetDatasMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(GetDatasMessageProcess.class);
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive getdatas message: {}", message);
		}
		
		GetDatasMessage getDatsMessage = (GetDatasMessage) message;
		
		List<InventoryItem> invList = getDatsMessage.getInvs();
		if(invList == null) {
			return null;
		}
		
		for (InventoryItem inventoryItem : invList) {
			if(inventoryItem.getType() == InventoryItem.Type.NewBlock) {
				//新区块数据获取
				newBlockInventory(inventoryItem, peer);
			} else {
				//TODO
			}
		}
		
		return null;
	}

	/*
	 * 下载新区块
	 */
	private void newBlockInventory(InventoryItem inventoryItem, Peer peer) {
		BlockStoreProvider blockStoreProvider = SpringContextUtils.getBean(BlockStoreProvider.class);
		BlockStore blockStore = blockStoreProvider.getBlock(inventoryItem.getHash().getBytes());
		peer.sendMessage(new NewBlockMessage(peer.getNetwork(), blockStore));
	}

}
