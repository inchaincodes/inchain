package org.inchain.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.inchain.utils.IpUtil;

public class SeedManagerTest {

	public static void main(String[] args) throws UnknownHostException {
		InetAddress[] response = InetAddress.getAllByName("test1.seed.inchain.org");
		
		for (InetAddress inetAddress : response) {
			System.out.println(inetAddress);
		}
		
		System.out.println(IpUtil.getIps());
		
		System.out.println(InetAddress.getLocalHost());
		
	}
}
