package org.inchain.consensus;

import java.util.Arrays;
import java.util.Date;

import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;

/**
 * 共识违规 - 超时没有发布新块
 * @author ln
 *
 */
public class TimeoutConsensusViolation {
	
	private byte[] hash160;
	
	private long periodStartTime;
	
	public TimeoutConsensusViolation(byte[] hash160, long periodStartTime) {
		super();
		this.hash160 = hash160;
		this.periodStartTime = periodStartTime;
	}
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public long getPeriodStartTime() {
		return periodStartTime;
	}
	public void setPeriodStartTime(long periodStartTime) {
		this.periodStartTime = periodStartTime;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimeoutConsensusViolation [hash160=");
		builder.append(Hex.encode(hash160));
		builder.append(", periodStartTime=");
		builder.append(DateUtil.convertDate(new Date(periodStartTime * 1000)));
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(hash160);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TimeoutConsensusViolation) {
			return Arrays.equals(hash160, ((TimeoutConsensusViolation)obj).hash160);
		} else {
			return false;
		}
	}
}
