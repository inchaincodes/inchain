package org.inchain.transaction;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.core.BroadcastMakeAntifakeCodeResult;
import org.inchain.core.Coin;
import org.inchain.core.exception.VerificationException;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Base58;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 防伪码生产交易测试
 * @author ln
 *
 */
public class AntifakeCodeMakeTransactionTest extends TestNetBaseTestCase {

	@Autowired
	private NetworkParams network;
	@Autowired
	private AppKit appKit;
	@Autowired
	private AccountKit accountKit;
	
	@PostConstruct
	public void init() throws IOException {
		appKit.start();
	}
	
	@Before
	public void waitAminute() {
		try {
			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 生产防伪码，调用accountKit里面封装好的方法
	 * @throws IOException 
	 * @throws VerificationException 
	 */
	@Test
	public void makeAntifakeCode() throws VerificationException, IOException {
		
		log.info("best block height {}", network.getBestBlockHeight());
		
		String productTx = "6f761f487bdc557bd244c9e32499be1bd89401b949987aa1f69ec905508ca5a8";
//		Coin reward = Coin.COIN.multiply(3).div(2);
		Coin reward = Coin.ZERO;
		BroadcastMakeAntifakeCodeResult result = accountKit.makeAntifakeCode(productTx, reward, "inchain123");
		
		log.info("broadcast result {}", result);
		
		log.info("antifake code is : {}, verify code is {}", Base58.encode(result.getAntifakeCode().getAntifakeCode()), result.getAntifakeCode().getVerifyCode());
		log.info("antifake base58 code is : {}", result.getAntifakeCode().base58Encode());
		
	}
}
