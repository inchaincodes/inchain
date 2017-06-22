package org.inchain.core;

import java.util.Arrays;

public class ByteHash {
	byte[] hash;

	public ByteHash(byte[] hash) {
		this.hash = hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ByteHash)) {
			return false;
		}
		ByteHash temp = (ByteHash)obj;
		if(hash == null || temp.hash == null) {
			return false;
		}
		return Arrays.equals(hash, temp.hash);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(hash);
	}
}