package org.inchain.core;

import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import org.inchain.kits.PeerKit;
import org.inchain.listener.ConnectionChangedListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 时间服务，存储网络时间与本地时间，所有与网络交互的时间，经过换算，保证网络时间同步
 * @author ln
 *
 */
@Service
public final class TimeService {

	@Autowired
	private PeerKit peerKit;
	
	/*
	 * 模拟网络时钟 
	 */
	private volatile static Date mockTime;
	
	/*
	 * 系统启动时间
	 */
	private final static Date systemStartTime = new Date();
	
	public TimeService() {
		startSyn();
	}

	/**
	 * 异步启动时间服务
	 */
	private void startSyn() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(2000l);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				TimeService.this.start();
			};
		}.start();
	}
	
	private void start() {
		//监听节点变化
		peerKit.addConnectionChangedListener(new ConnectionChangedListener() {
			@Override
			public void onChanged(int inCount, int outCount, CopyOnWriteArrayList<Peer> inPeers,
					CopyOnWriteArrayList<Peer> outPeers) {
				//计算连接的对等体与本地时差
				//TODO
			}
		});
	}
	
	/**
     * 以给定的秒数推进（或倒退）网络时间
     */
    public static Date rollMockClock(int seconds) {
        return rollMockClockMillis(seconds * 1000);
    }

    /**
     * 以给定的毫秒数推进（或倒退）网络时间
     */
    public static Date rollMockClockMillis(long millis) {
        if (mockTime == null)
            throw new IllegalStateException("You need to use setMockClock() first.");
        mockTime = new Date(mockTime.getTime() + millis);
        return mockTime;
    }

    /**
     * 将网络时间设置为当前时间
     */
    public static void setMockClock() {
        mockTime = new Date();
    }

    /**
     * 将网络时间设置为给定时间（以秒为单位）
     */
    public static void setMockClock(long mockClockSeconds) {
        mockTime = new Date(mockClockSeconds * 1000);
    }

    /**
	 * 当前网络时间
	 * @return long
	 */
    public static Date now() {
        return mockTime != null ? mockTime : new Date();
    }

    /**
	 * 当前毫秒时间
	 * @return long
	 */
    public static long currentTimeMillis() {
        return mockTime != null ? mockTime.getTime() : System.currentTimeMillis();
    }
	
	/**
	 * 当前时间秒数
	 * @return long
	 */
	public static long currentTimeSeconds() {
		return currentTimeMillis() / 1000;
	}
	
	/**
	 * 获取系统运行时长（返回毫秒数）
	 * @return long
	 */
	public static long getSystemRuningTimeMillis() {
		return new Date().getTime() - systemStartTime.getTime();
	}
	
	/**
	 * 获取系统运行时长（返回秒数）
	 * @return long
	 */
	public static long getSystemRuningTimeSeconds() {
		return getSystemRuningTimeMillis() / 1000;
	}
}
