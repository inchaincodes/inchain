package org.inchain.db;

import java.io.IOException;

import org.inchain.db.Db;
import org.inchain.db.LevelDB;
import org.junit.Test;

public class LevelDBTest {

	private static final String filepath = "./data/db/inchain";
	
	@Test
	public void testLevelDb() throws IOException {
		Db storage = new LevelDB(filepath);
		
		long max = 100000;
		for (long i = 0; i < max; i++) {
			String value = "测试，this is value of "+i;
			byte[] key = ("key_"+i).getBytes();
			storage.put(key, value.getBytes());
		}
		//随机读
		long time = System.currentTimeMillis();
		int count = 10000;
		for (long i = 0; i < count; i++) {
			long index = (long) (Math.random()*max);
			byte[] key = ("key_"+index).getBytes();
			String value = new String(storage.get(key));
			System.out.println(value);
		}
		System.out.println("读取 "+count+" 条数据耗时："+(System.currentTimeMillis() - time)+" ms");
		storage.close();
	}
}
