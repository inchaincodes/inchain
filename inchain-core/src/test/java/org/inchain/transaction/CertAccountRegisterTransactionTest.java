package org.inchain.transaction;

import java.util.Arrays;

import org.inchain.BaseTestCase;
import org.inchain.account.Account;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CertAccountRegisterTransactionTest extends BaseTestCase {

	@Autowired
	private AccountKit accountKit;
	@Autowired
	private NetworkParams network;
	
	@Test
	public void registerTest() throws Exception {
		
		accountKit.clearAccountList();
		
		byte[] body = new byte[1];
		Account account = accountKit.createNewCertAccount("123456", "000000", body);
		System.out.println("hash160: " + Hex.encode(account.getAddress().getHash160()));
		
		CertAccountRegisterTransaction tx = new CertAccountRegisterTransaction(network, account.getAddress().getHash160(), account.getMgPubkeys(), account.getTrPubkeys(), account.getBody());
		
		ECKey[] eckeys = account.decryptionMg("123456");
		tx.calculateSignature(Sha256Hash.wrap(Hex.decode("d936ad91a50e918ebb8376c7335fb7d16255333ea859d64d7d4d1f309052c8f8")), eckeys[0], eckeys[1]);
		
		tx.verfify();
		tx.verfifyScript();
		
		//序列化和反序列化
		byte[] txContent = tx.baseSerialize();
		System.out.println("tx id is :" + tx.getHash());
		
		CertAccountRegisterTransaction rtx = new CertAccountRegisterTransaction(network, txContent, 0);
		
		rtx.verfify();
		rtx.verfifyScript();
		System.out.println(Hex.encode(rtx.baseSerialize()));
		System.out.println("tx id is :" +rtx.getHash());

        assert(Arrays.equals(tx.baseSerialize(), rtx.baseSerialize()));
	}
}
