package org.inchain.net;

import java.net.InetSocketAddress;

import org.inchain.kits.PeerKit;
import org.inchain.network.MainNetParams;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.transaction.Transaction;

public class PeerGroupTest2 {

	public static void main(String[] args) {
		
//		SeedManager seedManager = new RemoteSeedManager();
//		seedManager.add(new Seed(new InetSocketAddress("192.168.1.181", 6888)));

		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 6888), true, 25000));
		
		NetworkParameters network = new MainNetParams(seedManager, 8888);
		
		PeerKit peerGroup = new PeerKit(network, 10);
		
		peerGroup.startSyn();

		while(true) {
			peerGroup.broadcastTransaction(new Transaction(network));
			try {
				Thread.sleep(100000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
