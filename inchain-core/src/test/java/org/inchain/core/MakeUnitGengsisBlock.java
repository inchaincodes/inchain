package org.inchain.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.UnitBaseTestCase;
import org.inchain.account.Account;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
import org.inchain.network.NetworkParams;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.transaction.CreditTransaction;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 制作创世块
 * @author ln
 *
 */
public class MakeUnitGengsisBlock extends UnitBaseTestCase {

	@Autowired
	private NetworkParams network;
	@Autowired
	private BlockStoreProvider blockStoreProvider;

	@Test
	public void makeTestNetGengsisBlock() throws Exception {
		
		Block gengsisBlock = new Block(network);
		
		gengsisBlock.setPreHash(Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")));
		gengsisBlock.setHeight(0);
		gengsisBlock.setTime(1478070769l);

		//交易列表
		List<Transaction> txs = new ArrayList<Transaction>();
		
		//产出货币总量
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(TransactionDefinition.VERSION);
		coinBaseTx.setType(TransactionDefinition.TYPE_COINBASE);
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		//创世账户
		ECKey key = ECKey.fromPrivate(new BigInteger("67354228887878139695633819126625517515785554606767523849461500912225575561110"));
		
		Address address = AccountTool.newAddress(network, key);

		System.out.println("==========================");
		System.out.println(address.getBase58());
		System.out.println("==========================");
				
		coinBaseTx.addOutput(Coin.MAX, address);
		coinBaseTx.verfifyScript();
		
		txs.add(coinBaseTx);
		
		
		//共识账户
		BigInteger[] privateKeys = {
				new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"),
				new BigInteger("52188072277803777502738867181821197739391264777454871393545634721804630880136"),
				new BigInteger("70949774079351797875601732907368565593785330858428914876767198731857299028554"),
				new BigInteger("35876700136292794264167572101940880527972010427306166598953832406950260704243"),
				new BigInteger("22179228508617634730737242365679835550222360221837033410034564397849174416545"),
		};
		
		for (int i = 0; i < privateKeys.length; i++) {
			BigInteger pri = privateKeys[i];
			
			key = ECKey.fromPrivate(pri);
			address = AccountTool.newAddress(network, key);

			System.out.println("==========================");
			System.out.println(address.getBase58());
			System.out.println("==========================");
			
			//注册账户授予信用积分
			CreditTransaction creditTx = new CreditTransaction(network);
			creditTx.setHash160(address.getHash160());
			creditTx.setCredit(999999l);
			
			txs.add(creditTx);
			
			//注册共识账户到区块里
			RegConsensusTransaction regConsensusTransaction = new RegConsensusTransaction(network, TransactionDefinition.VERSION, System.currentTimeMillis());
			Account account = new Account(network);
			account.setAddress(address);
			account.setEcKey(key);
			regConsensusTransaction.sign(account);
			
			regConsensusTransaction.verfify();
			regConsensusTransaction.verfifyScript();
			
			txs.add(regConsensusTransaction);
		}
		
//		//注册创世管理帐户
//		CertAccountRegisterTransaction certTx = new CertAccountRegisterTransaction(network, Hex.decode("0b01000000000000000000000098ba9559d02ae15f34b0209a87377f1e59c501730100022103ebe369f63421457abbca40b3295a3980db5488ee34d56ebe8d488f1d5d301f8321022700a96f3fd3b0d082abfd8bd758502f2e7e881eeaa5c69662c8eac7ade6d4330221028b3106d4cac5218388d2249503abab63c9b5de10525d13299d0423ab6f455a402103ca686fce8b25c1dd648e5dcbed9a8c95d8d5ec28baaefdbd7af2117fc22a6286c9c1200000000000000000000000000000000000000000000000000000000000000000c3140000000000000000000000000000000000000000874630440220296e7127545692d5580fc89aa84060374fb733facb91709fc2d8591b746e4baf022040cf22ff7ca342528890d867050473d861f3266d17119e705c68b950c0ffea4e4730450221008075c85feeee35d99e83a2678919904ff0b15283c017531fd5900f47efb65a47022056419266e203ec18e12fd3d77d8c2b68477f91a5f14ffee38b6e7878968a5621ac"));
//		
//		certTx.verfify();
//		certTx.verfifyScript();
//		
//		txs.add(certTx);
		
		gengsisBlock.setTxs(txs);
		
		gengsisBlock.setTxCount(txs.size());
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		System.out.println("merkle	hash: "+merkleHash);
		
		System.out.println("block hash: "+Hex.encode(gengsisBlock.getHash().getBytes()));
		
		System.out.println(Hex.encode(gengsisBlock.baseSerialize()));
		
		BlockStore blockStore = new BlockStore(network, gengsisBlock);

		System.out.println(Hex.encode(blockStore.baseSerialize()));
		

//		Assert.assertTrue(Arrays.equals(gengsisBlock.baseSerialize(), blockStore.baseSerialize()));
		
		BlockStore gengsisBlockTemp = new BlockStore(network, blockStore.baseSerialize());
		
		Sha256Hash merkleHashTemp = gengsisBlockTemp.getBlock().buildMerkleHash();
		
		System.out.println("merkle hash temp: "+merkleHashTemp);
		Assert.assertEquals(merkleHash, merkleHashTemp);
		
		blockStoreProvider.saveBlock(gengsisBlockTemp);
		
		
	}
}
