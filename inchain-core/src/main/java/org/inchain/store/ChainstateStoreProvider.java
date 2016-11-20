package org.inchain.store;

import org.inchain.network.NetworkParameters;
import org.inchain.transaction.Transaction;

/**
 * 链状态查询提供服务，存放的是所有的未花费交易
 * @author ln
 *
 */
public class ChainstateStoreProvider extends BaseStoreProvider {

	private static ChainstateStoreProvider INSTACE;
	
	//单例
	public static ChainstateStoreProvider getInstace(String dir, NetworkParameters network) {
		return getInstace(dir, network, -1, -1);
	}
	public static ChainstateStoreProvider getInstace(String dir, NetworkParameters network, long leveldbReadCache, int leveldbWriteCache) {
		if(INSTACE == null) {
			synchronized (locker) {
				if(INSTACE == null)
					INSTACE = new ChainstateStoreProvider(dir, network, leveldbReadCache, leveldbWriteCache);
			}
		}
		return INSTACE;
	}
	
	protected ChainstateStoreProvider(String dir, NetworkParameters network) {
		this(dir, network, -1, -1);
	}
	protected ChainstateStoreProvider(String dir, NetworkParameters network, long leveldbReadCache,
			int leveldbWriteCache) {
		super(dir, network, leveldbReadCache, leveldbWriteCache);
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
}
