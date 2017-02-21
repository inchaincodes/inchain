package org.inchain.network;

import java.net.InetSocketAddress;

/**
 * 种子
 * @author ln
 *
 */
public class Seed {

	public static final int SEED_CONNECT_WAIT = 0;
	public static final int SEED_CONNECT_ING = 1;
	public static final int SEED_CONNECT_SUCCESS = 2;
	public static final int SEED_CONNECT_FAIL = 3;
	public static final int SEED_CONNECT_CLOSE = 4;
	
	private InetSocketAddress address;
	private int staus;			//状态，0待连接，1连接中，2连接成功，3连接失败，4断开连接
	private long lastTime;		//最后连接时间
	private boolean retry;		//连接失败是否重试
	private int retryInterval;	//失败的节点，默认60秒重试
	private int failCount;		//失败了多少次
	
	public Seed(InetSocketAddress address) {
		this(address, false, 60);
	}
	
	public Seed(InetSocketAddress address,  boolean retry, int retryInterval) {
		super();
		this.address = address;
		this.retry = retry;
		this.retryInterval = retryInterval;
	}

	public InetSocketAddress getAddress() {
		return address;
	}
	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}
	public int getStaus() {
		return staus;
	}
	public void setStaus(int staus) {
		this.staus = staus;
	}
	public long getLastTime() {
		return lastTime;
	}
	public void setLastTime(long lastTime) {
		this.lastTime = lastTime;
	}
	public boolean isRetry() {
		return retry;
	}
	public void setRetry(boolean retry) {
		this.retry = retry;
	}
	public int getRetryInterval() {
		return retryInterval;
	}
	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}
	public int getFailCount() {
		return failCount;
	}
	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	@Override
	public String toString() {
		return "Seed [address=" + address + ", staus=" + staus + ", lastTime=" + lastTime + ", retry=" + retry
				+ ", retryInterval=" + retryInterval + "]";
	}
}
