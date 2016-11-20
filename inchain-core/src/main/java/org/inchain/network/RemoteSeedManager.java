package org.inchain.network;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 远程种子节点管理
 * @author ln
 *
 */
public class RemoteSeedManager implements SeedManager {
	
	private List<Seed> list = new ArrayList<Seed>();

	public RemoteSeedManager() {
		
	}
	
	@Override
	public List<Seed> getSeedList(int maxConnectionCount) {
		return list;
	}

	@Override
	public boolean hasMore() {
		return true;
	}

	public boolean add(Seed node) {
		return list.add(node);
	}
}
