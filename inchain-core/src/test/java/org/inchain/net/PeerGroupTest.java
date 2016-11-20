package org.inchain.net;

import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParameters;
import org.inchain.network.RemoteSeedManager;
import org.inchain.network.TestNetworkParameters;

public class PeerGroupTest {

	public static void main(String[] args) {
		
		RemoteSeedManager seedManager = new RemoteSeedManager();
		
		NetworkParameters params = new TestNetworkParameters(seedManager, 6888);
		
		PeerKit peerGroup = new PeerKit(params, 5);
		
		peerGroup.startSyn();
		
//		try {
//			Thread.sleep(10000l);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		peerGroup.broadcastTransaction(new Transaction());
	}
}
