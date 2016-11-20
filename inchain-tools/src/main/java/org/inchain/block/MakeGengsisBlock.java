package org.inchain.block;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NodeSeedManager;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.network.TestNetworkParameters;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.RegisterTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 制作创世块
 * @author ln
 *
 */
public class MakeGengsisBlock {

	public static void main(String[] args) throws Exception {
		makeTestNetGengsisBlock();
	}

	private static void makeTestNetGengsisBlock() throws Exception {
		
		SeedManager seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", 6888), true, 25000));
		
		NetworkParameters network = new TestNetworkParameters(seedManager, 8888);

		PeerKit peerKit = new PeerKit(network);
		peerKit.startSyn();
		
		String mgpw = "123456";
		String trpw = "654321";
		
		AccountKit accountKit = new AccountKit(network, peerKit);
		try {
			Thread.sleep(2000l);
			if(accountKit.getAccountList().isEmpty()) {
				accountKit.createNewAccount(mgpw, trpw);
			}
		} finally {
			accountKit.close();
			peerKit.stop();
		}
		
		Account account = accountKit.getAccountList().get(0);
		
		RegisterTransaction tx = new RegisterTransaction(network, account);
		//根据密码计算出私匙
		ECKey seedPri = ECKey.fromPublicOnly(account.getPriSeed());
		byte[] seedPribs = seedPri.getPubKey(false);
		
		tx.calculateSignature(ECKey.fromPrivate(AccountTool.genPrivKey1(seedPribs, mgpw.getBytes())), 
				ECKey.fromPrivate(AccountTool.genPrivKey2(seedPribs, mgpw.getBytes())));
		
		tx.verfifyScript();
		
		//序列化和反序列化
		byte[] txContent = tx.baseSerialize();
		
		System.out.println(Hex.encode(txContent));
		
		RegisterTransaction rtx = new RegisterTransaction(network, txContent);
		System.out.println(rtx.getAccount().getAddress().getHash160AsHex());
		
		rtx.verfify();
		rtx.verfifyScript();
		
		
		
		BlockStore gengsisBlock = new BlockStore(network);
		
		gengsisBlock.setPreHash(Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")));
		gengsisBlock.setHeight(0);
		gengsisBlock.setTime(1478070769l);
		gengsisBlock.setTxCount(1);

		//交易列表
		List<TransactionStore> txs = new ArrayList<TransactionStore>();
		
		//产出货币总量
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(Transaction.VERSION);
		coinBaseTx.setType(Transaction.TYPE_COINBASE);
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		coinBaseTx.addOutput(Coin.valueOf(100000000l), Address.fromBase58(network, "ThYf64mTNLSCKhEW1KVyVkAECRanhWeJC"));
		coinBaseTx.verfify();
		coinBaseTx.verfifyScript();
		
		txs.add(new TransactionStore(network, coinBaseTx));
		
		//注册创世帐户
		RegisterTransaction regTx = new RegisterTransaction(network, Hex.decode("0101000000010100a546304402207016a9642ba5bc3a13b5f7762e92936a50eb07fe1d8fe9193b714d07ab3f4c260220782d4ad2d83f983fd4f658add79d1bdab1329541514aff4275b601a286735a76473045022100afdae4c478ba213a0a9ae064d3dc687e13f4f7e559b3cd325c36861ef36a4cf80220055bf81e9db9a3d5d04453390a2b0471f8c7093e0842996610b101c20e120c99511424de55a2b2d32ed83c87b86a381e918ada34927b01a31424de55a2b2d32ed83c87b86a381e918ada34927b88c163210327260fbbb392bc13d9e8f2aabdf85d8c328eeec2114e37eb6d519fd1a0435cec2103cf8ca50b7711fb12a2a091ca5da5dd7493b77e342c205ebb0970469a400cd7e26721024148d14d898644570b4c44047d6687c1862cdbd0f813caf15a170550ad8c9d0621039844c6aa78fee792ce6ff793758b3c0d3c4217c6145f7c37844b5479b933d4e368ac00000000"));
		regTx.verfify();
		regTx.verfifyScript();
		
		txs.add(new TransactionStore(network, regTx));
		
		gengsisBlock.setTxs(txs);
		
		Sha256Hash merkleHash = gengsisBlock.getMerkleHash();
		System.out.println(merkleHash);
		Utils.checkState("c29b8e98c531ddf9211bbf1954885ceaf998d14c21e29edda8250fbacb2bf9f1".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		System.out.println(Hex.encode(gengsisBlock.getHash().getBytes()));
		Utils.checkState("897f3ee043572d069a0056f9ee9c3cdaa884691c1084a675fbbffa00e09c81fa".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
		System.out.println(Hex.encode(gengsisBlock.baseSerialize()));
		
	}
}
