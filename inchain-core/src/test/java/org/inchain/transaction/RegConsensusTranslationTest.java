package org.inchain.transaction;

import java.math.BigInteger;

import org.inchain.UnitBaseTestCase;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RegConsensusTranslationTest extends UnitBaseTestCase {
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;

	@Test
	public void testTranslation() {
		
		//共识账户
		ECKey key = ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"));
		Address address = AccountTool.newAddress(network, key);
		
		byte[] hash160 = address.getHash160();
		RegConsensusTransaction regConsensusTransaction = new RegConsensusTransaction(network, 1l, hash160, 1478070769l);
		regConsensusTransaction.sign(key);
		
		regConsensusTransaction.verfify();
		regConsensusTransaction.verfifyScript();
		
		RegConsensusTransaction regConsensusTransactionTemp = new RegConsensusTransaction(network, regConsensusTransaction.baseSerialize(), 0);
		
		Assert.assertEquals(regConsensusTransaction.getHash(), regConsensusTransactionTemp.getHash());
		
		try {
			Thread.sleep(3000l);
		
			peerKit.broadcastMessage(regConsensusTransactionTemp);

			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
