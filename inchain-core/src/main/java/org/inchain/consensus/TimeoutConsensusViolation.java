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
	
	private long currentPeriodStartTime;
	private long previousPeriodStartTime;
	
	public TimeoutConsensusViolation(byte[] hash160, long currentPeriodStartTime, long previousPeriodStartTime) {
		super();
		this.hash160 = hash160;
		this.currentPeriodStartTime = currentPeriodStartTime;
		this.previousPeriodStartTime = previousPeriodStartTime;
	}
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public long getCurrentPeriodStartTime() {
		return currentPeriodStartTime;
	}
	public void setCurrentPeriodStartTime(long currentPeriodStartTime) {
		this.currentPeriodStartTime = currentPeriodStartTime;
	}
	public long getPreviousPeriodStartTime() {
		return previousPeriodStartTime;
	}
	public void setPreviousPeriodStartTime(long previousPeriodStartTime) {
		this.previousPeriodStartTime = previousPeriodStartTime;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimeoutConsensusViolation [hash160=");
		builder.append(Hex.encode(hash160));
		builder.append(", currentPeriodStartTime=");
		builder.append(DateUtil.convertDate(new Date(currentPeriodStartTime * 1000)));
		builder.append(", previousPeriodStartTime=");
		builder.append(DateUtil.convertDate(new Date(previousPeriodStartTime * 1000)));
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
