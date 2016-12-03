package org.inchain.net;

import java.net.InetSocketAddress;

import org.inchain.BaseTestCase;
import org.inchain.kits.PeerKit;
import org.inchain.network.MainNetworkParams;
import org.inchain.network.NetworkParams;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.transaction.Transaction;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PeerGroupTest2 extends BaseTestCase {

	@Autowired
	private PeerKit peerKit;
	@Autowired
	private NetworkParams network;
	
	@Test
	public void test() {
		
//		SeedManager seedManager = new RemoteSeedManager();
//		seedManager.add(new Seed(new InetSocketAddress("192.168.1.181", 6888)));

		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 6888), true, 25000));
		
		peerKit.startSyn();

		while(true) {
			peerKit.broadcastTransaction(new Transaction(network));
			try {
				Thread.sleep(100000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
