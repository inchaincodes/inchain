package org.inchain.transaction;

import org.inchain.account.Account;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MakeCertAccountRegisterTransaction {
	
	public static void main(String[] args) throws Exception {
		
		String[] xmls = new String[] { "classpath:/applicationContext-testnet.xml", "classpath:/applicationContext.xml" };

		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(xmls);
		
		springContext.start();
		
		NetworkParams network = springContext.getBean(NetworkParams.class);
		AccountKit accountKit = springContext.getBean(AccountKit.class);
		
		try {
			Account managerAccount = accountKit.getAccountList().get(0);
			
			byte[] body = "食品测试公司".getBytes();
			Account account = accountKit.createNewCertAccount("123456", "000000", body);
			System.out.println("base58 : " + account.getAddress().getBase58());
			System.out.println("hash160: " + Hex.encode(account.getAddress().getHash160()));
			
			CertAccountRegisterTransaction tx = new CertAccountRegisterTransaction(network, account.getAddress().getHash160(), account.getMgPubkeys(), account.getTrPubkeys(), account.getBody());
			
			ECKey[] eckeys = managerAccount.decryptionMg("123456");
			
			Sha256Hash mgtx = Sha256Hash.wrap(Hex.decode("f8841e56f0f499f7c6c809c8224d8bbde4538d75bdef0c6b1bbf170f317fe700"));
			
			System.out.println("mgtx is : "+mgtx);
			
			tx.calculateSignature(mgtx, eckeys[0], eckeys[1]);
			
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
			System.out.println(new String(rtx.getBody()));

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			springContext.close();
			System.exit(0);
		}
	}
}
