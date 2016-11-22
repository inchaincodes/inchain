package org.inchain.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SeedManagerTest {

	public static void main(String[] args) throws UnknownHostException {
		InetAddress[] response = InetAddress.getAllByName("seed1.inchain.org");
		
		for (InetAddress inetAddress : response) {
			System.out.println(inetAddress);
		}
	}
}
