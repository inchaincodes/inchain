package org.inchain.mempool;

import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;

/**
 * 内存池，存放所有待打包的交易
 * @author ln
 *
 */
public interface MempoolContainer {

	boolean add(Sha256Hash hash, Transaction tx);
	
	boolean remove(Sha256Hash hash);
	
	boolean bathRemove(Sha256Hash[] hashs);
	
	Transaction get(Sha256Hash hash);
	
	Transaction[] getNewest(int max);
}
