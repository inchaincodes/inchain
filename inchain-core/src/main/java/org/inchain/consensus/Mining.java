package org.inchain.consensus;

/**
 * 开采待确认交易/也就在大家常说的挖矿
 * @author ln
 *
 */
public interface Mining {
	
	void start();
	
	void stop();
	
	int status();
	
	/**
	 * 执行打包
	 * @param timePeriod 我的时段
	 * @param periodCount	当前时段总数，也就是参与共识的人数
	 */
	void mining(int timePeriod, int periodCount);
}
