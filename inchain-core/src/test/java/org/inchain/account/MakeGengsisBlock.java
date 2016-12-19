package org.inchain.account;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.BaseTestCase;
import org.inchain.core.Coin;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockStore;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.CreditTransaction;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 制作创世块
 * @author ln
 *
 */
public class MakeGengsisBlock extends BaseTestCase {

	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private AccountKit accountKit;

	@Test
	public void makeTestNetGengsisBlock() throws Exception {
//		
//		String mgpw = "123456";
//		String trpw = "654321";
//		
//		try {
//			Thread.sleep(2000l);
//			if(accountKit.getAccountList().isEmpty()) {
//				accountKit.createNewAccount(mgpw, trpw);
//			}
//		} finally {
//			accountKit.close();
//			peerKit.stop();
//		}
//		
//		Account account = accountKit.getAccountList().get(0);
//		
//		RegisterTransaction tx = new RegisterTransaction(network, account);
//		//根据密码计算出私匙
//		ECKey seedPri = ECKey.fromPublicOnly(account.getPriSeed());
//		byte[] seedPribs = seedPri.getPubKey(false);
//		
//		tx.calculateSignature(ECKey.fromPrivate(AccountTool.genPrivKey1(seedPribs, mgpw.getBytes())), 
//				ECKey.fromPrivate(AccountTool.genPrivKey2(seedPribs, mgpw.getBytes())));
//		
//		tx.verfifyScript();
//		
//		//序列化和反序列化
//		byte[] txContent = tx.baseSerialize();
//		
//		RegisterTransaction rtx = new RegisterTransaction(network, txContent);
//		
//		rtx.verfify();
//		rtx.verfifyScript();
//		System.out.println(Hex.encode(rtx.baseSerialize()));
//		
		
		
		BlockStore gengsisBlock = new BlockStore(network);
		
		gengsisBlock.setPreHash(Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")));
		gengsisBlock.setHeight(0);
		gengsisBlock.setTime(1478070769l);

		//交易列表
		List<TransactionStore> txs = new ArrayList<TransactionStore>();
		
		//产出货币总量
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(TransactionDefinition.VERSION);
		coinBaseTx.setType(TransactionDefinition.TYPE_COINBASE);
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		coinBaseTx.addOutput(Coin.valueOf(100000000l), Address.fromBase58(network, "ThYf64mTNLSCKhEW1KVyVkAECRanhWeJC"));
		coinBaseTx.addOutput(Coin.valueOf(100000000l), Address.fromBase58(network, "ThYf64mTNLSCKhEW1KVyVkAECRanhWeJC"));
		coinBaseTx.verfify();
		coinBaseTx.verfifyScript();
		
		txs.add(new TransactionStore(network, coinBaseTx));
		
		//注册创世帐户
//		RegisterTransaction regTx = new RegisterTransaction(network, Hex.decode("0101000000010100a547304502210082bb8fdd903133c032a00c843254c387571ec9af6372183ecf9bffc227f89cdd02207f64221d24c866ee418fc838d96543241fe676ac65bd99a16f0af6d72e2719004630440220156d0ff9e76f8834b98899881b614720bd66e9be3404fffe9364299930c48c460220118f83f1ed6f9461087a6daa991c1e4cebd572e1d5ae6e6d9ded223ffa71019051145a29d0959ae0445f837f3534304cde744a87a9e801a3145a29d0959ae0445f837f3534304cde744a87a9e888c16321035a734e4998d176a4c0b94016333e70a61a8c470ae3f854e718da34c95d960bb42102334ec39ecc5454f6899444d78f20025e68d6a0c2356c1cee84c23feb9bd658d86721021b32e71d92968bcb4c63b5adece8e1181d1a7f60c8649d35ecb9e6fc8aeeff6021029e9a848cc3c3bd112b45770d77db058a9c4d17c5fdb353d046c682b3d07f223168ac00000000"));
//		regTx.verfify();
//		regTx.verfifyScript();
//		
//		txs.add(new TransactionStore(network, regTx));
		
		//共识账户
		ECKey key = ECKey.fromPrivate(new BigInteger("61914497277584841097702477783063064420681667313180238384957944936487927892583"));
		Address address = AccountTool.newAddress(network, key);

		System.out.println("==========================");
		System.out.println(address.getBase58());
		System.out.println("==========================");
		
		//注册账户授予信用积分
		CreditTransaction creditTx = new CreditTransaction(network);
		creditTx.setHash160(address.getHash160());
		creditTx.setCredit(999999l);
		
		txs.add(new TransactionStore(network, creditTx));
		
		//注册共识账户到区块里
		byte[] hash160 = address.getHash160();
		RegConsensusTransaction regConsensusTransaction = new RegConsensusTransaction(network, TransactionDefinition.VERSION, hash160, 1478070769l);
		regConsensusTransaction.sign(key);
		
		regConsensusTransaction.verfify();
		regConsensusTransaction.verfifyScript();
		
		txs.add(new TransactionStore(network, regConsensusTransaction));
		
		gengsisBlock.setTxs(txs);
		
		gengsisBlock.setTxCount(txs.size());
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		System.out.println("merkle	hash: "+merkleHash);
		Utils.checkState("9a041ee62f2a528ef70f85b6229b668afc7a37baa3ea690d1417c257989ce66c".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		System.out.println("block hash: "+Hex.encode(gengsisBlock.getHash().getBytes()));
		Utils.checkState("f388da4f984346ea964f3e758aa405d97810d2283ccc265e1ca1574604367e28".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
		System.out.println(Hex.encode(gengsisBlock.baseSerialize()));
		
		BlockStore gengsisBlockTemp = new BlockStore(network, gengsisBlock.baseSerialize());
		
		Sha256Hash merkleHashTemp = gengsisBlockTemp.buildMerkleHash();
		
		System.out.println("merkle hash temp: "+merkleHashTemp);
		
	}
}
