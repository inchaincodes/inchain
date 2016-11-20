package org.inchain.network;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置节点
 * @author ln
 *
 */
public class NodeSeedManager implements SeedManager {

	private List<Seed> list = new ArrayList<Seed>();
	
	public boolean add(Seed seed) {
		return list.add(seed);
	}
	
	@Override
	public List<Seed> getSeedList(int maxConnectionCount) {
		List<Seed> newList = new ArrayList<Seed>();
		List<Seed> removeList = new ArrayList<Seed>();
		//排除连接失败且不重试的
		for (Seed seed : list) {
			if(seed.getStaus() == Seed.SEED_CONNECT_WAIT ||
				(seed.getStaus() == Seed.SEED_CONNECT_FAIL && seed.isRetry() &&
					(System.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()))) {
				newList.add(seed);
			} else if(seed.getStaus() == Seed.SEED_CONNECT_FAIL && !seed.isRetry()) {
				removeList.add(seed);
			}
		}
		list.removeAll(removeList);
		return newList;
	}

	@Override
	public boolean hasMore() {
		for (Seed seed : list) {
			if(seed.getStaus() == Seed.SEED_CONNECT_WAIT ||
				(seed.getStaus() == Seed.SEED_CONNECT_FAIL && seed.isRetry() &&
					(System.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()))) {
				return true;
			}
		}
		return false;
	}
}
