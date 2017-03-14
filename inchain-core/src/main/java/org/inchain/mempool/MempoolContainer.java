package org.inchain.mempool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Input;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;

/**
 * 内存容器映射，必须要线程安全的
 * 先用ConcurrentSkipListMap简单实现
 * @author ln
 *
 */
public class MempoolContainer implements Mempool {

	private Lock locker = new ReentrantLock();
	
	private static final ConcurrentLinkedQueue<Transaction> container = new ConcurrentLinkedQueue<Transaction>();
	private static final Map<Sha256Hash, Transaction> indexContainer = new HashMap<Sha256Hash, Transaction>();
	
	private static final Mempool instace = new MempoolContainer();

	
	private MempoolContainer() {
	}
	
	public static Mempool getInstace() {
		return instace;
	}
	
	@Override
	public boolean add(Transaction tx) {
		locker.lock();
		try {
			if(indexContainer.containsKey(tx.getHash())) {
				return false;
			}
			
			//保证新的交易不能双花
			//在transactionValidator.valDo已经检测过了和区块上的不会重复花费，但是内存里面的没有检查
			//这里就检查交易的输入和内存里面的交易输入是否相同，如果相同则代表有双花风险，应该制止
			
			List<Input> thisInputs = tx.getInputs();
			//TODO 可以考虑这里改为布隆过滤器处理，应该会有性能提升？
			if(thisInputs != null && thisInputs.size() > 0) {
				for (Input input : thisInputs) {
					TransactionInput ti = (TransactionInput) input;
					TransactionOutput preOutput = ti.getFrom();
					
					for (Transaction transaction : container) {
						List<Input> inputs = transaction.getInputs();
						if(inputs == null || inputs.size() == 0) {
							continue;
						}
						for (Input input2 : inputs) {
							TransactionInput ti2 = (TransactionInput) input2;
							TransactionOutput preOutput2 = ti2.getFrom();
							if(preOutput2 != null && preOutput2.getParent().getHash().equals(preOutput.getParent().getHash()) && 
									preOutput2.getIndex() == preOutput.getIndex()) {
								return false;
							}
						}
					}
				}
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
