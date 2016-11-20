package org.inchain.mempool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;

/**
 * 内存容器映射，必须要线程安全的
 * 先用ConcurrentSkipListMap简单实现
 * @author ln
 *
 */
public class MempoolContainerMap implements MempoolContainer {

	private static final ConcurrentSkipListMap<Sha256Hash, Transaction> container = new ConcurrentSkipListMap<Sha256Hash, Transaction>();
	private static final MempoolContainer instace = new MempoolContainerMap();

	private MempoolContainerMap() {
	}
	
	public static MempoolContainer getInstace() {
		return instace;
	}
	
	@Override
	public boolean add(Sha256Hash hash, Transaction tx) {
		return container.put(hash, tx) != null;
	}

	@Override
	public boolean remove(Sha256Hash hash) {
		return container.remove(hash) != null;
	}

	@Override
	public boolean bathRemove(Sha256Hash[] hashs) {
		for (Sha256Hash hash : hashs) {
			container.remove(hash);
		}
		return true;
	}

	@Override
	public Transaction get(Sha256Hash hash) {
		return container.get(hash);
	}

	@Override
	public Transaction[] getNewest(int max) {
		List<Transaction> list = new ArrayList<Transaction>();

		while(max > 0) {
			Entry<Sha256Hash, Transaction> entry = container.pollLastEntry();
			if(entry == null) {
				break;
			}
			list.add(entry.getValue());
			max--;
		}
		return list.toArray(new Transaction[list.size()]);
	}
	
}
