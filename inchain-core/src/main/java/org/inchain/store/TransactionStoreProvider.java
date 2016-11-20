package org.inchain.store;

import org.inchain.core.Coin;
import org.inchain.network.NetworkParameters;
import org.inchain.transaction.Transaction;

/**
 * 交易存储提供服务
 * @author ln
 *
 */
public class TransactionStoreProvider extends ChainstateStoreProvider {

	private static TransactionStoreProvider INSTACE;
	
	//单例
	public static TransactionStoreProvider getInstace(String dir, NetworkParameters network) {
		return getInstace(dir, network, -1, -1);
	}
	public static TransactionStoreProvider getInstace(String dir, NetworkParameters network, long leveldbReadCache, int leveldbWriteCache) {
		if(INSTACE == null) {
			synchronized (locker) {
				if(INSTACE == null)
					INSTACE = new TransactionStoreProvider(dir, network, leveldbReadCache, leveldbWriteCache);
			}
		}
		return INSTACE;
	}
	
	private TransactionStoreProvider(String dir, NetworkParameters network) {
		super(dir, network);
	}
	private TransactionStoreProvider(String dir, NetworkParameters network, long leveldbReadCache, int leveldbWriteCache) {
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
	 * 获取地址的最新余额和未确认的余额
	 * @param hash160
	 * @return Coin[]
	 */
	public Coin[] getBalanceAndUnconfirmedBalance(byte[] hash160) {
		Coin balance = Coin.ZERO;
		Coin unconfirmedBalance = Coin.ZERO;
		
		
		
		return new Coin[]{balance, unconfirmedBalance};
	}
}
