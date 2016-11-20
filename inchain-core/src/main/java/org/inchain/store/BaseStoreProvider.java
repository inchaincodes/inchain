package org.inchain.store;

import java.io.IOException;

import org.inchain.db.Db;
import org.inchain.db.LevelDB;
import org.inchain.network.NetworkParameters;
import org.inchain.utils.Utils;

/**
 * 存储基类服务
 * @author ln
 *
 */
public abstract class BaseStoreProvider implements StoreProvider {
	
	//所有存储提供器都使用单例，这里定义一个用于实例化的单例锁
	protected static Object locker = new Object();

	protected Db db;
	protected NetworkParameters network;
	
	public BaseStoreProvider(String dir, NetworkParameters network) {
		this.db = new LevelDB(Utils.checkNotNull(dir));
		this.network = Utils.checkNotNull(network);
	}
	public BaseStoreProvider(String dir, NetworkParameters network, long leveldbReadCache, int leveldbWriteCache) {
		//存储目录不能为空
		Utils.checkNotNull(dir);
		if(leveldbReadCache <= 0l || leveldbWriteCache <= 0l) {
			this.db = new LevelDB(Utils.checkNotNull(dir));
		} else {
			this.db = new LevelDB(Utils.checkNotNull(dir), leveldbReadCache, leveldbWriteCache);
		}
		this.network = Utils.checkNotNull(network);
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
