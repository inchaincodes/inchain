package org.inchain.test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadLockTest {

	private Lock lock = new ReentrantLock();
	private int index;
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void test() {
		lock.lock();
		try {
			System.out.println("in");
			final Thread t = new Thread() {
				@Override
				public void run() {
					index++;
					try {
						Thread.sleep(5000l);
						synchronized (this) {
							notifyAll();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			try {
				t.start();
				synchronized (t) {
					t.wait(1000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("index is "+index);
		} finally {
			lock.unlock();
		}
	}
	
	public static void main(String[] args) {
		final ThreadLockTest test = new ThreadLockTest();
		
		for (int i = 0; i < 3; i++) {
			new Thread() {
				public void run() {
					test.test();
				};
			}.start();
		}
	}
}
