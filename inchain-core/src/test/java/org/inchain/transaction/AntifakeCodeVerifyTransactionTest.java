package org.inchain.transaction;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.core.BroadcastResult;
import org.inchain.core.VerifyAntifakeCodeResult;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.network.NetworkParams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 防伪码验证测试
 * @author ln
 *
 */
public class AntifakeCodeVerifyTransactionTest extends TestNetBaseTestCase {

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
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testVerifyAntifakeCode() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		
		log.info("best block height {}", network.getBestBlockHeight());
		
		String antifakeCodeContent = "xFdeHFv8RsHn6PHYRE8JTDLwRKPRe7mcej2R5c";
		VerifyAntifakeCodeResult result = accountKit.verifyAntifakeCode(antifakeCodeContent);

		log.info("{}", result);
		
	}
}
