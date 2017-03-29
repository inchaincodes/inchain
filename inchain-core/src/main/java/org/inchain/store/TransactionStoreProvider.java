package org.inchain.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.crypto.Sha256Hash;
import org.inchain.listener.NoticeListener;
import org.inchain.listener.TransactionListener;
import org.inchain.mempool.MempoolContainer;
import org.inchain.script.Script;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.BaseCommonlyTransaction;
import org.inchain.utils.StringUtil;
import org.iq80.leveldb.DBIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 交易存储提供服务，存储跟自己有关的所有交易
 * @author ln
 *
 */
@Repository
public class TransactionStoreProvider extends BaseStoreProvider {
	
	//存放交易记录账号的key
	private final static byte[] ADDRESSES_KEY = Sha256Hash.ZERO_HASH.getBytes();
	//交易记录对应的账号列表
	private List<byte[]> addresses = new CopyOnWriteArrayList<byte[]>();
	//我的交易列表
	private List<TransactionStore> mineTxList = new CopyOnWriteArrayList<TransactionStore>();
	//未花费的交易
	private List<TransactionStore> unspendTxList = new CopyOnWriteArrayList<TransactionStore>();
	
	//新交易监听器
	private TransactionListener transactionListener;
	//通知监听器
	private NoticeListener noticeListener;
	
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	
	public TransactionStoreProvider() {
		this(Configure.DATA_TRANSACTION);
	}
	
	public TransactionStoreProvider(String dir) {
		super(dir);
	}
	public TransactionStoreProvider(String dir, long leveldbReadCache, int leveldbWriteCache) {
		super(dir, leveldbReadCache, leveldbWriteCache);
	}

	/**
	 * 初始化
	 */
	@PostConstruct
	public void init() {
		SpringContextUtils.setNetwork(network);
		//本地交易记录对应的账号列表
		byte[] list = getBytes(ADDRESSES_KEY);
		if(list != null) {
			for (int i = 0; i < list.length; i+= Address.LENGTH) {
				byte[] hash160 = new byte[Address.LENGTH];
				System.arraycopy(list, i, hash160, 0, Address.LENGTH);
				addresses.add(hash160);
			}
		}
		
		//交易记录
		DBIterator iterator = db.getSourceDb().iterator();
		while(iterator.hasNext()) {
			Entry<byte[], byte[]> item = iterator.next();
			byte[] key = item.getKey();
			if(Arrays.equals(ADDRESSES_KEY, key)) {
				continue;
			}
			byte[] value = item.getValue();
			
			TransactionStore txs = new TransactionStore(network, value);
			mineTxList.add(txs);
			
			//是否未花费的交易
			Transaction tx = txs.getTransaction();
			if(tx.isPaymentTransaction()) {
				byte[] status = txs.getStatus();
				List<TransactionOutput> outputs = tx.getOutputs();
				for (int i = 0; i < outputs.size(); i++) {
					TransactionOutput output = outputs.get(i);
					Script script = output.getScript();
					if(status == null || status.length < i || status[i] != TransactionStore.STATUS_UNUSE || unspendTxList.contains(txs)) {
						continue;
					}
					for (byte[] hash160 : addresses) {
						if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
							unspendTxList.add(txs);
						}
					}
				}
			}
		}
		
		//绑定新交易监听器
		blockStoreProvider.addTransactionListener(new TransactionListener() {
			@Override
			public void newTransaction(TransactionStore txs) {
				processNewTransaction(txs);
			}
			@Override
			public void revokedTransaction(TransactionStore txs) {
				processRevokedTransaction(txs);
			}
		});
	}
	
	/**
	 * 回滚交易
	 * @param txs
	 */
	public void processRevokedTransaction(TransactionStore txs) {
		
		Transaction tx = txs.getTransaction();
		
		if(tx.isPaymentTransaction()) {
			//更新交易状态
			List<TransactionInput> inputs = tx.getInputs();
			if(inputs != null) {
				for (TransactionInput input : inputs) {
					if(input.getFroms() == null || input.getFroms().size() == 0) {
						continue;
					}
					for (TransactionOutput from : input.getFroms()) {
						Sha256Hash fromTxHash = from.getParent().getHash();
						
						for (TransactionStore transactionStore : mineTxList) {
							if(transactionStore.getTransaction().getHash().equals(fromTxHash)) {
								//更新内存
								byte[] ftxStatus = transactionStore.getStatus();
								ftxStatus[from.getIndex()] = TransactionStore.STATUS_UNUSE;
								transactionStore.setStatus(ftxStatus);
								//更新存储
								put(transactionStore.getTransaction().getHash().getBytes(), transactionStore.baseSerialize());
								break;
							}
						}
					}
				}
			}
		}
		
		delete(tx.getHash().getBytes());
		
		//从内存中删除
		for (TransactionStore t : mineTxList) {
			if(t.getTransaction().getHash().equals(tx.getHash())) {
				mineTxList.remove(t);
				break;
			}
		}
	}

	/**
	 * 处理新交易
	 * @param txs
	 */
	public void processNewTransaction(TransactionStore txs) {
		boolean hasUpdate = false;
		//交易是否已经存在
		for (TransactionStore transactionStore : mineTxList) {
			//如果存在，则更新高度
			if(transactionStore.getTransaction().getHash().equals(txs.getTransaction().getHash())) {
				transactionStore.setHeight(txs.getHeight());
				txs = transactionStore;
				hasUpdate = true;
				break;
			}
		}
		Transaction tx = txs.getTransaction();
		
		if(!hasUpdate) {
			//如果不存在，则新增
			mineTxList.add(txs);
			
			if(tx.isPaymentTransaction()) {
				//更新交易状态
				List<TransactionOutput> outputs = tx.getOutputs();
				
				List<TransactionInput> inputs = tx.getInputs();
				if(inputs != null) {
					for (TransactionInput input : inputs) {
						if(input.getFroms() == null || input.getFroms().size() == 0) {
							continue;
						}
						for (TransactionOutput from : input.getFroms()) {
							Sha256Hash fromTxHash = from.getParent().getHash();
							
							for (TransactionStore transactionStore : mineTxList) {
								if(transactionStore.getTransaction().getHash().equals(fromTxHash)) {
									//更新内存
									byte[] ftxStatus = transactionStore.getStatus();
									ftxStatus[from.getIndex()] = TransactionStore.STATUS_USED;
									transactionStore.setStatus(ftxStatus);
									//更新存储
									put(transactionStore.getTransaction().getHash().getBytes(), transactionStore.baseSerialize());
									break;
								}
							}
							
							for (TransactionStore unspendTx : unspendTxList) {
								if(unspendTx.getTransaction().getHash().equals(fromTxHash)) {
									//更新内存
									byte[] ftxStatus = unspendTx.getStatus();
									ftxStatus[from.getIndex()] = TransactionStore.STATUS_USED;
									unspendTx.setStatus(ftxStatus);
									
									//查询该笔交易是否还有我没有花费的交易
									List<TransactionOutput> outputsTemp = unspendTx.getTransaction().getOutputs();
									boolean hasUnspend = false;
									for (TransactionOutput transactionOutput : outputsTemp) {
										Script script = transactionOutput.getScript();
										for (byte[] hash160 : addresses) {
											if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)
													&& ftxStatus[transactionOutput.getIndex()] == TransactionStore.STATUS_UNUSE) {
												hasUnspend = true;
												break;
											}
										}
									}
									if(!hasUnspend) {
										unspendTxList.remove(unspendTx);
									}
									break;
								}
							}
						}
					}
				}
				//交易状态
				byte[] status = new byte[outputs.size()];
				
				for (int i = 0; i < outputs.size(); i++) {
					TransactionOutput output = outputs.get(i);
					Script script = output.getScript();
					status[i] = TransactionStore.STATUS_UNUSE;
					
					for (byte[] hash160 : addresses) {
						if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
							if(!unspendTxList.contains(txs)) {
								unspendTxList.add(txs);
							}
							break;
						}
					}
				}
				//设置交易存储状态
				txs.setStatus(status);
			}
			
			if(noticeListener != null) {
				// 我的交易，则提醒
				if(tx.isPaymentTransaction()) {
					//转入给我，则提醒
					if(tx.getOutputs() != null && tx.getOutputs().size() > 0) {
						TransactionOutput output = (TransactionOutput) tx.getOutputs().get(0);
						
						Script script = output.getScript();
						if(script.isSentToAddress() && blockStoreProvider.getAccountFilter().contains(script.getChunks().get(2).data)) {
							noticeReceiveAmount(tx, output);
						}
					}
				} else {
					BaseCommonlyTransaction commonlyTx = (BaseCommonlyTransaction) tx;
					if(blockStoreProvider.getAccountFilter().contains(commonlyTx.getHash160())) {
						noticeListener.onNotice("交易已提交", String.format("您的交易 %s 已被网络接受", commonlyTx.getHash()));
					}
				}
			}
		} else {
			//交易状态变化
			if(noticeListener != null && txs.getHeight() != -1l) {
				if(tx.isPaymentTransaction()) {
					noticeListener.onNotice("交易确认", String.format("交易 %s 已被收录至高度为 %d 的块", tx.getHash().toString(), txs.getHeight()));
				} else {
					BaseCommonlyTransaction commonlyTx = (BaseCommonlyTransaction) tx;
					if(blockStoreProvider.getAccountFilter().contains(commonlyTx.getHash160())) {
						noticeListener.onNotice("交易已确认", String.format("您的交易 %s 已成功收录进高度为 %d 的块", commonlyTx.getHash(), txs.getHeight()));
					}
				}
			}
		}
		
		put(txs.getTransaction().getHash().getBytes(), txs.baseSerialize());
		
		newConfirmTransaction(txs);
	}

	//提醒接收到付款
	private void noticeReceiveAmount(Transaction tx, TransactionOutput output) {
		if(tx.getType() == Definition.TYPE_COINBASE) {
			noticeListener.onNotice("参与共识产生新的块", String.format("%s参与共识，获得收入 %s INS", new Address(network, tx.getOutput(0).getScript().getChunks().get(2).data).getBase58(), Coin.valueOf(tx.getOutput(0).getValue()).toText()));
		} else if(tx.getType() == Definition.TYPE_PAY) {
			//对上一交易的引用以及索引值
			Sha256Hash fromId = output.getParent().getHash();
			int index = output.getIndex();
			
			byte[] key = new byte[fromId.getBytes().length + 1];
			
			System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
			key[key.length - 1] = (byte) index;
			
			//查询上次的交易
			Transaction preTransaction = null;
			
			//判断是否未花费
			byte[] state = chainstateStoreProvider.getBytes(key);
			if(!Arrays.equals(state, new byte[]{1})) {
				//查询内存池里是否有该交易
				preTransaction = MempoolContainer.getInstace().get(fromId);
			} else {
				//查询上次的交易
				preTransaction = blockStoreProvider.getTransaction(fromId.getBytes()).getTransaction();
			}
			TransactionOutput preOutput = (TransactionOutput) preTransaction.getOutput(index);
			
			noticeListener.onNotice("接收到新的转账交易", String.format("接收到一笔来自 %s 的转账，金额  %s INS", new Address(network, preOutput.getScript().getChunks().get(2).data).getBase58(), Coin.valueOf(preOutput.getValue()).toText()));
		}
	}
	
	/*
	 * 新的确认交易，更新交易记录
	 * @param tx
	 */
	private void newConfirmTransaction(TransactionStore ts) {
		if(transactionListener != null) {
			transactionListener.newTransaction(ts);
		}
	}

	@Override
	protected byte[] toByte(Store store) {
		if(store == null) {
			throw new NullPointerException("transaction is null");
		}
		TransactionStore transactionStore = (TransactionStore) store;
		
		Transaction transaction = transactionStore.getTransaction();
		if(transaction == null) {
			throw new NullPointerException("transaction is null");
		}
		//交易状态，放最前面
		byte[] status = transactionStore.getStatus();
		byte[] content = transaction.baseSerialize();
		
		int statusLength = 0;
		if(status != null) {
			statusLength = status.length;
		}
		byte[] storeContent = new byte[content.length + statusLength + 1];
		storeContent[0] = (byte) statusLength;
		if(status != null) {
			System.arraycopy(status, 0, storeContent, 1, status.length);
		}
		System.arraycopy(content, 0, storeContent, 1 + statusLength, content.length);
		return storeContent;
	}

	@Override
	protected Store pase(byte[] content) {
		if(content == null) {
			throw new NullPointerException("transaction content is null");
		}
		int statusLength = content[0];
		byte[] status = new byte[statusLength];
		if(statusLength > 0) {
			System.arraycopy(content, 1, status, 0, statusLength);
		}
		byte[] transactionContent = new byte[content.length - 1];
		System.arraycopy(content, 1, transactionContent, 0, content.length - 1);
		
		Transaction transaction = new Transaction(network, transactionContent);
		TransactionStore store = new TransactionStore(network, transaction);
		store.setStatus(status);
		return store;
	}
	
	/**
	 * 重新初始化交易记录列表
	 * @param hash160s
	 * @return boolean
	 */
	public boolean reloadTransaction(List<byte[]> hash160s) {
		
		//清除老数据
		DBIterator iterator = db.getSourceDb().iterator();
		while(iterator.hasNext()) {
			Entry<byte[], byte[]> item = iterator.next();
			byte[] key = item.getKey();
			
			delete(key);
		}
		
		//写入新列表
		byte[] addresses = new byte[hash160s.size() * Address.LENGTH];
		for (int i = 0; i < hash160s.size(); i++) {
			System.arraycopy(hash160s.get(i), 0, addresses, i * Address.LENGTH, Address.LENGTH);
		}
		put(ADDRESSES_KEY, addresses);
		
		this.addresses = hash160s;
		
		//遍历区块写入相关交易
		mineTxList = blockStoreProvider.loadRelatedTransactions(hash160s);
		unspendTxList = new CopyOnWriteArrayList<TransactionStore>();
		
		for (TransactionStore txs : mineTxList) {
			put(txs.getTransaction().getHash().getBytes(), txs.baseSerialize());
			
			//是否未花费的交易
			Transaction tx = txs.getTransaction();
			if(tx.isPaymentTransaction()) {
				byte[] status = txs.getStatus();
				List<TransactionOutput> outputs = tx.getOutputs();
				for (int i = 0; i < outputs.size(); i++) {
					TransactionOutput output = outputs.get(i);
					Script script = output.getScript();
					if(status == null || status.length < i || status[i] != TransactionStore.STATUS_UNUSE || unspendTxList.contains(txs)) {
						continue;
					}
					for (byte[] hash160 : this.addresses) {
						if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
							unspendTxList.add(txs);
						}
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 获取地址的最新余额和未确认的余额
	 * @param hash160
	 * @return Coin[]
	 */
	public Coin[] getBalanceAndUnconfirmedBalance(byte[] hash160) {
		Coin balance = Coin.ZERO;
		Coin unconfirmedBalance = Coin.ZERO;
		
		//查询当前区块最新高度
		long bestBlockHeight = network.getBestHeight();
		long localBestBlockHeight = network.getBestBlockHeight();
		
		if(bestBlockHeight < localBestBlockHeight) {
			bestBlockHeight = localBestBlockHeight;
		}
		
		for (TransactionStore transactionStore : unspendTxList) {
			//获取转入交易转入的多少钱
			Transaction tx = transactionStore.getTransaction();
			if(!tx.isPaymentTransaction()) {
				continue;
			}
			
			//如果交易不可用，则标记
			boolean txAvailable = true;
			if(tx.getLockTime() == -1l || (tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight) ||
					(tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > TimeService.currentTimeMillis())) {
				txAvailable = false;
			}
			
			byte[] key = tx.getHash().getBytes();
			byte[] status = transactionStore.getStatus();
			
			List<TransactionOutput> outputs = tx.getOutputs();
			
			for (int i = 0; i < outputs.size(); i++) {
				TransactionOutput output = outputs.get(i);
				Script script = output.getScript();
				if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {

					byte[] statueKey = new byte[key.length + 1];
					System.arraycopy(key, 0, statueKey, 0, key.length);
					statueKey[statueKey.length - 1] = (byte) i;
					
					//交易是否已花费
//					byte[] content = chainstateStoreProvider.getBytes(statueKey);
//					if(content == null) {
//						continue;
//					}
					//交易是否已花费
					if(status != null && status.length > 0 && status[i] == TransactionStore.STATUS_USED) {
						continue;
					}
					//本笔输出是否可用
					long lockTime = output.getLockTime();
					if(!txAvailable || lockTime == -1l || (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight) ||
							(lockTime >= Definition.LOCKTIME_THRESHOLD && lockTime > TimeService.currentTimeMillis())
							|| (i == 0 && transactionStore.getHeight() == -1l)) {
						unconfirmedBalance = unconfirmedBalance.add(Coin.valueOf(output.getValue()));
					} else {
						balance = balance.add(Coin.valueOf(output.getValue()));
					}
				}
			}
		}
		
		return new Coin[]{balance, unconfirmedBalance};
	}
	
	/**
	 * 获取所有交易记录
	 * @return List<TransactionStore>
	 */
	public List<TransactionStore> getTransactions() {
		return mineTxList;
	}
	
	/**
	 * 获取制定地址所有未花费的交易输出
	 * @return List<TransactionOutput>
	 */
	public List<TransactionOutput> getNotSpentTransactionOutputs(byte[] hash160) {
		
		List<TransactionOutput> txs = new ArrayList<TransactionOutput>();
		
		//查询当前区块最新高度
		long bestBlockHeight = network.getBestHeight();
		long localBestBlockHeight = network.getBestBlockHeight();
		
		if(bestBlockHeight < localBestBlockHeight) {
			bestBlockHeight = localBestBlockHeight;
		}
		
		for (TransactionStore transactionStore : unspendTxList) {
			
			//交易状态
			byte[] status = transactionStore.getStatus();
			
			Transaction tx = transactionStore.getTransaction();
			
			//如果不是转账交易，则跳过
			if(!tx.isPaymentTransaction()) {
				continue;
			}
			
			//如果交易不可用，则跳过
			if(tx.getLockTime() == -1l || (tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight) ||
					(tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > TimeService.currentTimeMillis())) {
				continue;
			}
			
			List<TransactionOutput> outputs = tx.getOutputs();
			
			//遍历交易输出
			for (int i = 0; i < outputs.size(); i++) {
				TransactionOutput output = outputs.get(i);
				Script script = output.getScript();
				if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {

					//交易是否已花费
					if(status[i] == TransactionStore.STATUS_USED) {
						continue;
					}
//					
//					//链上状态是否可用
//					byte[] statueKey = new byte[key.length + 1];
//					System.arraycopy(key, 0, statueKey, 0, key.length);
//					statueKey[statueKey.length - 1] = (byte) i;
//					
//					byte[] content = chainstateStoreProvider.getBytes(statueKey);
//					if(content == null) {
//						continue;
//					}
					
					//本笔输出是否可用
					long lockTime = output.getLockTime();
					if(lockTime == -1l || (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight) ||
							(lockTime > Definition.LOCKTIME_THRESHOLD && lockTime > TimeService.currentTimeMillis())) {
						continue;
					} else {
						txs.add((TransactionOutput) output);
					}
				}
			}
		}
		return txs;
	}

	/**
	 * 获取认证账户信息对应的最新的交易记录
	 * @param hash160
	 * @return Transaction
	 */
	public Transaction getAccountInfosNewestTransaction(byte[] hash160) {
		AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
		if(accountInfo == null) {
			return null;
		} else {
			return blockStoreProvider.getTransaction(accountInfo.getInfoTxid().getBytes()).getTransaction();
		}
	}

	public List<byte[]> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<byte[]> addresses) {
		this.addresses = addresses;
	}
	
	public List<TransactionStore> getMineTxList(String address) {
		
		if(StringUtil.isEmpty(address)) {
			return mineTxList;
		}
		
		List<TransactionStore> resultList = new ArrayList<TransactionStore>();
		
		byte[] hash160 = null;
		try {
			hash160 = Address.fromBase58(network, address).getHash160();
		} catch (Exception e) {
			return resultList;
		}
		for (TransactionStore txs : mineTxList) {
			Transaction tx = txs.getTransaction();
			if(blockStoreProvider.checkTxIsMine(tx, hash160)) {
				resultList.add(txs);
			}
		}
		return resultList;
	}
	
	public void setTransactionListener(TransactionListener transactionListener) {
		this.transactionListener = transactionListener;
	}
	public void setNoticeListener(NoticeListener noticeListener) {
		this.noticeListener = noticeListener;
	}
}
