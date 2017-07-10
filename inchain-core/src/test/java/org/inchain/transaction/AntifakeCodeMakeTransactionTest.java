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
		
		log.info("make anti code------------------------------------------------------- ", network.getBestBlockHeight());
		BroadcastMakeAntifakeCodeResult result = null;
		for(int i = 0; i<1; i++) {
			String productTx = "ec0618332cd5330f82fb4d28b931b9e88c74db802b2ff7fff9bf84fa7f13187f";
			Coin reward = Coin.ZERO;
			result = accountKit.makeAntifakeCode(productTx, reward, "inchain123");
			log.info("code :" + (i+1));
			log.info("{}--{}--{}", Base58.encode(result.getAntifakeCode().getAntifakeCode()), result.getAntifakeCode().getVerifyCode(),result.getAntifakeCode().base58Encode());

			log.info("{}","");
		}

		log.info("make anti code end-----------------------------------------------", result);
		

		
	}
}
