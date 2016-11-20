package org.inchain.db;


import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

/**
 * level db 存储
 * @author ln
 *
 */
public class LevelDB implements Db {

	//TODO 缓存
	
	private DB db = null;
	
	private static final long LEVELDB_READ_CACHE_DEFAULT = 100 * 1048576; // 100 M
    private static final int LEVELDB_WRITE_CACHE_DEFAULT = 10 * 1048576; // 10 M
    
	//DB读写缓存大小
	private long leveldbReadCache;
	private int leveldbWriteCache;
	//数据保存目录
	private String filename;
	
	public LevelDB(String filename) {
		this(filename, LEVELDB_READ_CACHE_DEFAULT, LEVELDB_WRITE_CACHE_DEFAULT);
	}
	
	public LevelDB(String finalname, long leveldbReadCache, int leveldbWriteCache) {
		this.filename = finalname;
		this.leveldbReadCache = leveldbReadCache;
		this.leveldbWriteCache = leveldbWriteCache;
		openDB();
//		list();
	}
	
	private void list() {
		
		long time = System.currentTimeMillis();
		int count = 0;
		DBIterator iterator = db.iterator();
		while(iterator.hasNext()) {
			Entry<byte[], byte[]> item = iterator.next();
			byte[] key = item.getKey();
			byte[] value = item.getValue();
			count ++;
		}
		System.out.println("遍历 " + count + " 条数据，耗时："+(System.currentTimeMillis() - time)+" ms");
	}

	private void openDB() {
        Options options = new Options();
        options.createIfMissing(true);
        // options.compressionType(CompressionType.NONE);
        options.cacheSize(leveldbReadCache);
        options.writeBufferSize(leveldbWriteCache);
        options.maxOpenFiles(10000);
        // options.blockSize(1024*1024*50);
        try {
            db = Iq80DBFactory.factory.open(new File(filename), options);
        } catch (IOException e) {
            throw new RuntimeException("Can not open DB", e);
        }
    }
	
	@Override
	public boolean put(byte[] key, byte[] value) {
		db.put(key, value);
		return true;
	}

	@Override
	public byte[] get(byte[] key) {
		return db.get(key);
	}

	@Override
	public boolean delete(byte[] key) {
		db.delete(key);
		return true;
	}

	@Override
	public void close() throws IOException {
		db.close();
	}

}
