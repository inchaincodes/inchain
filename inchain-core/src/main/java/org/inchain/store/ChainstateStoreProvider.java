package org.inchain.store;

import org.inchain.Configure;
import org.inchain.transaction.Transaction;
import org.springframework.stereotype.Repository;

/**
 * 链状态查询提供服务，存放的是所有的未花费交易，以及共识节点
 * @author ln
 *
 */
@Repository
public class ChainstateStoreProvider extends BaseStoreProvider {

	protected ChainstateStoreProvider() {
		this(Configure.DATA_CHAINSTATE);
	}
	
	protected ChainstateStoreProvider(String dir) {
		this(dir, -1, -1);
	}
	protected ChainstateStoreProvider(String dir, long leveldbReadCache,
			int leveldbWriteCache) {
		super(dir, leveldbReadCache, leveldbWriteCache);
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
		return transaction.baseSerialize();
	}

	@Override
	protected Store pase(byte[] content) {
		if(content == null) {
			throw new NullPointerException("transaction content is null");
		}
		Transaction transaction = new Transaction(network, content);
		TransactionStore store = new TransactionStore(network, transaction);
		return store;
	}
	
	/**
	 * 获取账户的公钥
	 * 如果是认证账户，则获取的是最新的公钥
	 * @param hash160 账户的hash160
	 * @return byte[][] 普通账户返回1个，认证账户前两个为管理公钥、后两个为交易公钥，当没有查到时返回null
	 */
	public byte[][] getAccountPubkeys(byte[] hash160) {
		AccountStore store = getAccountInfo(hash160);
		if(store == null) {
			return null;
		} else {
			return store.getPubkeys();
		}
	}

	/**
	 * 获取账户信息
	 * @param hash160
	 * @return AccountStore
	 */
	public AccountStore getAccountInfo(byte[] hash160) {
		byte[] accountBytes = getBytes(hash160);
		if(accountBytes == null) {
			return null;
		}
		AccountStore store = new AccountStore(network, accountBytes);
		return store;
	}
	
	/**
	 * 保存账户信息
	 * @param accountInfo
	 * @return boolean
	 */
	public boolean saveAccountInfo(AccountStore accountInfo) {
		try {
			put(accountInfo.getHash160(), accountInfo.baseSerialize());
			return true;
		} catch (Exception e) {
			log.error("保存账户信息出错：", e);
			return false;
		}
	}
}
