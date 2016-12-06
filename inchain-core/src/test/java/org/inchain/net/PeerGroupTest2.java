package org.inchain.net;

import java.util.Random;

import org.inchain.BaseTestCase;
import org.inchain.kits.PeerKit;
import org.inchain.message.PingMessage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PeerGroupTest2 extends BaseTestCase {

	@Autowired
	private PeerKit peerKit;
	
	@Test
	public void test() {
		
		while(true) {
			try {
				Thread.sleep(8000l);
				peerKit.broadcastMessage(new PingMessage(new Random().nextLong()));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
