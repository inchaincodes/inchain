package org.inchain.test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.inchain.crypto.ECKey;

public class ECkeyTest {

	public static void main(String[] args) throws InterruptedException {
		
		ECKey ek = new ECKey();
		System.out.println(ek.getPrivKey());
		
		final Set<Integer> set = new HashSet<Integer>();
		
		long time = System.currentTimeMillis();
		
		ExecutorService executors = Executors.newFixedThreadPool(4);
		
		for (int i = 0; i < 100; i++) {
			
			executors.execute(new Thread(){
				@Override
				public void run() {
					ECKey key = new ECKey();
					
					set.add(key.getPubKey(true).length);
				}
			});
		}
		executors.shutdown();
		executors.awaitTermination(1000, TimeUnit.SECONDS);
		
		System.out.println(set);
		System.out.println("耗时："+(System.currentTimeMillis() - time));
	}
}
