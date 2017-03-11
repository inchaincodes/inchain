package org.inchain.consensus;

/**
 * 开采待确认交易/也就在大家常说的挖矿
 * @author ln
 *
 */
public interface Mining {
	
	void start();
	
	void stop();
	
	/**
	 * 重置共识
	 * @param stopNow 如果正在共识，是否马上停止
	 */
	void reset(boolean stopNow);
	
	int status();
	
	/**
	 * 执行打包
	 */
	void mining();
	
	/**
	 * 立刻停止打包，注意不是停止服务
	 */
	void stopMining();
}
