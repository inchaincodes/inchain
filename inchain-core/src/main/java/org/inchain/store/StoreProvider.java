package org.inchain.store;

import java.io.IOException;

/**
 * 存储服务
 * @author ln
 *
 */
public interface StoreProvider {

	void put(Store store);
	
	Store get(byte[] key);
	
	void delete(byte[] key);
	
	void close() throws IOException;
}
