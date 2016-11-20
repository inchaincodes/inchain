package org.inchain.account;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.math.BigInteger;
import java.net.InetSocketAddress;

import org.inchain.Configure;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.MainNetParams;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.network.TestNetworkParameters;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountTest {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void testAddress() {
		NetworkParameters network = TestNetworkParameters.get();
		
//		int i = 0;
//		while(true) {
//			Address address = AccountTool.newAddress(network, Address.VERSION_TEST_PK);
//			log.info("new address is :" + address);
//			if(!address.getBase58().startsWith("i")) {
//				System.err.println("==============");
//				return;
//			}
//			i++;
//			if(i == 100) {
//				break;
//			}
//		}
		Address address = Address.fromP2PKHash(network, Address.VERSION_TEST_PK, 
				Utils.sha256hash160(ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"))
						.getPubKey(false)));
		
		assertEquals(address.getBase58(), "i5xL7pYbLsHYwcbmBGHNDxG6vUjqpHQJcf");
		
		address = AccountTool.newAddressFromPrikey(network, Address.VERSION_TEST_PK, new BigInteger(Hex.decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")));
		assertEquals(address.getBase58(), "i3a1NQjXr88yTctEKyTRnbRXxdgLNEvLLw");
		
		address = Address.fromBase58(network, "179sduXmc57hbYsP5Ar476pJKkdx9CyiXD");
		assertEquals(address.getHash160AsHex(), "437e59f902d96c513ecba8e997f982e40a65b461");
	}
	
	@Test
	public void testAccountManager() throws Exception {
		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 6888), true, 25000));
		
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
			Thread.sleep(2000l);
			if(accountKit.getAccountList().isEmpty()) {
				accountKit.createNewAccount("123456", "0123456");
			}
		} finally {
//			accountKit.close();
//			peerKit.stop();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new AccountTest().testAccountManager();
	}
}
