package org.inchain.core;

import org.inchain.kits.AppKit;
import org.inchain.network.TestNetworkParameters;

public class AppKitTest {

	public static void main(String[] args) {
		
		TestNetworkParameters network = TestNetworkParameters.get();
		
		AppKit kit = new AppKit(network);
		kit.startSyn();
	}
}
