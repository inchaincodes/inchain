package org.inchain.core;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.inchain.BaseTestCase;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AccountTest extends BaseTestCase {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private NetworkParams network;

//	@Test
//	public void testAllPrefix() {
//		for (int i = 0; i < 254; i++) {
//			Address address = AccountTool.newAddress(network, i);
//			if(address.getBase58().startsWith("V")) {
//				System.out.println(address.getBase58()+"    "+i);
//			}
//		}
//		
//		for (int i = 0; i < 100000; i++) {
//			Address address = AccountTool.newAddress(network, 71);
//			if(!address.getBase58().startsWith("V")) {
//				System.err.println(address.getBase58()+"   ========  "+i);
//			}
//		}
//	}

	@Test
	public void testAddress() {
		ECKey key = AccountTool.newPriKey();
		
		log.info("pri key is :" + key.getPrivateKeyAsHex());
		log.info("pub key is :" + key.getPublicKeyAsHex());
		log.info("pub key not compressed is :" + key.getPublicKeyAsHex(false));
		
		int i = 0;
		while(true) {
			Address address = AccountTool.newAddress(network, network.getSystemAccountVersion());
			log.info("new address is :" + address);
			if(!address.getBase58().startsWith("u")) {
				System.err.println("==============");
				return;
			}
			i++;
			if(i == 100) {
				break;
			}
		}
		Address address = Address.fromP2PKHash(network, network.getSystemAccountVersion(), 
				Utils.sha256hash160(ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"))
						.getPubKey(false)));
		
		assertEquals(address.getBase58(), "uMRDgrtfDvG5qkWBs1cHoTt8YbxFf7cDch");
		
		address = AccountTool.newAddressFromPrikey(network, network.getSystemAccountVersion(), new BigInteger(Hex.decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")));
		assertEquals(address.getBase58(), "uK2twT5bjB7WMknf1inMN73ZaktkGMSMnP");
		
		address = Address.fromBase58(network, "uK2twT5bjB7WMknf1inMN73ZaktkGMSMnP");
		assertEquals(address.getHash160AsHex(), "010966776006953d5567439e5e39f86a0d273bee");
	}
	
}
