package org.inchain.consensus;

/**
 * 打包信息，轮到我打包时，根据共识会议，获取我的打包信息
 * @author ln
 *
 */
public class MiningInfos {

	//我的时段
	private int timePeriod;
	//当前时段总数，也就是参与共识的人数
	private int periodCount;
	//当前轮开始时间
	private long periodStartTime;
	//当前轮结束时间
	private long periodEndTime;
	//我的账户hash
	private byte[] hash160;
	//我的时段开始时间
	private long beginTime;
	//我的时段结束时间
	private long endTime;
	
	public int getTimePeriod() {
		return timePeriod;
	}
	public void setTimePeriod(int timePeriod) {
		this.timePeriod = timePeriod;
	}
	public int getPeriodCount() {
		return periodCount;
	}
	public void setPeriodCount(int periodCount) {
		this.periodCount = periodCount;
	}
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public long getBeginTime() {
		return beginTime;
	}
	public void setBeginTime(long beginTime) {
		this.beginTime = beginTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	public long getPeriodStartTime() {
		return periodStartTime;
	}
	public void setPeriodStartTime(long periodStartTime) {
		this.periodStartTime = periodStartTime;
	}
	public long getPeriodEndTime() {
		return periodEndTime;
	}
	public void setPeriodEndTime(long periodEndTime) {
		this.periodEndTime = periodEndTime;
	}
}
