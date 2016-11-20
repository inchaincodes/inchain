package org.inchain.test;

import org.inchain.utils.Hex;

public class Base16Test {

	public static void main(String[] args) {
		String res = Hex.encode("12#$%089*)(8df0sa0SfLKALf0sa9ss3456".getBytes());
		System.out.println(res);
	}
}
