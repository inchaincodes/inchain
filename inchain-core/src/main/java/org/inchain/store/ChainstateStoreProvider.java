package org.inchain.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.business.RelevanceSubAccountTransaction;
import org.inchain.transaction.business.RemoveSubAccountTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 链状态查询提供服务，存放的是所有的未花费交易，以及共识节点
 * @author ln
 *
 */
@Repository
public class ChainstateStoreProvider extends BaseStoreProvider {
	
	@Autowired
	private BlockStoreProvider blockStoreProvider;

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
	
	/**
	 * 认证商家添加子账户
	 * @param relevancSubAccountTx
	 * @return boolean
	 */
	public boolean addSubAccount(RelevanceSubAccountTransaction relevancSubAccountTx) {
		byte[] subAccountKey = new byte[22];
		subAccountKey[0] = 0;
		subAccountKey[1] = 1;
		System.arraycopy(relevancSubAccountTx.getHash160(), 0, subAccountKey, 2, Address.LENGTH);
		
		byte[] subAccounts = getBytes(subAccountKey);
		if(subAccounts == null) {
			subAccounts = new byte[0];
		}
		byte[] newSubAccounts = new byte[subAccounts.length + Address.LENGTH + Sha256Hash.LENGTH];
		System.arraycopy(subAccounts, 0, newSubAccounts, 0, subAccounts.length);
		System.arraycopy(relevancSubAccountTx.getRelevanceHash160(), 0, newSubAccounts, subAccounts.length, Address.LENGTH);
		System.arraycopy(relevancSubAccountTx.getHash().getBytes(), 0, newSubAccounts, subAccounts.length + Address.LENGTH, Sha256Hash.LENGTH);
		put(subAccountKey, newSubAccounts);
		return true;
	}
	
	/**
	 * 认证商家删除子账户
	 * @param removeSubAccountTx
	 * @return boolean
	 */
	public boolean removeSubAccount(RemoveSubAccountTransaction removeSubAccountTx) {
		byte[] subAccountKey = new byte[22];
		subAccountKey[0] = 0;
		subAccountKey[1] = 1;
		System.arraycopy(removeSubAccountTx.getHash160(), 0, subAccountKey, 2, Address.LENGTH);
		
		byte[] subAccounts = getBytes(subAccountKey);
		
		byte[] newSubAccounts = new byte[subAccounts.length - (Address.LENGTH + Sha256Hash.LENGTH)];
		
		//找出位置在哪里
		//判断在列表里面才更新，否则就被清空了
		boolean hashExist = false;
		for (int j = 0; j < subAccounts.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
			byte[] addressHash160 = Arrays.copyOfRange(subAccounts, j, j + Address.LENGTH);
			byte[] txhash = Arrays.copyOfRange(subAccounts, j + Address.LENGTH, j + Address.LENGTH + Sha256Hash.LENGTH);
			if(Arrays.equals(addressHash160, removeSubAccountTx.getRelevanceHash160()) && Arrays.equals(txhash, removeSubAccountTx.getTxhash().getBytes())) {
				hashExist = true;
				System.arraycopy(subAccounts, 0, newSubAccounts, 0, j);
				System.arraycopy(subAccounts, j + (Address.LENGTH + Sha256Hash.LENGTH), newSubAccounts, j, subAccounts.length - j - (Address.LENGTH + Sha256Hash.LENGTH));
				break;
			}
		}
		if(hashExist) {
			put(subAccountKey, newSubAccounts);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 获取认证账户的子账户列表
	 * @param certHash160
	 * @return List<RelevanceSubAccountTransaction>
	 */
	public List<RelevanceSubAccountTransaction> getSubAccountList(byte[] certHash160) {
		List<RelevanceSubAccountTransaction> list = new ArrayList<RelevanceSubAccountTransaction>();
		
		byte[] subAccountKey = new byte[22];
		subAccountKey[0] = 0;
		subAccountKey[1] = 1;
		System.arraycopy(certHash160, 0, subAccountKey, 2, Address.LENGTH);
		
		byte[] subAccounts = getBytes(subAccountKey);
		if(subAccounts == null || subAccounts.length == 0) {
			return list;
		}
		
		for (int j = 0; j < subAccounts.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
			Sha256Hash txHash = Sha256Hash.wrap(Arrays.copyOfRange(subAccounts, j + Address.LENGTH, j + Address.LENGTH + Sha256Hash.LENGTH));
			
			TransactionStore txs = blockStoreProvider.getTransaction(txHash.getBytes());
			if(txs == null) {
				continue;
			}
			RelevanceSubAccountTransaction rst = (RelevanceSubAccountTransaction) txs.getTransaction();
			list.add(rst);
		}
		return list;
	}
	
	/**
	 * 获取认证账户的子账户数量
	 * @param certHash160
	 * @return int
	 */
	public int getSubAccountCount(byte[] certHash160) {
		byte[] subAccountKey = new byte[22];
		subAccountKey[0] = 0;
		subAccountKey[1] = 1;
		System.arraycopy(certHash160, 0, subAccountKey, 2, Address.LENGTH);
		
		byte[] subAccounts = getBytes(subAccountKey);
		if(subAccounts == null || subAccounts.length == 0) {
			return 0;
		}
		return (int)(subAccounts.length / (Address.LENGTH + Sha256Hash.LENGTH));
	}
	
	/**
	 * 检查是否是认证账户的子账户
	 * @param certHash160
	 * @param hash160
	 * @return Sha256Hash 返回商家添加子账户的交易id，有可能返回null
	 */
	public Sha256Hash checkIsSubAccount(byte[] certHash160, byte[] hash160) {
		byte[] subAccountKey = new byte[22];
		subAccountKey[0] = 0;
		subAccountKey[1] = 1;
		System.arraycopy(certHash160, 0, subAccountKey, 2, Address.LENGTH);
		
		byte[] subAccounts = getBytes(subAccountKey);
		if(subAccounts == null || subAccounts.length == 0) {
			return null;
		}
		for (int j = 0; j < subAccounts.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
			byte[] addressHash160 = Arrays.copyOfRange(subAccounts, j, j + Address.LENGTH);
			if(Arrays.equals(addressHash160, hash160)) {
				return Sha256Hash.wrap(Arrays.copyOfRange(subAccounts, j + Address.LENGTH, j + Address.LENGTH + Sha256Hash.LENGTH));
			}
		}
		return null;
	}
	
	/**
	 * 通过别名查询账户信息
	 * @param alias
	 * @return AccountStore
	 */
	public AccountStore getAccountInfoByAlias(byte[] alias) {
		byte[] hash160 = getAccountHash160ByAlias(alias);
		if(hash160 == null) {
			return null;
		}
		return getAccountInfo(hash160);
	}
	
	/**
	 * 通过别名查询账户hash160
	 * @param alias
	 * @return byte[]
	 */
	public byte[] getAccountHash160ByAlias(byte[] alias) {
		if(alias == null) {
			return null;
		}
		return getBytes(Sha256Hash.hash(alias));
	}
	
	/**
	 * 设置账户别名
	 * 消耗相应的信用点
	 * @param hash160
	 * @param alias
	 * @return boolean
	 */
	public boolean setAccountAlias(byte[] hash160, byte[] alias) {
		AccountStore accountInfo = getAccountInfo(hash160);
		if(accountInfo == null) {
			return false;
		}
		//设置别名
		accountInfo.setAlias(alias);
		saveAccountInfo(accountInfo);
		
		put(Sha256Hash.hash(alias), hash160);
		
		return true;
	}
	
	/**
	 * 修改账户别名
	 * 消耗相应的信用点
	 * @param hash160
	 * @param alias
	 * @return boolean
	 */
	public boolean updateAccountAlias(byte[] hash160, byte[] alias) {
		AccountStore accountInfo = getAccountInfo(hash160);
		if(accountInfo == null) {
			return false;
		}
		//删除旧别名
		byte[] oldAlias = accountInfo.getAlias();
		if(oldAlias != null && oldAlias.length > 0) {
			delete(Sha256Hash.hash(accountInfo.getAlias()));
		}
		//设置新的别名
		accountInfo.setAlias(alias);
		accountInfo.setCert(accountInfo.getCert() + Configure.UPDATE_ALIAS_SUB_CREDIT);
		saveAccountInfo(accountInfo);
		
		put(Sha256Hash.hash(alias), hash160);
		
		return true;
	}
}
