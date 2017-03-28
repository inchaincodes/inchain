package org.inchain.transaction;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Product;
import org.inchain.core.ProductKeyValue;
import org.inchain.core.TimeService;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
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
//		Product product = createProduct();
		
		//我的账户
		Account account = accountKit.getDefaultAccount();
		account.decryptionTr("inchain123");
		
		long nonce = RandomUtil.randomLong();
		long password = RandomUtil.randomLong();
		
		Sha256Hash productTx = Sha256Hash.wrap("6cb7f57548c98e9c29a698b2cb219c53850b429c448da6999868a0cfc89e2558");
		
		GeneralAntifakeTransaction tx = new GeneralAntifakeTransaction(network, productTx, nonce, password);
		tx.makeSign(account);
		
		//不能广播
//		tx.verfify();
//		tx.verfifyScript();
		
		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= antifake has {} ========", tx.getAntifakeHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
		
//		tx = new GeneralAntifakeTransaction(network, tx.baseSerialize());
		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= antifake has {} ========", tx.getAntifakeHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
		
		Account systemAccount = accountKit.getSystemAccount();
		
		assert(systemAccount != null);
		
		tx.sign(systemAccount);
		
		tx.verify();
		tx.verifyScript();
		
		TransactionValidatorResult valResult = transactionValidator.valDo(tx).getResult();
		log.info("val result : {}" , valResult);
		
		MempoolContainer.getInstace().add(tx);
		
		log.info("tx : {}", tx);
		log.info("========= has {} ========", tx.getHash());
		log.info("========= antifake has {} ========", tx.getAntifakeHash());
		log.info("========= size {} ========", tx.baseSerialize().length);
		
		BroadcastResult result = peerKit.broadcast(tx).get();
		log.info("result {}", result);
	}

	private Product createProduct() {
		List<ProductKeyValue> contents = new ArrayList<ProductKeyValue>();
		contents.add(new ProductKeyValue("name", "名称", "印链-闪迪U盘"));
		contents.add(new ProductKeyValue("description", "描述", "32G"));
		contents.add(new ProductKeyValue("content", "详情", "回馈老用户，免费赠送"));
		contents.add(new ProductKeyValue("productionDate", "生产日期", "2017-02-23"));
		contents.add(new ProductKeyValue("createTime", "创建时间", TimeService.currentTimeMillisOfBytes()));
		
		Product product = new Product(contents);
		return product;
	}
}
