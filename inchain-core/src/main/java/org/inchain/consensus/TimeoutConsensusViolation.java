package org.inchain.consensus;

import java.util.Arrays;

import org.inchain.utils.Hex;

/**
 * 共识违规 - 超时没有发布新块
 * @author ln
 *
 */
public class TimeoutConsensusViolation {
	
	private byte[] hash160;
	
	private long periodStartPoint;
	
	public TimeoutConsensusViolation(byte[] hash160, long periodStartPoint) {
		super();
		this.hash160 = hash160;
		this.periodStartPoint = periodStartPoint;
	}
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	
	public long getPeriodStartPoint() {
		return periodStartPoint;
	}
	public void setPeriodStartPoint(long periodStartPoint) {
		this.periodStartPoint = periodStartPoint;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimeoutConsensusViolation [hash160=");
		builder.append(Hex.encode(hash160));
		builder.append(", periodStartPoint=");
		builder.append(periodStartPoint);
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
