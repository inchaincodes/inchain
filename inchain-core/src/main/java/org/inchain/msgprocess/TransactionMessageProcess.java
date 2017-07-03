package org.inchain.msgprocess;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.core.Definition;
import org.inchain.core.Peer;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.filter.InventoryFilter;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.Mempool;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryItem.Type;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.message.RejectMessage;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.CertAccountTransaction;
import org.inchain.utils.Hex;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
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

	private Lock locker = new ReentrantLock();
	
	private Mempool mempool = MempoolContainer.getInstace();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionValidator transactionValidator;
	@Autowired
	private InventoryFilter filter;
	
	//没有找到的交易暂存
	private List<Transaction> notFoundTxList = new CopyOnWriteArrayList<Transaction>();
	
	public TransactionMessageProcess() {
	}
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		Transaction tx = (Transaction) message;

		if(log.isDebugEnabled()) {
			log.debug("transaction message {}", Hex.encode(tx.baseSerialize()));
		}
		try {
			locker.lock();
			
			Sha256Hash id = tx.getHash();

			if(filter.contains(id.getBytes())) {
				return new MessageProcessResult(tx.getHash(), false);
			} else {
				filter.insert(id.getBytes());
			}
			
			//验证交易的合法性
			tx.verify();
			
			//交易逻辑验证，验证不通过抛出VerificationException
			MessageProcessResult verifyRes = verifyTx(tx);
			if(verifyRes != null) {
				return verifyRes;
			}
			
			if(log.isDebugEnabled()) {
				log.debug("verify success! tx id : {}", id);
			}
			
			//加入内存池
			boolean res = mempool.add(tx);
//			if(!res) {
//				log.error("加入内存池失败："+ id);
//				//加入内存池失败，有两种情况，第一是重复交易已经存在，第二是双花交易，出现失败时不做处理即可
//				return new MessageProcessResult(tx.getHash(), true);
//			}
			
			//转发交易
			InventoryItem item = new InventoryItem(Type.Transaction, id);
			InventoryMessage invMessage = new InventoryMessage(peer.getNetwork(), item);
			peerKit.broadcastMessage(invMessage);
	
			//验证是否是转入到我账上的交易
			checkIsMine(tx);
			
			//检查是否有积压交易
//			checkFoundOld(tx);
			
			return new MessageProcessResult(tx.getHash(), true);
		} catch (Exception e) {
			log.error("tx error ", e);
			RejectMessage replyMessage = new RejectMessage(network, tx.getHash());
			//TODO
			return new MessageProcessResult(tx.getHash(), false, replyMessage);
		} finally {
			locker.unlock();
		}
	}

//	private void checkFoundOld(Transaction tx) {
//		if(notFoundTxList != null && notFoundTxList.size() > 0) {
//			for (Transaction transaction : notFoundTxList) {
//				List<TransactionInput> inputs = transaction.getInputs();
//				if(inputs == null || inputs.size() == 0) {
//					continue;
//				}
//				boolean hasFound = false;
//				for (TransactionInput transactionInput : inputs) {
//					List<TransactionOutput> froms = transactionInput.getFroms();
//					if(froms == null || froms.size() == 0) {
//						continue;
//					}
//					for (TransactionOutput transactionOutput : froms) {
//						if(transactionOutput.getParent().getHash().equals(tx.getHash())) {
//							//找到了
//							mempool.add(transaction);
//							notFoundTxList.remove(transaction);
//							hasFound = true;
//							
//							//递归查找
//							foundInOlds(transaction);
//							break;
//						}
//					}
//					if(hasFound) {
//						break;
//					}
//				}
//			}
//		}
//	}
	
//	private void foundInOlds(Transaction tx) {
//		if(notFoundTxList != null && notFoundTxList.size() > 0) {
//			for (Transaction transaction : notFoundTxList) {
//				List<TransactionInput> inputs = transaction.getInputs();
//				if(inputs == null || inputs.size() == 0) {
//					continue;
//				}
//				boolean hasFound = false;
//				for (TransactionInput transactionInput : inputs) {
//					List<TransactionOutput> froms = transactionInput.getFroms();
//					if(froms == null || froms.size() == 0) {
//						continue;
//					}
//					for (TransactionOutput transactionOutput : froms) {
//						if(transactionOutput.getParent().getHash().equals(tx.getHash())) {
//							//找到了
//							mempool.add(transaction);
//							notFoundTxList.remove(transaction);
//							hasFound = true;
//							//递归查找
//							foundInOlds(transaction);
//							break;
//						}
//					}
//					if(hasFound) {
//						break;
//					}
//				}
//			}
//		}
//	}

	//交易逻辑验证，验证不通过抛出VerificationException
	private MessageProcessResult verifyTx(Transaction tx) throws VerificationException {
		TransactionValidatorResult rs = transactionValidator.valDo(tx, null).getResult();
		
		if(!rs.isSuccess()) {
			//没有找到的交易
			if(rs.getErrorCode() == TransactionValidatorResult.ERROR_CODE_NOT_FOUND) {
				//没有找到，当验证成功处理，只是暂时不加入内存池
//				notFoundTxList.add(tx);
//				//转发交易
//				InventoryItem item = new InventoryItem(Type.Transaction, tx.getHash());
//				InventoryMessage invMessage = new InventoryMessage(network, item);
//				peerKit.broadcastMessage(invMessage);
				
				return new MessageProcessResult(tx.getHash(), true);
			} else if(rs.getErrorCode() == TransactionValidatorResult.ERROR_CODE_EXIST) {
				return new MessageProcessResult(tx.getHash(), true);
			} else {
				log.warn("新交易验证失败, error code: {} , {}", rs.getErrorCode(), rs.getMessage());
				throw new VerificationException(rs.getMessage());
			}
		}
		return null;
	}

	/*
	 * 是否转入到我的账上的交易，如果是，则通知
	 */
	private void checkIsMine(Transaction tx) {
		if(tx.getType() == Definition.TYPE_COINBASE || tx.getType() == Definition.TYPE_PAY) {
			Output output = tx.getOutputs().get(0);
			TransactionOutput tOutput = (TransactionOutput) output;
			
			Script script = tOutput.getScript();
			if(script.isSentToAddress() && blockStoreProvider.getAccountFilter().contains(script.getChunks().get(2).data)) {
				blockStoreProvider.updateMineTx(new TransactionStore(network, tx));
			}
		} else if(tx instanceof CertAccountTransaction) {
			CertAccountTransaction cat = (CertAccountTransaction) tx;
			byte[] hash160 = cat.getHash160();
			if(blockStoreProvider.getAccountFilter().contains(hash160)) {
				blockStoreProvider.updateMineTx(new TransactionStore(network, tx));
			}
		}
	}
	
//	/**
//	 * 获取待定交易
//	 * @param hash
//	 * @return Transaction
//	 */
//	public Transaction getPendingTx(Sha256Hash hash) {
//		for (Transaction transaction : notFoundTxList) {
//			if(transaction.getHash().equals(hash)) {
//				return transaction;
//			}
//		}
//		return null;
//	}
//
//	public int getPendingTxCount() {
//		return notFoundTxList.size();
//	}
}
