package org.inchain.mempool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;

/**
 * 内存容器映射，必须要线程安全的
 * 先用ConcurrentSkipListMap简单实现
 * @author ln
 *
 */
public class MempoolContainerMap implements MempoolContainer {

	private Lock locker = new ReentrantLock();
	
	private static final ConcurrentLinkedQueue<Transaction> container = new ConcurrentLinkedQueue<Transaction>();
	private static final Map<Sha256Hash, Transaction> indexContainer = new HashMap<Sha256Hash, Transaction>();
	
	private static final MempoolContainer instace = new MempoolContainerMap();

	
	private MempoolContainerMap() {
	}
	
	public static MempoolContainer getInstace() {
		return instace;
	}
	
	@Override
	public boolean add(Transaction tx) {
		locker.lock();
		try {
			if(indexContainer.containsKey(tx.getHash())) {
				return false;
			}
			boolean success = container.add(tx);
			if(success) {
				indexContainer.put(tx.getHash(), tx);
			}
			return success;
		} finally {
			locker.unlock();
		}
	}

	@Override
	public boolean remove(Sha256Hash hash) {
		Transaction tx = indexContainer.remove(hash);
		if(tx != null) {
			return container.remove(tx);
		} else {
			return false;
		}
	}

	@Override
	public boolean bathRemove(Sha256Hash[] hashs) {
		for (Sha256Hash hash : hashs) {
			container.remove(hash);
		}
		return true;
	}

	@Override
	public Transaction get() {
		Transaction tx = container.poll();
		if(tx != null) {
			indexContainer.remove(tx.getHash());
		}
		return tx;
	}
	
	@Override
	public Transaction get(Sha256Hash hash) {
		return indexContainer.get(hash);
	}

	@Override
	public Transaction[] getNewest(int max) {
		List<Transaction> list = new ArrayList<Transaction>();

		while(max > 0) {
			Transaction tx = container.poll();
			if(tx == null) {
				break;
			} else {
				indexContainer.remove(tx.getHash());
			}
			list.add(tx);
			max--;
		}
		return list.toArray(new Transaction[list.size()]);
	}
	
}
