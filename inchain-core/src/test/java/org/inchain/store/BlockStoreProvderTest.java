package org.inchain.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.inchain.BaseTestCase;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
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
		BlockStore bestBlock = storeProvider.getBestBlock();
		if(bestBlock != null) {
			storeProvider.delete(bestBlock.getBlock().getHash().getBytes());
		}
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
		
		Block block = new Block(network);
		
		block.setPreHash(Sha256Hash.wrap(Hex.decode("0d438118c28d4b3644779d18032db8af3a5dfac6d2d004212e90473380e0cb62")));
		block.setHeight(1);
		block.setTime(1478164677l);

		//交易列表
		List<Transaction> txs = new ArrayList<Transaction>();
		
		//coinbase
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(TransactionDefinition.VERSION);
		coinBaseTx.setType(TransactionDefinition.TYPE_COINBASE);
		coinBaseTx.setLockTime(1478164688l);
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		coinBaseTx.addOutput(Coin.valueOf(100l), Address.fromBase58(network, "uNdmAUpGqrNYgguFQT97eByXb6v1CUtcHR"));
		coinBaseTx.verfify();
		coinBaseTx.verfifyScript();
		
		txs.add(coinBaseTx);
		
		block.setTxs(txs);
		block.setTxCount(txs.size());
		
		Sha256Hash merkleHash = block.buildMerkleHash();
		block.setMerkleHash(merkleHash);
		
		testBlock.setBlock(block);
		
		System.out.println(testBlock.getBlock().getHash());
		
		storeProvider.saveBlock(testBlock);
		
	}
	
	@Test
	public void testGetBlockHeader() {
		Sha256Hash hash = Sha256Hash.wrap(Hex.decode("23971206eec0ac170be2a9371867442b15cd422729c5265e3c580c2c51b36ea7"));
		BlockHeaderStore header = storeProvider.getHeader(hash.getBytes());
		assertNotNull(header);
		
		assertEquals("995a76201ae69cf05f58519f7c858829442d4d3c05029826c9c06e206795e447", Hex.encode(header.getBlockHeader().getMerkleHash().getBytes()));
		
		assertEquals("0d438118c28d4b3644779d18032db8af3a5dfac6d2d004212e90473380e0cb62", Hex.encode(header.getBlockHeader().getPreHash().getBytes()));
		
		List<Sha256Hash> txHashs = header.getBlockHeader().getTxHashs();
		
		assertEquals(1, txHashs.size());
		
		Transaction tx = storeProvider.getTransaction(txHashs.get(0).getBytes()).getTransaction();
		assertNotNull(tx);
		assertEquals("995a76201ae69cf05f58519f7c858829442d4d3c05029826c9c06e206795e447", tx.getHash().toString());
	}
	
	@Test
	public void testGetBlockHeaderByTx() {
		TransactionStore tx = storeProvider.getTransaction(Hex.decode("995a76201ae69cf05f58519f7c858829442d4d3c05029826c9c06e206795e447"));
		assertNotNull(tx);
		assertEquals("995a76201ae69cf05f58519f7c858829442d4d3c05029826c9c06e206795e447", tx.getTransaction().getHash().toString());

		BlockHeaderStore header = storeProvider.getHeaderByHeight(tx.getHeight());
		assertNotNull(header);
		assertEquals("995a76201ae69cf05f58519f7c858829442d4d3c05029826c9c06e206795e447", Hex.encode(header.getBlockHeader().getMerkleHash().getBytes()));
	}
	
	@Test
	public void testGetBlock() {

		BlockStore blockStore = storeProvider.getBlock(Hex.decode("23971206eec0ac170be2a9371867442b15cd422729c5265e3c580c2c51b36ea7"));
		
		assertNotNull(blockStore);
	}
	
	@Test
	public void testGetBlockByHeight() {

		BlockStore blockStore = storeProvider.getBlockByHeight(1l);
		
		assertEquals("23971206eec0ac170be2a9371867442b15cd422729c5265e3c580c2c51b36ea7", Hex.encode(blockStore.getBlock().getHash().getBytes()));
		assertNotNull(blockStore);
	}
	
	@Test
	public void testGetBestBlock() {

		BlockStore blockStore = storeProvider.getBestBlock();
		
		assertEquals("23971206eec0ac170be2a9371867442b15cd422729c5265e3c580c2c51b36ea7", Hex.encode(blockStore.getBlock().getHash().getBytes()));
		assertNotNull(blockStore);
	}
}
