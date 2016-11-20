package org.inchain.account;

import java.io.File;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.inchain.Configure;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.network.TestNetworkParameters;

public class RegisterAccount {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) throws Exception {
		
		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 8322), true, 25000));
		
		NetworkParameters network = new TestNetworkParameters(seedManager, 8888);
		
		//测试前先清空帐户目录
		File dir = new File(Configure.DATA_ACCOUNT);
		if(dir.listFiles() != null) {
			for (File file : dir.listFiles()) {
				file.delete();
			}
		}
		
		PeerKit peerKit = new PeerKit(network);
		peerKit.startSyn();
		
		AccountKit accountKit = new AccountKit(network, peerKit);
		try {
			Thread.sleep(1000l);
			if(accountKit.getAccountList().isEmpty()) {
				accountKit.createNewAccount("123456", "0123456");
			}
		} finally {
//			accountKit.close();
//			peerKit.stop();
		}
	}
	
}
