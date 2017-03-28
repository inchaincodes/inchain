package org.inchain.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Definition;
import org.inchain.core.Product;
import org.inchain.core.ProductKeyValue;
import org.inchain.core.TimeService;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.utils.Hex;
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
			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testAddProdudct() throws InterruptedException, ExecutionException, TimeoutException {
		
		Product product = createProduct();
		log.info("{}", Hex.encode(product.serialize()));
		assert(product != null);
		
		ProductTransaction tx = new ProductTransaction(network, product);
		
		accountKit.decryptWallet("inchain123", Definition.TX_VERIFY_TR);
		
		tx.sign(accountKit.getDefaultAccount());
		tx.verify();
		tx.verifyScript();
		
		log.info("tx {}", tx.getHash());
		log.info("tx size is {}", tx.baseSerialize().length);
		
		tx = new ProductTransaction(network, tx.baseSerialize());

		tx.verify();
		tx.verifyScript();
		
		log.info("tx {}", tx.getHash());
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
