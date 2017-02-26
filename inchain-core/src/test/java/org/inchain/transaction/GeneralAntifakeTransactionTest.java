package org.inchain.transaction;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.core.BroadcastResult;
import org.inchain.core.KeyValuePair;
import org.inchain.core.Product;
import org.inchain.core.Product.ProductType;
import org.inchain.core.TimeService;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.utils.RandomUtil;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
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
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private TransactionValidator transactionValidator;
	
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
		
		long nonce = RandomUtil.randomLong();
		long password = RandomUtil.randomLong();
		
		GeneralAntifakeTransaction tx = new GeneralAntifakeTransaction(network, product, nonce, password);
		tx.makeSign(account);
		
		//不能广播
//		tx.verfify();
//		tx.verfifyScript();
		
		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= antifake has {} ========", tx.getAntifakeHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
		
		tx = new GeneralAntifakeTransaction(network, tx.baseSerialize());
		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= antifake has {} ========", tx.getAntifakeHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
		
		tx.sign(account);
		
		tx.verfify();
		tx.verfifyScript();
		
		TransactionValidatorResult valResult = transactionValidator.valDo(tx).getResult();
		log.info("val result : {}" , valResult);
		
		MempoolContainerMap.getInstace().add(tx);
		
		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= antifake has {} ========", tx.getAntifakeHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
		
		BroadcastResult result = peerKit.broadcast(tx).get();
		log.info("result {}", result);
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
