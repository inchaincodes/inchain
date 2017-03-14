package org.inchain.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.inchain.utils.Utils;
import org.springframework.stereotype.Service;

/**
 * 时间服务，存储网络时间与本地时间，所有与网络交互的时间，经过换算，保证网络时间同步
 * @author ln
 *
 */
@Service
public final class TimeService {

	/** 时间偏移差距触发点，超过该值会导致本地时间重设，单位毫秒 **/
	public static final long TIME_OFFSET_BOUNDARY = 2000l;
	
	/*
	 * 网络时间初始化标志
	 */
	private volatile static boolean netTimeInit;
	/*
	 * 模拟网络时钟 
	 */
	private volatile static Date mockTime;
	/*
	 * 网络时间与本地时间的偏移
	 */
	private static long netTimeOffset;
	
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
		Thread monitorThread = new Thread() {
			@Override
			public void run() {
				monitorTimeChange();
			}
		};
		monitorThread.setName("time change monitor");
		monitorThread.start();
	}

	/**
	 * 启动一个服务，监控本地时间的变化
	 * 如果本地时间有变化，则设置 TimeService.netTimeOffset;
	 */
	public void monitorTimeChange() {
		long lastTime = System.currentTimeMillis();
		while(true) {
			//动态调整网络时间
//			if(currentTimeMillis() - () > TIME_OFFSET_BOUNDARY) {
//				
//			}
			
			long newTime = System.currentTimeMillis();
			if(Math.abs(newTime - lastTime) > 800) {
				System.out.println("本地时间调整了："+((newTime - lastTime)));
				TimeService.netTimeOffset -= (newTime - lastTime);
			}
			lastTime = newTime;
			try {
				Thread.sleep(500l);
			} catch (InterruptedException e) {
			}
		}
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
        return System.currentTimeMillis() + netTimeOffset;
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
	
	/**
	 * 返回当前时间毫秒数的字节数组
	 * @return byte[]
	 */
	public static byte[] currentTimeMillisOfBytes() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
		try {
			Utils.int64ToByteStreamLE(currentTimeMillis(), bos);
			return bos.toByteArray();
		} catch (Exception e) {
			return null;
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 初始化网络时间
	 * 规则：
	 * 	1、只要有一个节点的时间和本地时间差距不超过2s，则以本地时间为准，这样能有效的防止恶意节点的欺骗
	 * 	2、当所有连接的节点时间偏移超过2s，则取多数时间相近的节点时间作为网络时间
	 * @return boolean
	 */
	public static boolean netTimeHasInit() {
		return netTimeInit;
	}
	public static void initNetTime() {
		netTimeInit = true;
	}

	/**
	 * 设置网络偏移时间
	 * @param netTimeOffset
	 */
	public static void setNetTimeOffset(long netTimeOffset) {
		TimeService.netTimeOffset = netTimeOffset;
	}
	public static long getNetTimeOffset() {
		return netTimeOffset;
	}
}
