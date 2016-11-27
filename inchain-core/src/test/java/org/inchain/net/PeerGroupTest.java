package org.inchain.net;

import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.inchain.network.RemoteSeedManager;
import org.inchain.network.TestNetworkParams;

public class PeerGroupTest {

	public static void main(String[] args) {
		
		RemoteSeedManager seedManager = new RemoteSeedManager();
		
		NetworkParams params = new TestNetworkParams(seedManager);
		
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
