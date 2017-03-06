package org.inchain.consensus;

import org.inchain.utils.Hex;

/**
 * 当前打包人的块段和时间段
 * @author ln
 *
 */
public class ConsensusInfos {

	/** 获取的结果是不确定的 **/
	public final static int RESULT_UNCERTAIN = 0;
	/** 获取的结果是确定的 **/
	public final static int RESULT_SUCCESS = 1;
	
	public static ConsensusInfos UNCERTAIN = new ConsensusInfos(RESULT_UNCERTAIN);
	
	public ConsensusInfos(int result) {
		this.result = result;
	}
	
	public ConsensusInfos(long height, byte[] hash160) {
		this.result = RESULT_SUCCESS;
		this.height = height;
		this.hash160 = hash160;
	}
	
	public ConsensusInfos(long height, byte[] hash160, long beginTime, long endTime) {
		this.result = RESULT_SUCCESS;
		this.height = height;
		this.hash160 = hash160;
		this.beginTime = beginTime;
		this.endTime = endTime;
	}

	private int result;
	private long height;
	private byte[] hash160;
	private long beginTime;	//当前共识人的块周期开始时间
	private long endTime;	//当前共识人的块周期结束时间
	
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public int getResult() {
		return result;
	}
	public void setResult(int result) {
		this.result = result;
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

	@Override
	public String toString() {
		return "ConsensusInfos [result=" + result + ", height=" + height + ", hash160=" + Hex.encode(hash160)
				+ ", beginTime=" + beginTime + ", endTime=" + endTime + "]";
	}
	
	
}
