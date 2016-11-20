package org.inchain.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParameters;
import org.inchain.network.TestNetworkParameters;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.iq80.leveldb.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BlockStoreProvderTest {

	private NetworkParameters network;
	private BlockStoreProvider storeProvider;
	
	@Before
	public void init() throws IOException {
		network = TestNetworkParameters.get();
		
		//清空目录
		FileUtils.deleteDirectoryContents(new File(Configure.DATA_BLOCK));
		
		storeProvider = new BlockStoreProvider(Configure.DATA_BLOCK, network);
		
		//保存创始块
		storeProvider.saveBlock(network.getGengsisBlock());
		testSave();
	}
	
	@After
	public void close() throws IOException {
		storeProvider.close();
	}
	
	@Test
	public void testInit() throws Exception {
		assertNotNull(network);
		assertNotNull(storeProvider);
	}
	
	public void testSave() throws IOException {
		
		BlockStore testBlock = new BlockStore(network);
		
		testBlock.setPreHash(Sha256Hash.wrap(Hex.decode("59a03c5f24966e6b438e7f1d699d240fa74329f58ad10f992780b796e9e39b73")));
		testBlock.setHeight(1);
		testBlock.setTime(1478164677l);

		//交易列表
		List<TransactionStore> txs = new ArrayList<TransactionStore>();
		
		//coinbase
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(Transaction.VERSION);
		coinBaseTx.setType(Transaction.TYPE_COINBASE);
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
		Sha256Hash hash = Sha256Hash.wrap(Hex.decode("200babcc53e27f835774e221e68677481569f3732263f81bb454ac0edb8083cb"));
		BlockHeaderStore header = storeProvider.getHeader(hash.getBytes());
		assertNotNull(header);
		
		assertEquals("e20dacb0a8e2ac580120cf4db083533e6f9c2871b990ce07fa8ad84837850385", Hex.encode(header.getMerkleHash().getBytes()));
		
		assertEquals("59a03c5f24966e6b438e7f1d699d240fa74329f58ad10f992780b796e9e39b73", Hex.encode(header.getPreHash().getBytes()));
		
		List<Sha256Hash> txHashs = header.getTxHashs();
		
		assertEquals(1, txHashs.size());
		
		TransactionStore tx = storeProvider.getTransaction(txHashs.get(0).getBytes());
		assertNotNull(tx);
		assertEquals("e20dacb0a8e2ac580120cf4db083533e6f9c2871b990ce07fa8ad84837850385", Hex.encode(tx.getKey()));
	}
	
	@Test
	public void testGetBlockHeaderByTx() {
		TransactionStore tx = storeProvider.getTransaction(Hex.decode("e20dacb0a8e2ac580120cf4db083533e6f9c2871b990ce07fa8ad84837850385"));
		assertNotNull(tx);
		assertEquals("e20dacb0a8e2ac580120cf4db083533e6f9c2871b990ce07fa8ad84837850385", Hex.encode(tx.getKey()));

		BlockHeaderStore header = storeProvider.getHeaderByHeight(tx.getHeight());
		assertNotNull(header);
		assertEquals("e20dacb0a8e2ac580120cf4db083533e6f9c2871b990ce07fa8ad84837850385", Hex.encode(header.getMerkleHash().getBytes()));
	}
	
	@Test
	public void testGetBlock() {

		BlockStore blockStore = storeProvider.getBlock(Hex.decode("200babcc53e27f835774e221e68677481569f3732263f81bb454ac0edb8083cb"));
		
		assertNotNull(blockStore);
	}
	
	@Test
	public void testGetBlockByHeight() {

		BlockStore blockStore = storeProvider.getBlockByHeight(1l);
		
		assertEquals("200babcc53e27f835774e221e68677481569f3732263f81bb454ac0edb8083cb", Hex.encode(blockStore.getHash().getBytes()));
		assertNotNull(blockStore);
	}
	
	@Test
	public void testGetBestBlock() {

		BlockStore blockStore = storeProvider.getBestBlock();
		
		assertEquals("200babcc53e27f835774e221e68677481569f3732263f81bb454ac0edb8083cb", Hex.encode(blockStore.getHash().getBytes()));
		assertNotNull(blockStore);
	}
}
