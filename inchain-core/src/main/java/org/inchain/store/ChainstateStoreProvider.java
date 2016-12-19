package org.inchain.store;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.transaction.Transaction;
import org.iq80.leveldb.DBIterator;
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
	 * 加载所以的共识节点
	 * @return
	 */
	public Map<byte[], byte[]> loadAllConsensusAccount() {
		DBIterator iterator = db.getSourceDb().iterator();
		Map<byte[], byte[]> consunsusMap = new HashMap<byte[], byte[]>();
		while(iterator.hasNext()) {
			Entry<byte[], byte[]> item = iterator.next();
			byte[] key = item.getKey();
			if(key.length == Address.LENGTH) {
				byte[] value = item.getValue();
				if(value.length >= 42) {
					if(value[41] == 1) {
						consunsusMap.put(key, Arrays.copyOfRange(value, 8, 41));
					}
				}
			}
		}
		return consunsusMap;
	}
}
