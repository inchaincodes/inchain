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
	
	private long preBlockHeight;
	private long nextBlockHeight;
	
	public TimeoutConsensusViolation(byte[] hash160, long preBlockHeight, long nextBlockHeight) {
		super();
		this.hash160 = hash160;
		this.preBlockHeight = preBlockHeight;
		this.nextBlockHeight = nextBlockHeight;
	}
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public long getPreBlockHeight() {
		return preBlockHeight;
	}
	public void setPreBlockHeight(long preBlockHeight) {
		this.preBlockHeight = preBlockHeight;
	}
	public long getNextBlockHeight() {
		return nextBlockHeight;
	}
	public void setNextBlockHeight(long nextBlockHeight) {
		this.nextBlockHeight = nextBlockHeight;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimeoutConsensusViolation [hash160=");
		builder.append(Hex.encode(hash160));
		builder.append(", preBlockHeight=");
		builder.append(preBlockHeight);
		builder.append(", nextBlockHeight=");
		builder.append(nextBlockHeight);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Hex.encode(hash160).hashCode();
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
