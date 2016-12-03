package org.inchain.net;

import org.inchain.BaseTestCase;
import org.inchain.kits.PeerKit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PeerGroupTest extends BaseTestCase {

	@Autowired
	private PeerKit peerKit;
	
	@Test
	public void test() {
		
		peerKit.startSyn();
		
//		try {
//			Thread.sleep(10000l);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		peerKit.broadcastTransaction(new Transaction());
	}
}
