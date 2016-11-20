package org.inchain.db;

import java.io.IOException;

/**
 * 存储接口
 * @author ln
 *
 */
public interface Db {

	boolean put(byte[] key, byte[] value);
	
	byte[] get(byte[] key);

	boolean delete(byte[] key);
	
	void close() throws IOException;
}
