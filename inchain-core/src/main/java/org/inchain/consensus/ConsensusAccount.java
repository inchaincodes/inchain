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

	private byte[] hash160;
	private byte[][] pubkeys;
	
	private int length;
	private String hash160Hex;
	private Sha256Hash sortValue;
	
	public ConsensusAccount(byte[] hash160, byte[][] pubkeys) {
		this.hash160 = hash160;
		this.pubkeys = pubkeys;
	}
	
	public ConsensusAccount(byte[] content) {
		parse(content, 0);
	}
	
	public ConsensusAccount(byte[] content, int offset) {
		parse(content, offset);
	}

	public byte[] baseSerialize() {
		
		ByteArrayTool byteArray = new ByteArrayTool();
		
		byteArray.append(hash160);
		
		if(pubkeys == null) {
			byteArray.append(0);
		} else {
			byteArray.append(pubkeys.length);
			for (byte[] pubkey : pubkeys) {
				byteArray.append(new VarInt(pubkey.length).encode());
				byteArray.append(pubkey);
			}
		}
		
		return byteArray.toArray();
	}
	
	public void parse(byte[] content, int offset) {
		
		int cursor = offset;
		
		hash160 = new byte[Address.LENGTH];
		System.arraycopy(content, cursor, hash160, 0, Address.LENGTH);
		cursor += hash160.length;
		
		int count = content[cursor];
		cursor++;
		
		pubkeys = new byte[count][];
		
		for (int i = 0; i < count; i++) {
			VarInt varint = new VarInt(content, cursor);
			cursor += varint.getOriginalSizeInBytes();
			
			byte[] pubkey = new byte[(int) varint.value];
			System.arraycopy(content, cursor, pubkey, 0, pubkey.length);
			cursor += pubkey.length;
		}
		length = cursor - offset;
	}
	
	
	public byte[] getHash160() {
		return hash160;
	}
	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}
	public byte[][] getPubkeys() {
		return pubkeys;
	}
	public void setPubkeys(byte[][] pubkeys) {
		this.pubkeys = pubkeys;
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
