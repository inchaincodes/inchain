package org.inchain.net;

import java.util.Random;

import org.inchain.UnitBaseTestCase;
import org.inchain.kits.PeerKit;
import org.inchain.message.PingMessage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PeerGroupTest2 extends UnitBaseTestCase {

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
