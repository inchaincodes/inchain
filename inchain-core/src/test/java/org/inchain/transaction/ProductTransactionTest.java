package org.inchain.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.core.BroadcastResult;
import org.inchain.core.KeyValuePair;
import org.inchain.core.Product;
import org.inchain.core.TimeService;
import org.inchain.core.Definition;
import org.inchain.core.Product.ProductType;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.business.ProductTransaction;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 新增商品测试
 * @author ln
 *
 */
public class ProductTransactionTest extends TestNetBaseTestCase {

	@Autowired
	private AppKit appKit;
	@Autowired
	private PeerKit peerKit;
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
	public void testAddProdudct() throws InterruptedException, ExecutionException, TimeoutException {
		
		Product product = createProduct();
		
		assert(product != null);
		
		ProductTransaction tx = new ProductTransaction(network, product);
		
		accountKit.decryptWallet("inchain123", Definition.TX_VERIFY_TR);
		
		tx.sign(accountKit.getDefaultAccount());
		tx.verify();
		tx.verifyScript();
		
		log.info("tx {}", tx);
		log.info("tx size is {}", tx.baseSerialize().length);
		
		tx = new ProductTransaction(network, tx.baseSerialize());

		tx.verify();
		tx.verifyScript();
		
		log.info("tx {}", tx);
		log.info("tx size is {}", tx.baseSerialize().length);
		
		//加入内存池
		MempoolContainer.getInstace().add(tx);
		
		//广播
		BroadcastResult result = peerKit.broadcast(tx).get();
		if(result.isSuccess()) {
			log.info("广播成功");
		} else {
			log.error("广播失败 {}", result.getMessage());
		}
		
	}
	
	private Product createProduct() {
		List<KeyValuePair> contents = new ArrayList<KeyValuePair>();
		contents.add(new KeyValuePair(ProductType.NAME, "印链-闪迪U盘"));
		contents.add(new KeyValuePair(ProductType.DESCRIPTION, "64G"));
		contents.add(new KeyValuePair(ProductType.CONTENT, "回馈老用户，免费赠送"));
		contents.add(new KeyValuePair(ProductType.PRODUCTION_DATE, "2017-02-27"));
		contents.add(new KeyValuePair(ProductType.CREATE_TIME, TimeService.currentTimeMillisOfBytes()));
		contents.add(new KeyValuePair(ProductType.CONTENT, "回馈老用户，免费赠送"));
		
		Product product = new Product(contents);
		return product;
	}
	
}
