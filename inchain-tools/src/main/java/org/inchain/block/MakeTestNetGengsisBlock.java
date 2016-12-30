package org.inchain.block;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockStore;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.CreditTransaction;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 制作测试网络的创世块
 * @author ln
 *
 */
public class MakeTestNetGengsisBlock {

	public static void main(String[] args) throws Exception {
		makeTestNetGengsisBlock();
	}

	private static void makeTestNetGengsisBlock() throws Exception {
		String[] xmls = new String[] { "classpath:/applicationContext-testnet.xml", "classpath:/applicationContext.xml" };

		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(xmls);
		
		springContext.start();
		
		try {
		
			NetworkParams network = springContext.getBean(NetworkParams.class);
			
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
			
			//货币存放账户
//			ECKey key = ECKey.fromPrivate(new BigInteger(""));
//			Address address = AccountTool.newAddress(network, key);
			
			Address address = Address.fromBase58(network, "toMahRViJBfKJ49QzYymKVb6JqNCLxTPN4");
	
			System.out.println("==========================");
			System.out.println(address.getBase58());
			System.out.println("==========================");
			
			coinBaseTx.addOutput(Coin.MAX, address);
			coinBaseTx.verfify();
			coinBaseTx.verfifyScript();
			
			txs.add(new TransactionStore(network, coinBaseTx));
			
			//共识账户
			BigInteger[] privateKeys = {
					new BigInteger("107447043214236960233284625627849494703774370863291752674105079799661386075456"),
					new BigInteger("43844130434206996243672984321998705117202243392831727444700563034812998002524"),
					new BigInteger("87659972937345252176535544968053194042587062778130262451373438996366876202976"),
					new BigInteger("68523560502237491324568328649263417921709098004860727210190461036838667744977"),
					new BigInteger("5677304405773443406527200295962884282966122567045677590246274870852788030021"),
					new BigInteger("1947129081671449933372347289984628389952390710594834055987045166370636787381"),
					new BigInteger("56113874067965085407617696209797239891176171878287162003209813432741449212635"),
			};
			
			for (int i = 0; i < privateKeys.length; i++) {
				BigInteger pri = privateKeys[i];
				
				ECKey key = ECKey.fromPrivate(pri);
				address = AccountTool.newAddress(network, key);

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
				RegConsensusTransaction regConsensusTransaction = new RegConsensusTransaction(network, TransactionDefinition.VERSION, hash160, System.currentTimeMillis());
				regConsensusTransaction.sign(key);
				
				regConsensusTransaction.verfify();
				regConsensusTransaction.verfifyScript();
				
				txs.add(new TransactionStore(network, regConsensusTransaction));
			}
			
			//注册创世管理帐户
			CertAccountRegisterTransaction certTx = new CertAccountRegisterTransaction(network, Hex.decode("0b010000000000000000000000209f25d38efcf3b9a1832e6480fffbe7ccb49ba41ee9878de5ba86e58db0e993bee7a791e68a80e69c89e99990e585ace58fb8022102d06b679c33838c27fd4315376618d97b000fdd467f19c21c8f7f86f4ebe8b0b321032d171ed5eca13309eb134a60eb889bff26a754141ba55c9602d94cb237c74fe602210300777b64f7b8282065524ef442b783441a09f2d45cb8f6fddf4984fc99e4a48021020172e17f39d5397f135b162b28fd54f001e7693c05136d87310839004b3b791ac9c1200000000000000000000000000000000000000000000000000000000000000000c314000000000000000000000000000000000000000087463044022046862e4536c87bf63c39dc62faea52fae94ea822047ff287eae2f42997c03eec02204e7b07d044b419c23dd18410fecceecc71accf30bb4470fe78cfe60a0eb0964d473045022100b8fdb880ae94a2d19c655fe71db609da0c3c74ea841176c348475168f5000a5a022070d22edefb14cca8813ca44f4b8cde53b7ac904662b0c61f6eb0615975bb17ccac"));
			
			certTx.verfify();
			//创世块里不验证签名
//			certTx.verfifyScript();
			
			txs.add(new TransactionStore(network, certTx));
			
			System.out.println("cert id : "+certTx.getHash());
			
			gengsisBlock.setTxs(txs);
			gengsisBlock.setTxCount(txs.size());
			
			Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
			System.out.println("the merkle hash is: "+ merkleHash);
			
			System.out.println("the block hash is: "+ Hex.encode(gengsisBlock.getHash().getBytes()));
			
			System.out.println(Hex.encode(gengsisBlock.baseSerialize()));
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			springContext.close();
			System.exit(0);
		}
	}
}
