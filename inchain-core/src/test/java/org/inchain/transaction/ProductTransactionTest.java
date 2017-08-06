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
		
		accountKit.decryptWallet("inchain123" ,null,Definition.TX_VERIFY_TR);
		
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
		log.info("tx content is {}",Hex.encode(tx.baseSerialize()));
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
		contents.add(new ProductKeyValue("name", "书名", "区块链：量子财富观"));
		contents.add(new ProductKeyValue("anthor", "主编", "韩锋、张晓玫"));
		contents.add(new ProductKeyValue("anthor1", "参编", "龚鸣、蒋昊然、张夏等"));
		contents.add(new ProductKeyValue("description", "详情", "中国版本图书馆CIP数据核字（2017）第133041号"));
		contents.add(new ProductKeyValue("publisher", "出版社", "机械工业出版社"));
		contents.add(new ProductKeyValue("publishtime", "印刷日期", "2017年7月第1版第1次印刷"));
		contents.add(new ProductKeyValue("ISBN", "ISBN", "978-7-111-57261-9"));
		contents.add(new ProductKeyValue("createTime", "创建时间", TimeService.currentTimeMillisOfBytes()));
		
		Product product = new Product(contents);
		return product;
	}
	
}
