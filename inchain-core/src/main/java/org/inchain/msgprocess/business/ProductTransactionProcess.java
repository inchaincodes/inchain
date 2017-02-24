package org.inchain.msgprocess.business;

import org.inchain.core.Peer;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryItem.Type;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.message.RejectMessage;
import org.inchain.msgprocess.MessageProcess;
import org.inchain.msgprocess.MessageProcessResult;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 创建商品交易处理器
 * @author ln
 *
 */
@Service
public class ProductTransactionProcess implements MessageProcess {
	
	private static final Logger log = LoggerFactory.getLogger(ProductTransactionProcess.class);
	
	private MempoolContainer mempool = MempoolContainerMap.getInstace();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
	public ProductTransactionProcess() {
	}
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		ProductTransaction tx = (ProductTransaction) message;

		if(log.isDebugEnabled()) {
			log.debug("productTransaction message {}", Hex.encode(tx.baseSerialize()));
		}
		try {

			//验证交易的合法性
			tx.verfify();
			tx.verfifyScript();
			
			Sha256Hash id = tx.getHash();
			
			if(log.isDebugEnabled()) {
				log.debug("verify success! tx id : {}", id);
			}
			
			//逻辑验证，验证不通过抛出VerificationException
			verifyTx(tx);
			
			//加入内存池
			boolean res = mempool.add(tx);
			if(!res) {
				log.error("加入内存池失败："+ id);
			}
			
			//转发交易
			InventoryItem item = new InventoryItem(Type.Transaction, id);
			InventoryMessage invMessage = new InventoryMessage(peer.getNetwork(), item);
			peerKit.broadcastMessage(invMessage);
	
			//验证是否是转入到我账上的交易
			checkIsMine(tx);
			
			return new MessageProcessResult(tx.getHash(), true);
		} catch (Exception e) {
			log.error("tx error ", e);
			RejectMessage replyMessage = new RejectMessage(network, tx.getHash());
			return new MessageProcessResult(tx.getHash(), false, replyMessage);
		}
	}

	//逻辑验证，验证不通过抛出VerificationException
	private void verifyTx(ProductTransaction tx) throws VerificationException {
		//不能重复创建产品
		TransactionStore txs = blockStoreProvider.getTransaction(tx.getHash().getBytes());
		if(txs != null) {
			new VerificationException("不能重复创建产品");
		}
	}

	/*
	 * 是否转入到我的账上的交易，如果是，则通知
	 */
	private void checkIsMine(ProductTransaction tx) {
		byte[] hash160 = tx.getHash160();
		if(blockStoreProvider.getAccountFilter().contains(hash160)) {
			blockStoreProvider.updateMineTx(new TransactionStore(network, tx));
		}
	}
}
