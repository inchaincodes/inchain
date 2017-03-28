package org.inchain.mempool;

import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;

/**
 * 内存池，存放所有待打包的交易
 * @author ln
 *
 */
public interface Mempool {

	/**
	 * 将交易放入内存池中
	 * @param tx	交易
	 * @return boolean
	 */
	boolean add(Transaction tx);
	
	/**
	 * 从内存池中移除交易
	 * @param hash	交易hash
	 * @return	boolean
	 */
	boolean remove(Sha256Hash hash);
	
	/**
	 * 批量从内存池中移除交易
	 * @param hashs	交易hash数组
	 * @return boolean
	 */
	boolean bathRemove(Sha256Hash[] hashs);

	/**
	 * 获取最早的交易，同时从内存池中移除交易
	 * @return Transaction
	 */
	Transaction get();
	
	/**
	 * 获取交易，不会移除
	 * @param hash 交易hash
	 * @return Transaction
	 */
	Transaction get(Sha256Hash hash);
	
	/**
	 * 批量获取最早的交易，获取之后同时从内存池中移除交易
	 * @param max	最大获取数量
	 * @return Transaction[]
	 */
	Transaction[] getNewest(int max);
	
	/**
	 * 获取内存里面交易数量
	 * @return int
	 */
	int getTxCount();
}
