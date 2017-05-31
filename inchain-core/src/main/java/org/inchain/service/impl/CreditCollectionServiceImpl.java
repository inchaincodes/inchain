package org.inchain.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.inchain.Configure;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
import org.inchain.service.CreditCollectionService;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.business.CreditTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 信用采集服务
 * //TODO 定时清理过期的资源，以免造成OOM
 * @author ln
 *
 */
@Service
public class CreditCollectionServiceImpl implements CreditCollectionService {
	
	private static final Logger log = LoggerFactory.getLogger(CreditCollectionServiceImpl.class);
	
	private Map<Integer, Map<ByteHash, List<Long>>> container = new HashMap<Integer, Map<ByteHash, List<Long>>>();

	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
	/**
	 * 初始化，加载最近48小时内的信用累积记录
	 */
	@PostConstruct
	public void init() {
		Thread t = new Thread() {
			@Override
			public void run() {
				//延迟1秒启动
				try {
					Thread.sleep(1000l);
				} catch (InterruptedException e) {
					log.error("", e);
				}
				onload();
			}
		};
		t.setName("credit collection service");
		t.start();
	}
	
	/**
	 * 验证是否可以获得信用
	 * @param type			类型，参考 Definition 里的定义
	 * @param hash160		信用获得人
	 * @param time			以凭证打包进区块的时间为准，也就是以凭证所在的区块时间为准来判断
	 * @return boolean
	 */
	@Override
	public boolean verification(int type, byte[] hash160, long time) {
		Map<ByteHash, List<Long>> map = container.get(type);
		if(map == null) {
			return true;
		}
		List<Long> times = map.get(new ByteHash(hash160));
		if(times == null || times.size() == 0) {
			return true;
		}
		if(time - times.get(0) >= Configure.CERT_CHANGE_PAY_INTERVAL_SECOND) {
			return true;
		}
		return false;
	}

	/**
	 * 增加信用
	 * @param type		类型，参考 Definition 里的定义
	 * @param hash160	信用获得人
	 * @param time			凭证所在的区块时间
	 * @return boolean
	 */
	@Override
	public boolean addCredit(int type, byte[] hash160, long time) {
		Map<ByteHash, List<Long>> map = container.get(type);
		if(map == null) {
			map = new HashMap<ByteHash, List<Long>>();
			container.put(type, map);
		}
		List<Long> times = map.get(new ByteHash(hash160));
		if(times == null) {
			times = new ArrayList<Long>();
			map.put(new ByteHash(hash160), times);
		}
		times.add(0, time);
		return true;
	}

	/**
	 * 移除最近的记录，主链上的块回滚时调用
	 * @param type		类型，参考 Definition 里的定义
	 * @param hash160	信用获得人
	 * @return boolean
	 */
	@Override
	public boolean removeCredit(int type, byte[] hash160) {
		Map<ByteHash, List<Long>> map = container.get(type);
		if(map == null) {
			return false;
		}
		List<Long> times = map.get(new ByteHash(hash160));
		if(times == null || times.size() == 0) {
			return false;
		}
		return times.remove(0) != null;
	}

	/**
	 * 系统启动初始加载，加载最近的信用累积记录，具体多少时间根据设置而定
	 * 会阻塞直到加载完成
	 * @return boolean
	 */
	@Override
	public boolean onload() {

		long nowTime = TimeService.currentTimeMillis();
		
		BlockStore blockStore = blockStoreProvider.getBestBlock();
		
		if(blockStore == null) {
			//代表第一次启动，不做任何处理
			return true;
		}
		Block block = blockStore.getBlock();
		
		while(true) {
			
			//遍历交易记录，找出信用累积交易
			List<Transaction> txs = block.getTxs();
			
			for (Transaction tx : txs) {
				if(tx.getType() == Definition.TYPE_CREDIT) {
					CreditTransaction ctx = (CreditTransaction) tx;
					byte[] hash160 = ctx.getOwnerHash160();
					
					addCredit(ctx.getReasonType(), hash160, block.getTime());
				}
			}
			
			if(Sha256Hash.ZERO_HASH.equals(block.getPreHash())) {
				break;
			}
			//如果没有查询到上一个块，则停止，这种情况一般不会发生，做判断是良好的避免NPE的习惯
			blockStore = blockStoreProvider.getBlock(block.getPreHash().getBytes());
			if(blockStore == null) {
				break;
			}
			block = blockStore.getBlock();
			//如果时间超过了要取的范围，就停止
			if(nowTime - block.getTime() * 1000 > Configure.CERT_CHANGE_PAY_INTERVAL) {
				break;
			}
		}
		log.info("加载信用记录成功，耗时{}毫秒", TimeService.currentTimeMillis() - nowTime);
		
		return true;
	}
	
	/**
	 * 清理数据
	 */
	@Override
	public boolean clean() {
		container.clear();
		return true;
	}

	static class ByteHash {
		byte[] hash;

		public ByteHash(byte[] hash) {
			this.hash = hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof ByteHash)) {
				return false;
			}
			ByteHash temp = (ByteHash)obj;
			if(hash == null || temp.hash == null) {
				return false;
			}
			return Arrays.equals(hash, temp.hash);
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(hash);
		}
	}
}
