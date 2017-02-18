package org.inchain.store;

import java.io.IOException;

import org.inchain.db.Db;
import org.inchain.db.LevelDB;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 存储基类服务
 * @author ln
 *
 */
public abstract class BaseStoreProvider implements StoreProvider {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	//所有存储提供器都使用单例，这里定义一个用于实例化的单例锁
	protected static Object locker = new Object();

	protected Db db;
	
	@Autowired
	protected NetworkParams network;
	
	public BaseStoreProvider(String dir) {
		this.db = new LevelDB(Utils.checkNotNull(dir));
	}
	public BaseStoreProvider(String dir, long leveldbReadCache, int leveldbWriteCache) {
		//存储目录不能为空
		Utils.checkNotNull(dir);
		if(leveldbReadCache <= 0l || leveldbWriteCache <= 0l) {
			this.db = new LevelDB(Utils.checkNotNull(dir));
		} else {
			this.db = new LevelDB(Utils.checkNotNull(dir), leveldbReadCache, leveldbWriteCache);
		}
	}
	
	protected abstract byte[] toByte(Store store);
	protected abstract Store pase(byte[] content);
	
	public void put(byte[] key, byte[] value) {
		db.put(key, value);
	}
	
	public byte[] getBytes(byte[] key) {
		return db.get(key);
	}
	
	@Override
	public void put(Store store) {
		byte[] content = toByte(store);
		db.put(store.getKey(), content);
	}

	@Override
	public Store get(byte[] key) {
		if(key == null) {
			return null;
		}
		Store store = pase(db.get(key));
		if(store != null) {
			store.setKey(key);
		}
		return store;
	}

	@Override
	public void delete(byte[] key) {
		db.delete(key);
	}
	
	/**
	 * 释放资源
	 * @throws IOException 
	 */
	public void close() throws IOException {
		db.close();
	}

}
