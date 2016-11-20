package org.inchain.test;

import org.inchain.utils.Base58;

public class Base58Test {

	public static void main(String[] args) {
		byte[] b = Base58.decode("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
		System.out.println(b.length);
		System.out.println(new String(b));
		
		byte[] versionAndDataBytes = Base58.decodeChecked("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        byte versionByte = versionAndDataBytes[0];

        int version = versionByte & 0xFF;
        System.out.println("version is "+version);
        
        System.out.println(versionAndDataBytes.length);
        
        byte[] bytes = new byte[versionAndDataBytes.length - 1];
        System.arraycopy(versionAndDataBytes, 1, bytes, 0, versionAndDataBytes.length - 1);
        
		System.out.println(new String(bytes));
		
	}
}
