package org.inchain.test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Test {

	public static class Mytask implements Callable<String> {
		@Override
		public String call() throws Exception {
			System.out.println("---------------------start");
			Thread.sleep(4000l);
			System.out.println("---------------------end");
			return "result";
		}
		
	}
	
	public static class MytaskListener {
		public void done() {
			
		}
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		ExecutorService executors = Executors.newCachedThreadPool();
		Future<String> f = executors.submit(new Mytask());
		
		System.out.println(" wait ....");
		String s = f.get();
		System.out.println(s);
		
		executors.shutdownNow();
	}
}
