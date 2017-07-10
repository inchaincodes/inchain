package org.inchain.transaction;

import java.io.IOException;
import java.util.Arrays;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.core.AccountKeyValue;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.utils.Hex;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class CertAccountRegisterTransactionTest extends TestNetBaseTestCase {

	@Autowired
	private AccountKit accountKit;
	@Autowired
	private NetworkParams network;
	@Autowired
	private AppKit appKit;

	@Before
	public void waitAminute() {
		try {
			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@PostConstruct
	public void init() throws IOException {
		appKit.start();
	}
	
	@Test
	public void registerTest() throws Exception {
		
		accountKit.clearAccountList();
		accountKit.loadAccount();
		AccountKeyValue[] values = {
				new AccountKeyValue("name", "名称", "重庆印链科技有限公司"),
				new AccountKeyValue("address", "地址", "重庆市南岸区"),
				new AccountKeyValue("logo", "图片", "http://file.inchain.org/images/inchain_logo_100x100.png"),
				new AccountKeyValue("creditCode", "信用代码", "91500108MA5UB32H3N"),
				new AccountKeyValue("phone", "电话", "023-86331069"),
				new AccountKeyValue("website", "官网", "https://www.inchain.org"),
				new AccountKeyValue("descript", "描述", "账户管理员"),

				//new AccountKeyValue("descript", "描述", "重庆印链科技有限公司是一家以区块链技术驱动的创新型企业，其主导的区块链社区项目Inchain-印链是一个以防伪为基础业务的公开平台，为社会各企业、机构、艺术家等提供品牌、知识产权保护服务。"),
		};
		
		AccountBody body = new AccountBody(values);
		Account maccount  = accountKit.getDefaultAccount();

		
		Account account = accountKit.createNewCertAccount("inchain123456", "inchain123", body, "inchain123","cVLNVfTN62gZjFPJJZMbeEtBMA9dcBAYXA");
		System.out.println("hash160: " + Hex.encode(account.getAddress().getHash160()));
		
		CertAccountRegisterTransaction tx = new CertAccountRegisterTransaction(network, account.getAddress().getHash160(), account.getMgPubkeys(), account.getTrPubkeys(), account.getBody(),maccount.getAddress().getHash160(),maccount.getLevel());

		accountKit.clearAccountList();
		accountKit.loadAccount();
		ECKey[] eckeys = accountKit.getDefaultAccount().decryptionTr("inchain123");
		//ECKey[] eckeys = account.decryptionMg("inchain123456");
		tx.calculateSignature(tx.getHash(), eckeys[0], null);




		//tx.calculateSignature(Sha256Hash.wrap(Hex.decode("7c96dc721df61d797328325873d14fe67aadb4feac07f33cd26b541aa5457c8a")), eckeys[0], eckeys[1]);
		
		tx.verify();
//		tx.verfifyScript();
		
		//序列化和反序列化
		byte[] txContent = tx.baseSerialize();
		System.out.println("tx id is :" + tx.getHash());


		CertAccountRegisterTransaction rtx = new CertAccountRegisterTransaction(network, txContent, 0);

		rtx.verify();

		System.out.println(rtx.getBody().serialize().length);
//		rtx.verfifyScript();
		System.out.println(Hex.encode(rtx.baseSerialize()));
		System.out.println("tx id is :" +rtx.getHash());

        assert(Arrays.equals(tx.baseSerialize(), rtx.baseSerialize()));
	}
}
