package org.inchain.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.business.AntifakeTransferTransaction;
import org.inchain.transaction.business.CirculationTransaction;
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
		byte[] newSubAccounts = new byte[subAccounts.length + Address.HASH_LENGTH + Sha256Hash.LENGTH];
		System.arraycopy(subAccounts, 0, newSubAccounts, 0, subAccounts.length);
		System.arraycopy(relevancSubAccountTx.getRelevanceHashs(), 0, newSubAccounts, subAccounts.length, Address.HASH_LENGTH);
		System.arraycopy(relevancSubAccountTx.getHash().getBytes(), 0, newSubAccounts, subAccounts.length + Address.HASH_LENGTH, Sha256Hash.LENGTH);
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
		
		byte[] newSubAccounts = new byte[subAccounts.length - (Address.HASH_LENGTH + Sha256Hash.LENGTH)];
		
		//找出位置在哪里
		//判断在列表里面才更新，否则就被清空了
		boolean hashExist = false;
		for (int j = 0; j < subAccounts.length; j += (Address.HASH_LENGTH + Sha256Hash.LENGTH)) {
			byte[] addressHashsTemp = Arrays.copyOfRange(subAccounts, j, j + Address.HASH_LENGTH);
			byte[] txhash = Arrays.copyOfRange(subAccounts, j + Address.HASH_LENGTH, j + Address.HASH_LENGTH + Sha256Hash.LENGTH);
			if(Arrays.equals(addressHashsTemp, removeSubAccountTx.getRelevanceHashs()) && Arrays.equals(txhash, removeSubAccountTx.getTxhash().getBytes())) {
				hashExist = true;
				System.arraycopy(subAccounts, 0, newSubAccounts, 0, j);
				System.arraycopy(subAccounts, j + (Address.HASH_LENGTH + Sha256Hash.LENGTH), newSubAccounts, j, subAccounts.length - j - (Address.HASH_LENGTH + Sha256Hash.LENGTH));
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
		
		for (int j = 0; j < subAccounts.length; j += (Address.HASH_LENGTH + Sha256Hash.LENGTH)) {
			Sha256Hash txHash = Sha256Hash.wrap(Arrays.copyOfRange(subAccounts, j + Address.HASH_LENGTH, j + Address.HASH_LENGTH + Sha256Hash.LENGTH));
			
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
		return (int)(subAccounts.length / (Address.HASH_LENGTH + Sha256Hash.LENGTH));
	}
	
	/**
	 * 检查是否是认证账户的子账户
	 * @param certHash160
	 * @param addressHashs
	 * @return Sha256Hash 返回商家添加子账户的交易id，有可能返回null
	 */
	public Sha256Hash checkIsSubAccount(byte[] certHash160, byte[] addressHashs) {
		byte[] subAccountKey = new byte[22];
		subAccountKey[0] = 0;
		subAccountKey[1] = 1;
		System.arraycopy(certHash160, 0, subAccountKey, 2, Address.LENGTH);
		
		byte[] subAccounts = getBytes(subAccountKey);
		if(subAccounts == null || subAccounts.length == 0) {
			return null;
		}
		for (int j = 0; j < subAccounts.length; j += (Address.HASH_LENGTH + Sha256Hash.LENGTH)) {
			byte[] addressHashsTemp = Arrays.copyOfRange(subAccounts, j, j + Address.HASH_LENGTH);
			if(Arrays.equals(addressHashsTemp, addressHashs)) {
				return Sha256Hash.wrap(Arrays.copyOfRange(subAccounts, j + Address.HASH_LENGTH, j + Address.HASH_LENGTH + Sha256Hash.LENGTH));
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
		//扣除信用
		accountInfo.setCert(accountInfo.getCert() + Configure.UPDATE_ALIAS_SUB_CREDIT);
		saveAccountInfo(accountInfo);
		
		put(Sha256Hash.hash(alias), hash160);
		
		return true;
	}
	
	/**
	 * 添加防伪码流转信息
	 * @param antifakeCode
	 * @param hash160
	 * @param txHash
	 */
	public void addCirculation(byte[] antifakeCode, byte[] hash160, Sha256Hash txHash) {
		byte[] circulationKey = new byte[22];
		circulationKey[0] = 0;
		circulationKey[1] = 1;
		System.arraycopy(antifakeCode, 0, circulationKey, 2, Address.LENGTH);
		
		byte[] circulations = getBytes(circulationKey);
		if(circulations == null) {
			circulations = new byte[0];
		}
		byte[] newCirculations = new byte[circulations.length +  Address.LENGTH + Sha256Hash.LENGTH];
		System.arraycopy(circulations, 0, newCirculations, 0, circulations.length);
		System.arraycopy(hash160, 0, newCirculations, circulations.length, Address.LENGTH);
		System.arraycopy(txHash.getBytes(), 0, newCirculations, circulations.length + Address.LENGTH, Sha256Hash.LENGTH);
		put(circulationKey, newCirculations);
	}

	/**
	 * 获取防伪码对应人已添加的流转信息数量
	 * @param antifakeCode
	 * @param hash160
	 * @return int
	 */
	public int getCirculationCount(byte[] antifakeCode, byte[] hash160) {
		byte[] circulationKey = new byte[22];
		circulationKey[0] = 0;
		circulationKey[1] = 1;
		System.arraycopy(antifakeCode, 0, circulationKey, 2, Address.LENGTH);
		
		byte[] circulations = getBytes(circulationKey);
		
		if(circulations == null) {
			return 0;
		}
		
		int count = 0;
		for (int j = 0; j < circulations.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
			byte[] addressHash160 = Arrays.copyOfRange(circulations, j, j + Address.LENGTH);
			if(Arrays.equals(addressHash160, hash160)) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * 获取防伪码对应已添加的流转信息数量
	 * @param antifakeCode
	 * @return int
	 */
	public int getCirculationCount(byte[] antifakeCode) {
		byte[] circulationKey = new byte[22];
		circulationKey[0] = 0;
		circulationKey[1] = 1;
		System.arraycopy(antifakeCode, 0, circulationKey, 2, Address.LENGTH);
		
		byte[] circulations = getBytes(circulationKey);
		
		if(circulations == null) {
			return 0;
		}
		
		return circulations.length / (Address.LENGTH + Sha256Hash.LENGTH);
	}
	
	/**
	 * 获取防伪码流转信息列表
	 * @param antifakeCode
	 * @return List<CirculationTransaction>
	 */
	public List<CirculationTransaction> getCirculationList(byte[] antifakeCode) {
		List<CirculationTransaction> list = new ArrayList<CirculationTransaction>();
		
		byte[] circulationKey = new byte[22];
		circulationKey[0] = 0;
		circulationKey[1] = 1;
		System.arraycopy(antifakeCode, 0, circulationKey, 2, Address.LENGTH);
		
		byte[] circulations = getBytes(circulationKey);
		
		if(circulations == null) {
			return list;
		}
		
		for (int j = 0; j < circulations.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
			Sha256Hash txHash = Sha256Hash.wrap(Arrays.copyOfRange(circulations, j + Address.LENGTH, j + Address.LENGTH + Sha256Hash.LENGTH));
			
			TransactionStore txs = blockStoreProvider.getTransaction(txHash.getBytes());
			if(txs == null) {
				continue;
			}
			CirculationTransaction ctx = (CirculationTransaction) txs.getTransaction();
			list.add(ctx);
		}
		return list;
	}

	/**
	 * 转让防伪码
	 * @param antifakeCode
	 * @param hash160
	 * @param receiveHashs
	 * @param txHash
	 */
	public void antifakeTransfer(byte[] antifakeCode, byte[] hash160, byte[] receiveHashs, Sha256Hash txHash) {
		byte[] transferKey = new byte[22];
		transferKey[0] = 0;
		transferKey[1] = 2;
		System.arraycopy(antifakeCode, 0, transferKey, 2, Address.LENGTH);
		
		byte[] transfers = getBytes(transferKey);
		if(transfers == null) {
			transfers = new byte[0];
		}
		byte[] newTransfers = new byte[transfers.length +  Address.HASH_LENGTH + Sha256Hash.LENGTH];
		System.arraycopy(transfers, 0, newTransfers, 0, transfers.length);
		System.arraycopy(receiveHashs, 0, newTransfers, transfers.length, Address.HASH_LENGTH);
		System.arraycopy(txHash.getBytes(), 0, newTransfers, transfers.length + Address.HASH_LENGTH, Sha256Hash.LENGTH);
		
		AccountStore accountInfo = getAccountInfo(hash160);
		//扣除信用
		accountInfo.setCert(accountInfo.getCert() + Configure.TRANSFER_ANTIFAKECODE_SUB_CREDIT);
		saveAccountInfo(accountInfo);
				
		put(transferKey, newTransfers);
	}
	
	/**
	 * 获取防伪码的拥有者，流转链的最后一个人，如果流转链没有，则是验证者，会返回null，调用者自行判断
	 * @param antifakeCode
	 * @return byte[]
	 */
	public byte[] getAntifakeCodeOwner(byte[] antifakeCode) {
		byte[] transferKey = new byte[22];
		transferKey[0] = 0;
		transferKey[1] = 2;
		System.arraycopy(antifakeCode, 0, transferKey, 2, Address.LENGTH);
		
		byte[] transfers = getBytes(transferKey);
		if(transfers == null || transfers.length == 0) {
			return null;
		}
		int count = transfers.length / (Address.HASH_LENGTH + Sha256Hash.LENGTH);
		int index = (count - 1) * (Address.HASH_LENGTH + Sha256Hash.LENGTH);
		return Arrays.copyOfRange(transfers, index, index + Address.HASH_LENGTH);
	}
	
	/**
	 * 获取防伪码的转让次数
	 * @param antifakeCode
	 * @return int
	 */
	public int getAntifakeCodeTransferCount(byte[] antifakeCode) {
		byte[] transferKey = new byte[22];
		transferKey[0] = 0;
		transferKey[1] = 2;
		System.arraycopy(antifakeCode, 0, transferKey, 2, Address.LENGTH);
		
		byte[] transfers = getBytes(transferKey);
		if(transfers == null || transfers.length == 0) {
			return 0;
		}
		return transfers.length / (Address.HASH_LENGTH + Sha256Hash.LENGTH);
	}
	
	/**
	 * 获取防伪码转让列表
	 * @param antifakeCode
	 * @return List<CirculationTransaction>
	 */
	public List<AntifakeTransferTransaction> getAntifakeCodeTransferList(byte[] antifakeCode) {
		List<AntifakeTransferTransaction> list = new ArrayList<AntifakeTransferTransaction>();
		
		byte[] transferKey = new byte[22];
		transferKey[0] = 0;
		transferKey[1] = 2;
		System.arraycopy(antifakeCode, 0, transferKey, 2, Address.LENGTH);
		
		byte[] transfers = getBytes(transferKey);
		
		if(transfers == null) {
			return list;
		}
		
		for (int j = 0; j < transfers.length; j += (Address.HASH_LENGTH + Sha256Hash.LENGTH)) {
			Sha256Hash txHash = Sha256Hash.wrap(Arrays.copyOfRange(transfers, j + Address.HASH_LENGTH, j + Address.HASH_LENGTH + Sha256Hash.LENGTH));
			
			TransactionStore txs = blockStoreProvider.getTransaction(txHash.getBytes());
			if(txs == null) {
				continue;
			}
			AntifakeTransferTransaction atx = (AntifakeTransferTransaction) txs.getTransaction();
			list.add(atx);
		}
		return list;
	}

	/**
	 * 获取防伪码验证的交易
	 * @param antifakeCode
	 * @return Sha256Hash
	 */
	public Sha256Hash getAntifakeVerifyTx(byte[] antifakeCode) {
		byte[] key = new byte[22];
		key[0] = 0;
		key[1] = 3;
		System.arraycopy(antifakeCode, 0, key, 2, Address.LENGTH);
		
		byte[] content = getBytes(key);
		if(content == null) {
			return null;
		}
		return Sha256Hash.wrap(content);
	}

	/**
	 * 防伪码验证
	 * @param antifakeCode
	 * @param hash
	 */
	public void verifyAntifakeCode(byte[] antifakeCode, Sha256Hash hash) {
		
		byte[] key = new byte[22];
		key[0] = 0;
		key[1] = 3;
		System.arraycopy(antifakeCode, 0, key, 2, Address.LENGTH);
		
		put(key, hash.getBytes());
	}
}
