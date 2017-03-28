package org.inchain.account;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.math.BigInteger;
import java.net.InetSocketAddress;

import org.inchain.Configure;
import org.inchain.UnitBaseTestCase;
import org.inchain.crypto.ECKey;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AccountTest extends UnitBaseTestCase {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private NetworkParams network;
	
	@Test
	public void testAddress() {
		
		Address address = Address.fromP2PKHash(network, network.getSystemAccountVersion(), 
				Utils.sha256hash160(ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"))
						.getPubKey(false)));
		
		assertEquals(address.getBase58(), "uMRDgrtfDvG5qkWBs1cHoTt8YbxFf7cDch");
		
		address = AccountTool.newAddressFromPrikey(network, network.getSystemAccountVersion(), new BigInteger(Hex.decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")));
		assertEquals(address.getBase58(), "uK2twT5bjB7WMknf1inMN73ZaktkGMSMnP");
		
	}
	
	@Test
	public void testAccountManager() throws Exception {
		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 6888), true, 25000));
		
		//测试前先清空帐户目录
		File dir = new File(Configure.DATA_ACCOUNT);
		if(dir.listFiles() != null) {
			for (File file : dir.listFiles()) {
				file.delete();
			}
		}
		
		PeerKit peerKit = getBean(PeerKit.class);
		peerKit.startSyn();
		
		AccountKit accountKit = getBean(AccountKit.class);
		try {
			Thread.sleep(2000l);
			if(accountKit.getAccountList().isEmpty()) {
				accountKit.createNewCertAccount("123456", "0123456", AccountBody.empty(), "");
			}
		} finally {
			accountKit.close();
			peerKit.stop();
			
			log.info("test end");
		}
	}
}
