package org.inchain.msgprocess;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.List;

import org.inchain.consensus.ConsensusMeeting;
import org.inchain.core.Peer;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.DataNotFoundMessage;
import org.inchain.message.GetDatasMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.Message;
import org.inchain.message.NewBlockMessage;
import org.inchain.network.NetworkParams;
import org.inchain.service.BlockForkService;
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
	private NetworkParams network;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private BlockForkService blockForkService;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private TransactionMessageProcess transactionMessageProcess;
	
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
			} else if(inventoryItem.getType() == InventoryItem.Type.Block){
				//获取区块数据
				Block block = getBlock(inventoryItem);
				if(block == null) {
					sendMessage(peer, new DataNotFoundMessage(network, inventoryItem.getHash()));
				} else {
					sendMessage(peer, block);
				}
			} else if(inventoryItem.getType() == InventoryItem.Type.Consensus) {
				//共识消息
				ConsensusMessage consensusMessage = consensusMeeting.getMeetingMessage(inventoryItem.getHash());
				if(consensusMessage == null) {
					sendMessage(peer, new DataNotFoundMessage(network, inventoryItem.getHash()));
					continue;
				}
				sendMessage(peer, consensusMessage);
			}
		}
		
		return null;
	}

	private void sendMessage(Peer peer, Message message) {
		try {
			peer.sendMessage(message);
		} catch (NotYetConnectedException | IOException e) {
			if(log.isDebugEnabled()) {
				log.debug("发送消息出错，可能原因是连接已关闭", e.getMessage());
			}
		}
	}

	/**
	 * 获取交易数据
	 * @param inventoryItem
	 * @param peer
	 */
	private void txInventory(InventoryItem inventoryItem, Peer peer) {
		//首先查看内存里面有没有交易
		Transaction tx = MempoolContainer.getInstace().get(inventoryItem.getHash());
		if(tx == null) {
//			tx = transactionMessageProcess.getPendingTx(inventoryItem.getHash());
//			if(tx != null) {
//				sendMessage(peer, tx);
//				return;
//			}
			//内存里面没有，则查询存储
			TransactionStore ts = blockStoreProvider.getTransaction(inventoryItem.getHash().getBytes());
			if(ts == null || ts.getTransaction() == null) {
				//数据没找到，回应notfound
				sendMessage(peer, new DataNotFoundMessage(network, inventoryItem.getHash()));
				return;
			} else {
				tx = ts.getTransaction();
			}
		}
		sendMessage(peer, tx);
	}

	/*
	 * 下载新区块
	 */
	private void newBlockInventory(InventoryItem inventoryItem, Peer peer) {
		Block block = getBlock(inventoryItem);
		if(block == null) {
			sendMessage(peer, new DataNotFoundMessage(network, inventoryItem.getHash()));
		} else {
			sendMessage(peer, new NewBlockMessage(peer.getNetwork(), block.baseSerialize()));
		}
	}

	private Block getBlock(InventoryItem inventoryItem) {
		Block block = null;
		
		BlockStore blockStore = blockStoreProvider.getBlock(inventoryItem.getHash().getBytes());
		if(blockStore == null) {
			block = blockForkService.getBlock(inventoryItem.getHash());
		} else {
			block = blockStore.getBlock();
		}
		return block;
	}

}
