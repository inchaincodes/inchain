package org.inchain.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.math.BigInteger;

import org.inchain.BaseTestCase;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ChainstateStoreProviderTest extends BaseTestCase {

	@Autowired
	private NetworkParams network;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	
	@Before
	public void init() throws IOException {
	}
	
	@After
	public void close() throws IOException {
	}
	
	@Test
	public void testInit() throws Exception {
		assertNotNull(network);
		assertNotNull(chainstateStoreProvider);
	}
	
	@Test
	public void testGetCredit() throws Exception {
		ECKey key = ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"));
		Address address = AccountTool.newAddress(network, key);
		byte[] infos = chainstateStoreProvider.getBytes(address.getHash160());
		
		long credit = Utils.readUint32BE(infos, 0);
		
		assertEquals(999999l, credit);
		
		assertEquals(1, infos[41]);
		
		long blanace = Utils.readUint32BE(infos, 4);
		assertEquals(0, blanace);
	}
}
