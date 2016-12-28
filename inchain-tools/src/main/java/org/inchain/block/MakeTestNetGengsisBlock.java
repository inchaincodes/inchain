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
			gengsisBlock.setTxCount(1);
	
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
			
			gengsisBlock.setTxs(txs);
			
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
