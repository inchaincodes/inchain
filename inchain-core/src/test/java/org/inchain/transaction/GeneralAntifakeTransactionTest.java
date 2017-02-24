package org.inchain.transaction;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.core.KeyValuePair;
import org.inchain.core.Product;
import org.inchain.core.Product.ProductType;
import org.inchain.core.TimeService;
import org.inchain.core.Definition;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class GeneralAntifakeTransactionTest extends TestNetBaseTestCase {

	@Autowired
	private AppKit appKit;
	@Autowired
	private AccountKit accountKit;
	@Autowired
	private NetworkParams network;
	
	@PostConstruct
	public void init() {
		appKit.startSyn();
	}
	
	@Before
	public void waitAminute() {
		try {
			Thread.sleep(2000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testMakeProduct() throws Exception {
		
		Product product = createProduct();
		
		assert(product != null);
		
		log.info("产品信息： {}", product);
		
	}
	
	@Test
	public void testMakeTransaction() throws Exception {
		Product product = createProduct();
		
		//我的账户
		Account account = accountKit.getDefaultAccount();
		account.decryptionTr("inchain123");
		
		byte[] sign1 = new byte[0];
		byte[] sign2 = new byte[0];
		
		Sha256Hash txid = account.getAccountTransaction().getHash();
		
		Script signScript = ScriptBuilder.createCertAccountScript(Definition.TX_VERIFY_TR, txid, 
				account.getAddress().getHash160(), sign1, sign2);
		
		GeneralAntifakeTransaction tx = new GeneralAntifakeTransaction(network, product, 0d, 0d, signScript);
		
		tx.sign(account);

		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
	}

	private Product createProduct() {
		List<KeyValuePair> contents = new ArrayList<KeyValuePair>();
		contents.add(new KeyValuePair(ProductType.NAME, "印链-闪迪U盘"));
		contents.add(new KeyValuePair(ProductType.DESCRIPTION, "32G"));
		contents.add(new KeyValuePair(ProductType.CONTENT, "回馈老用户，免费赠送"));
		contents.add(new KeyValuePair(ProductType.PRODUCTION_DATE, "2017-02-23"));
		contents.add(new KeyValuePair(ProductType.CREATE_TIME, TimeService.currentTimeMillisOfBytes()));
		contents.add(new KeyValuePair(ProductType.CONTENT, "回馈老用户，免费赠送"));
		
		Product product = new Product(contents);
		return product;
	}
}
