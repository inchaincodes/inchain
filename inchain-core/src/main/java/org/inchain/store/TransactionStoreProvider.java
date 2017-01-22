package org.inchain.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.TimeHelper;
import org.inchain.crypto.Sha256Hash;
import org.inchain.listener.TransactionListener;
import org.inchain.script.Script;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionOutput;
import org.iq80.leveldb.DBIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 交易存储提供服务，存储跟自己有关的所有交易
 * @author ln
 *
 */
@Repository
public class TransactionStoreProvider extends ChainstateStoreProvider {
	
	//存放交易记录账号的key
	private final static byte[] ADDRESSES_KEY = Sha256Hash.ZERO_HASH.getBytes();
	//交易记录对应的账号列表
	private List<byte[]> addresses = new CopyOnWriteArrayList<byte[]>();
	//我的交易列表
	private List<TransactionStore> mineTxList = new ArrayList<TransactionStore>();
	
	//新交易监听器
	private TransactionListener transactionListener;
	
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
			
			TransactionStore tx = new TransactionStore(network, value);
			mineTxList.add(tx);
		}
	
		//绑定新交易监听器
		blockStoreProvider.addTransactionListener(new TransactionListener() {
			@Override
			public void newTransaction(TransactionStore tx) {
				boolean hasUpdate = false;
				//交易是否已经存在
				for (TransactionStore ts : mineTxList) {
					//如果存在，则更新状态
					if(ts.getTransaction().getHash().equals(tx.getTransaction().getHash())) {
						tx.setHeight(ts.getHeight());
						tx.setStatus((byte)1);//已确定
						hasUpdate = true;
						break;
					}
				}
				if(!hasUpdate) {
					//如果不存在，则新增
					mineTxList.add(tx);
				}
				newConfirmTransaction(tx);
			}
		});
	}
	
	/*
	 * 新的确认交易，更新交易记录
	 * @param tx
	 */
	private void newConfirmTransaction(TransactionStore ts) {
		put(ts.getTransaction().getHash().getBytes(), ts.baseSerialize());
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
		byte status = transactionStore.getStatus();
		byte[] content = transaction.baseSerialize();
		byte[] storeContent = new byte[content.length + 1];
		storeContent[0] = status;
		System.arraycopy(content, 0, storeContent, 1, content.length);
		return storeContent;
	}

	@Override
	protected Store pase(byte[] content) {
		if(content == null) {
			throw new NullPointerException("transaction content is null");
		}
		byte status = content[0];
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
	 * @return
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
		
		for (TransactionStore ts : mineTxList) {
			put(ts.getTransaction().getHash().getBytes(), ts.baseSerialize());
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
		
		for (TransactionStore transactionStore : mineTxList) {
			//获取转入交易转入的多少钱
			Transaction tx = transactionStore.getTransaction();
			
			byte[] key = tx.getHash().getBytes();
			
			List<Output> outputs = tx.getOutputs();
			
			for (int i = 0; i < outputs.size(); i++) {
				Output output = outputs.get(i);
				Script script = output.getScript();
				if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {

					byte[] statueKey = new byte[key.length + 1];
					System.arraycopy(key, 0, statueKey, 0, key.length);
					statueKey[statueKey.length - 1] = (byte) i;
					
					//交易是否已花费
					byte[] content = chainstateStoreProvider.getBytes(statueKey);
					if(content == null) {
						continue;
					}
					
					//本笔输出是否可用
					long lockTime = output.getLockTime();
					if(lockTime == -1l || (lockTime < TransactionDefinition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight) ||
							(lockTime > TransactionDefinition.LOCKTIME_THRESHOLD && lockTime > TimeHelper.currentTimeMillis())) {
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
	 * @return
	 */
	public List<TransactionStore> getTransactions() {
		return mineTxList;
	}
	
	/**
	 * 获取制定地址所有未花费的交易输出
	 * @return
	 */
	public List<TransactionOutput> getNotSpentTransactionOutputs(byte[] hash160) {
		
		List<TransactionOutput> txs = new ArrayList<TransactionOutput>();
		
		//查询当前区块最新高度
		long bestBlockHeight = network.getBestHeight();
		long localBestBlockHeight = network.getBestBlockHeight();
		
		if(bestBlockHeight < localBestBlockHeight) {
			bestBlockHeight = localBestBlockHeight;
		}
		
		for (TransactionStore transactionStore : mineTxList) {
			
			Transaction tx = transactionStore.getTransaction();
			
			byte[] key = tx.getHash().getBytes();
			
			//如果不是转账交易，则跳过
			if(!(tx.getType() == TransactionDefinition.TYPE_PAY || tx.getType() == TransactionDefinition.TYPE_COINBASE)) {
				continue;
			}
			
			//如果交易不可用，则跳过
			if(tx.getLockTime() == -1l || (tx.getLockTime() < TransactionDefinition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight) ||
					(tx.getLockTime() > TransactionDefinition.LOCKTIME_THRESHOLD && tx.getLockTime() > TimeHelper.currentTimeMillis())) {
				continue;
			}
			
			List<Output> outputs = tx.getOutputs();
			
			//遍历交易输出
			for (int i = 0; i < outputs.size(); i++) {
				Output output = outputs.get(i);
				Script script = output.getScript();
				if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {

					byte[] statueKey = new byte[key.length + 1];
					System.arraycopy(key, 0, statueKey, 0, key.length);
					statueKey[statueKey.length - 1] = (byte) i;
					
					//交易是否已花费
					byte[] content = chainstateStoreProvider.getBytes(statueKey);
					if(content == null) {
						continue;
					}
					
					//本笔输出是否可用
					long lockTime = output.getLockTime();
					if(lockTime == -1l || (lockTime < TransactionDefinition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight) ||
							(lockTime > TransactionDefinition.LOCKTIME_THRESHOLD && lockTime > TimeHelper.currentTimeMillis())) {
						continue;
					} else {
						txs.add((TransactionOutput) output);
					}
				}
			}
		}
		return txs;
	}

	public List<byte[]> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<byte[]> addresses) {
		this.addresses = addresses;
	}
	
	public List<TransactionStore> getMineTxList() {
		return mineTxList;
	}
	
	public void setTransactionListener(TransactionListener transactionListener) {
		this.transactionListener = transactionListener;
	}
}
