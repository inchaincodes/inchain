package org.inchain.transaction;

import java.math.BigInteger;

import org.inchain.UnitBaseTestCase;
import org.inchain.account.Account;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.business.RegConsensusTransaction;
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
		RegConsensusTransaction regConsensusTransaction = new RegConsensusTransaction(network, 1l, 1478070769l);
		Address address = AccountTool.newAddress(network, key);
		
		Account account = new Account(network);
		account.setAddress(address);
		account.setEcKey(key);
		regConsensusTransaction.sign(account);
		
		regConsensusTransaction.verify();
		regConsensusTransaction.verifyScript();
		
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
