package org.inchain.client;

import org.inchain.account.Address;
import org.inchain.network.NetworkParams;
import org.inchain.network.TestNetworkParams;
import org.inchain.utils.Hex;

/**
 * Test main class
 * @author ln
 *
 */
public class Daemon {

	public static void main(String[] args) {
		//TODO
		NetworkParams network = new TestNetworkParams();
		Long a = Long.parseLong("12171");
		System.out.println(new Address(network,Hex.decode("481e05c83604abf86cca029c4260674c45c85c58")).getBase58());
	}
}
