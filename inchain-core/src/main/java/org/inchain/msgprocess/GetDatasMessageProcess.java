package org.inchain.msgprocess;

import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.core.Peer;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.GetDatasMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.Message;
import org.inchain.message.NewBlockMessage;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 清单数据下载处理器
 * @author ln
 *
 */
@Service
public class GetDatasMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(GetDatasMessageProcess.class);
	
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
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
			} else if(inventoryItem.getType() == InventoryItem.Type.Transaction) {
				//交易数据获取
				txInventory(inventoryItem, peer);
			} else {
				//TODO
			}
		}
		
		return null;
	}

	/**
	 * 获取交易数据
	 * @param inventoryItem
	 * @param peer
	 */
	private void txInventory(InventoryItem inventoryItem, Peer peer) {
		//首先查看内存里面有没有交易
		Transaction tx = MempoolContainerMap.getInstace().get(inventoryItem.getHash());
		if(tx == null) {
			//内存里面没有，则查询存储
			TransactionStore ts = blockStoreProvider.getTransaction(inventoryItem.getHash().getBytes());
			if(ts == null || ts.getTransaction() == null) {
				//数据没找到，回应notfound
				//TODO
				return;
			} else {
				tx = ts.getTransaction();
			}
		}
		peer.sendMessage(tx);
	}

	/*
	 * 下载新区块
	 */
	private void newBlockInventory(InventoryItem inventoryItem, Peer peer) {
		BlockStoreProvider blockStoreProvider = SpringContextUtils.getBean(BlockStoreProvider.class);
		BlockStore blockStore = blockStoreProvider.getBlock(inventoryItem.getHash().getBytes());
		peer.sendMessage(new NewBlockMessage(peer.getNetwork(), blockStore.getBlock().baseSerialize()));
	}

}
