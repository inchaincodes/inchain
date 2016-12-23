package org.inchain.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.inchain.BaseTestCase;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.ScriptBuilder;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BlockStoreProvderTest extends BaseTestCase {

	@Autowired
	private NetworkParams network;
	@Autowired
	private BlockStoreProvider storeProvider;
	
	@Before
	public void init() throws IOException {
		storeProvider.delete(storeProvider.getBestBlock().getHash().getBytes());
		//保存创始块
		storeProvider.saveBlock(network.getGengsisBlock());
		testSave();
	}
	
	@After
	public void close() throws IOException {
	}
	
	@Test
	public void testInit() throws Exception {
		assertNotNull(network);
		assertNotNull(storeProvider);
	}
	
	public void testSave() throws IOException {
		
		BlockStore testBlock = new BlockStore(network);
		
		testBlock.setPreHash(Sha256Hash.wrap(Hex.decode("f388da4f984346ea964f3e758aa405d97810d2283ccc265e1ca1574604367e28")));
		testBlock.setHeight(1);
		testBlock.setTime(1478164677l);

		//交易列表
		List<TransactionStore> txs = new ArrayList<TransactionStore>();
		
		//coinbase
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(TransactionDefinition.VERSION);
		coinBaseTx.setType(TransactionDefinition.TYPE_COINBASE);
		coinBaseTx.setLockTime(1478164688l);
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		coinBaseTx.addOutput(Coin.valueOf(100l), Address.fromBase58(network, "ThYf64mTNLSCKhEW1KVyVkAECRanhWeJC"));
		coinBaseTx.verfify();
		coinBaseTx.verfifyScript();
		
		txs.add(new TransactionStore(network, coinBaseTx, testBlock.getHeight(), 0));
		
		testBlock.setTxs(txs);
		testBlock.setTxCount(txs.size());
		
		Sha256Hash merkleHash = testBlock.buildMerkleHash();
		testBlock.setMerkleHash(merkleHash);
		
		System.out.println(testBlock.getHash());
		
		storeProvider.saveBlock(testBlock);
		
	}
	
	@Test
	public void testGetBlockHeader() {
		Sha256Hash hash = Sha256Hash.wrap(Hex.decode("dc806dcad31d0410fdc10e6ae5869e24da01874c48cd23f228f9822dd77bb2c5"));
		BlockHeaderStore header = storeProvider.getHeader(hash.getBytes());
		assertNotNull(header);
		
		assertEquals("625617ae421a0ce77b5e32c34f183c60d3cf3fc05fba0d7ca8b77a57db6e2a24", Hex.encode(header.getMerkleHash().getBytes()));
		
		assertEquals("f388da4f984346ea964f3e758aa405d97810d2283ccc265e1ca1574604367e28", Hex.encode(header.getPreHash().getBytes()));
		
		List<Sha256Hash> txHashs = header.getTxHashs();
		
		assertEquals(1, txHashs.size());
		
		TransactionStore tx = storeProvider.getTransaction(txHashs.get(0).getBytes());
		assertNotNull(tx);
		assertEquals("625617ae421a0ce77b5e32c34f183c60d3cf3fc05fba0d7ca8b77a57db6e2a24", Hex.encode(tx.getKey()));
	}
	
	@Test
	public void testGetBlockHeaderByTx() {
		TransactionStore tx = storeProvider.getTransaction(Hex.decode("625617ae421a0ce77b5e32c34f183c60d3cf3fc05fba0d7ca8b77a57db6e2a24"));
		assertNotNull(tx);
		assertEquals("625617ae421a0ce77b5e32c34f183c60d3cf3fc05fba0d7ca8b77a57db6e2a24", Hex.encode(tx.getKey()));

		BlockHeaderStore header = storeProvider.getHeaderByHeight(tx.getHeight());
		assertNotNull(header);
		assertEquals("625617ae421a0ce77b5e32c34f183c60d3cf3fc05fba0d7ca8b77a57db6e2a24", Hex.encode(header.getMerkleHash().getBytes()));
	}
	
	@Test
	public void testGetBlock() {

		BlockStore blockStore = storeProvider.getBlock(Hex.decode("dc806dcad31d0410fdc10e6ae5869e24da01874c48cd23f228f9822dd77bb2c5"));
		
		assertNotNull(blockStore);
	}
	
	@Test
	public void testGetBlockByHeight() {

		BlockStore blockStore = storeProvider.getBlockByHeight(1l);
		
		assertEquals("dc806dcad31d0410fdc10e6ae5869e24da01874c48cd23f228f9822dd77bb2c5", Hex.encode(blockStore.getHash().getBytes()));
		assertNotNull(blockStore);
	}
	
	@Test
	public void testGetBestBlock() {

		BlockStore blockStore = storeProvider.getBestBlock();
		
		assertEquals("dc806dcad31d0410fdc10e6ae5869e24da01874c48cd23f228f9822dd77bb2c5", Hex.encode(blockStore.getHash().getBytes()));
		assertNotNull(blockStore);
	}
}
