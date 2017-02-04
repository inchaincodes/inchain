package org.inchain.msgprocess;

import java.util.List;

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
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 交易消息
 * @author ln
 *
 */
@Service
public class TransactionMessageProcess implements MessageProcess {
	
	private static final Logger log = LoggerFactory.getLogger(TransactionMessageProcess.class);
	
	private MempoolContainer mempool = MempoolContainerMap.getInstace();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;
	
	public TransactionMessageProcess() {
	}
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		Transaction tx = (Transaction) message;

		if(log.isDebugEnabled()) {
			log.debug("transaction message {}", Hex.encode(tx.baseSerialize()));
		}
		try {
			//验证交易的合法性
			tx.verfifyScript();
			
			Sha256Hash id = tx.getHash();
			
			if(log.isDebugEnabled()) {
				log.debug("verify success! tx id : {}", id);
			}
			
			//交易逻辑验证，验证不通过抛出VerificationException
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
			RejectMessage replyMessage = new RejectMessage(network);
			//TODO
			return new MessageProcessResult(tx.getHash(), false, replyMessage);
		}
	}

	//交易逻辑验证，验证不通过抛出VerificationException
	private void verifyTx(Transaction tx) {
		int type = tx.getType();
		//帐户注册，hash160不能重复
		if(type == TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER) {
			
		} else if(type == TransactionDefinition.TYPE_CHANGEPWD) {
			
		} else if(type == TransactionDefinition.TYPE_PAY) {
			//普通交易，验证交易来源和交易金额是否正确
			List<Input> inputs = tx.getInputs();
			for (Input input : inputs) {
				TransactionInput tInput = (TransactionInput) input;
				TransactionOutput output = tInput.getFrom();
				if(output == null) {
					throw new VerificationException("error input");
				}
				Sha256Hash fromId = output.getParent().getHash();
//				trans
			}
			
		} else {
			throw new VerificationException("error transaction");
		}
	}

	/*
	 * 是否转入到我的账上的交易，如果是，则通知
	 */
	private void checkIsMine(Transaction tx) {
		Output output = tx.getOutputs().get(0);
		TransactionOutput tOutput = (TransactionOutput) output;
		
		Script script = tOutput.getScript();
		if(script.isSentToAddress() && blockStoreProvider.getAccountFilter().contains(script.getChunks().get(2).data)) {
			blockStoreProvider.updateMineTx(new TransactionStore(network, tx));
		}
	}
}
