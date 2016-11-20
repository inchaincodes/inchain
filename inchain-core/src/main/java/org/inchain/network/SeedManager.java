package org.inchain.network;

import java.util.List;

public interface SeedManager {

	boolean add(Seed seed);
	
	List<Seed> getSeedList(int maxConnectionCount);

	boolean hasMore();
}
