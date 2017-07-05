package org.inchain.consensus;

import java.util.Arrays;

import org.inchain.account.Address;
import org.inchain.core.VarInt;
import org.inchain.crypto.Sha256Hash;
import org.inchain.utils.ByteArrayTool;
import org.inchain.utils.Hex;

/**
 * 共识账户
 * @author ln
 *
 */
public class ConsensusAccount {

	//打包人
	private byte[] hash160;
	//委托人
	private byte[] commissioned;

	private int length;
	private String hash160Hex;
	private Sha256Hash sortValue;
	
	public ConsensusAccount(byte[] hash160, byte[] commissioned) {
		this.hash160 = hash160;
		this.commissioned = commissioned;
	}
	
	public ConsensusAccount(byte[] content, int offset) {
		parse(content, offset);
	}

	public byte[] baseSerialize() {
		
		ByteArrayTool byteArray = new ByteArrayTool();
		
		byteArray.append(hash160);

		return byteArray.toArray();
	}
	
	public void parse(byte[] content, int offset) {
		
		int cursor = offset;
		
		hash160 = new byte[Address.LENGTH];
		System.arraycopy(content, cursor, hash160, 0, Address.LENGTH);
		cursor += hash160.length;

		length = cursor - offset;
	}
	
	
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public int getLength() {
		return length;
	}
	public String getHash160Hex() {
		if(hash160Hex == null) {
			hash160Hex = Hex.encode(hash160);
		}
		return hash160Hex;
	}
	public Sha256Hash getSortValue() {
		return sortValue;
	}
	public void setSortValue(Sha256Hash sortValue) {
		this.sortValue = sortValue;
	}

	public byte[] getCommissioned() {
		return commissioned;
	}

	@Override
	public String toString() {
		return "ConsensusAccount [hash160=" + Hex.encode(hash160) + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ConsensusAccount) {
			return Arrays.equals(hash160, ((ConsensusAccount)obj).hash160);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(hash160);
	}
}
