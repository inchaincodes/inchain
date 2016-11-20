package org.inchain.account;

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
		
		RegisterTransaction rtx = new RegisterTransaction(network, txContent);
		
		rtx.verfify();
		rtx.verfifyScript();
		System.out.println(Hex.encode(rtx.baseSerialize()));
		
		
		
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
		RegisterTransaction regTx = new RegisterTransaction(network, Hex.decode("0101000000010100a547304502210082bb8fdd903133c032a00c843254c387571ec9af6372183ecf9bffc227f89cdd02207f64221d24c866ee418fc838d96543241fe676ac65bd99a16f0af6d72e2719004630440220156d0ff9e76f8834b98899881b614720bd66e9be3404fffe9364299930c48c460220118f83f1ed6f9461087a6daa991c1e4cebd572e1d5ae6e6d9ded223ffa71019051145a29d0959ae0445f837f3534304cde744a87a9e801a3145a29d0959ae0445f837f3534304cde744a87a9e888c16321035a734e4998d176a4c0b94016333e70a61a8c470ae3f854e718da34c95d960bb42102334ec39ecc5454f6899444d78f20025e68d6a0c2356c1cee84c23feb9bd658d86721021b32e71d92968bcb4c63b5adece8e1181d1a7f60c8649d35ecb9e6fc8aeeff6021029e9a848cc3c3bd112b45770d77db058a9c4d17c5fdb353d046c682b3d07f223168ac00000000"));
		regTx.verfify();
		regTx.verfifyScript();
		
		txs.add(new TransactionStore(network, regTx));
		
		gengsisBlock.setTxs(txs);
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		System.out.println(merkleHash);
		Utils.checkState("86c346bda7710779081dd0a89205ad72240c20da44578362d62076a683e94533".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		System.out.println(Hex.encode(gengsisBlock.getHash().getBytes()));
		Utils.checkState("4a608dbba13de948ecf1f912eb7721413b2edfe28cb13d50ce47af4b03654bb1".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
		System.out.println(Hex.encode(gengsisBlock.baseSerialize()));
		
	}
}
