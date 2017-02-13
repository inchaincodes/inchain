package org.inchain.block;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
import org.inchain.network.NetworkParams;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.LocalTransactionSigner;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.CreditTransaction;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
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
			
			//货币存放账户
			ECKey key = ECKey.fromPrivate(new BigInteger("78448139577019774065854737654668714060779258923948372860518458045054970376981"));
			Address address = AccountTool.newAddress(network, key);
			
//			Address address = Address.fromBase58(network, "toMahRViJBfKJ49QzYymKVb6JqNCLxTPN4");
	
			System.out.println("==========================");
			System.out.println(address.getBase58());
			System.out.println("==========================");
			
			coinBaseTx.addOutput(Coin.MAX, 0, address);
			coinBaseTx.verfifyScript();
			
			txs.add(coinBaseTx);
			
			//货币存放账户
			ECKey key1 = ECKey.fromPrivate(new BigInteger("107447043214236960233284625627849494703774370863291752674105079799661386075456"));
			Address address1 = AccountTool.newAddress(network, key1);
			
			//把永久锁定的转入一个账号内
			Transaction tx = new Transaction(network);
			tx.setVersion(TransactionDefinition.VERSION);
			tx.setType(TransactionDefinition.TYPE_PAY);
			tx.setLockTime(0l);
			input = new TransactionInput((TransactionOutput)coinBaseTx.getOutput(0));
			tx.addInput(input);
			tx.addOutput(Coin.valueOf(400000000).multiply(Coin.COIN.value), -1, address1);
			tx.addOutput(Coin.valueOf(600000000).multiply(Coin.COIN.value), 0, address);
			//创建一个输入的空签名
			input.setScriptSig(ScriptBuilder.createInputScript(null, key));
			//签名交易
			final LocalTransactionSigner signer = new LocalTransactionSigner();
			signer.signInputs(tx, key);
//			tx.verfify(); 
			tx.verfifyScript();
			
			txs.add(tx);
			
			tx = new Transaction(network, tx.baseSerialize());
			tx.verfifyScript();
			
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
				
				key = ECKey.fromPrivate(pri);
				address = AccountTool.newAddress(network, key);

				System.out.println("==========================");
				System.out.println(address.getBase58());
				System.out.println("==========================");
				
				Account account = new Account(network);
				account.setAddress(address);
				account.setEcKey(key);
				
				//注册账户授予信用积分
				CreditTransaction creditTx = new CreditTransaction(network);
				creditTx.setCredit(999999l);
				creditTx.sign(account);
				
				txs.add(creditTx);
				
				//注册共识账户到区块里
				RegConsensusTransaction regConsensusTransaction = new RegConsensusTransaction(network, TransactionDefinition.VERSION, System.currentTimeMillis());
				regConsensusTransaction.sign(account);
				
				regConsensusTransaction.verfify();
				regConsensusTransaction.verfifyScript();
				
				txs.add(regConsensusTransaction);
			}
			
			//注册创世管理帐户
			CertAccountRegisterTransaction certTx = new CertAccountRegisterTransaction(network, Hex.decode("0b010000003a42f3215a010000a35e97d7189abb8274afa8e264b106a1e1041096fd980d1f01e9878de5ba86e58db0e993bee7a791e68a80e69c89e99990e585ace58fb81304e9878de5ba86e5b882e58d97e5b2b8e58cbafd180c0389504e470d0a1a0a0000000d4948445200000050000000500803000000b9cf029f0000001974455874536f6674776172650041646f626520496d616765526561647971c9653c0000038869545874584d4c3a636f6d2e61646f62652e786d7000000000003c3f787061636b657420626567696e3d22efbbbf222069643d2257354d304d7043656869487a7265537a4e54637a6b633964223f3e203c783a786d706d65746120786d6c6e733a783d2261646f62653a6e733a6d6574612f2220783a786d70746b3d2241646f626520584d5020436f726520352e362d633133382037392e3135393832342c20323031362f30392f31342d30313a30393a30312020202020202020223e203c7264663a52444620786d6c6e733a7264663d22687474703a2f2f7777772e77332e6f72672f313939392f30322f32322d7264662d73796e7461782d6e7323223e203c7264663a4465736372697074696f6e207264663a61626f75743d222220786d6c6e733a786d704d4d3d22687474703a2f2f6e732e61646f62652e636f6d2f7861702f312e302f6d6d2f2220786d6c6e733a73745265663d22687474703a2f2f6e732e61646f62652e636f6d2f7861702f312e302f73547970652f5265736f75726365526566232220786d6c6e733a786d703d22687474703a2f2f6e732e61646f62652e636f6d2f7861702f312e302f2220786d704d4d3a4f726967696e616c446f63756d656e7449443d22786d702e6469643a63366435353139652d333762642d343630662d613136662d6263623131316565396535652220786d704d4d3a446f63756d656e7449443d22786d702e6469643a42333641414136314544434131314536394142353936384532413336433538422220786d704d4d3a496e7374616e636549443d22786d702e6969643a42333641414136304544434131314536394142353936384532413336433538422220786d703a43726561746f72546f6f6c3d2241646f62652050686f746f73686f7020434320323031352e3520284d6163696e746f736829223e203c786d704d4d3a4465726976656446726f6d2073745265663a696e7374616e636549443d22786d702e6969643a31663063653734362d373938392d616634662d396362342d393233623361313333346635222073745265663a646f63756d656e7449443d2261646f62653a646f6369643a70686f746f73686f703a36363933666334302d656463372d313165362d626534372d613738373163636365383764222f3e203c2f7264663a4465736372697074696f6e3e203c2f7264663a5244463e203c2f783a786d706d6574613e203c3f787061636b657420656e643d2272223f3ebee777fb00000180504c5445f75d4ff97c4bfa854af9794bfca546f7614ef8797cfcad6efee3d9fda946feeeeefcbca9f65150f54552f44152f54d51f43d53f65a4ffef2ecf54951f8796bfeebe3f43a53fca253fdcbbbfa9587fdd6c8f8827dfff4edfcab4ffdd9d6f76862f86a4df65450fb9848fbac99fdad45f98a88f66259f9897bfb9a47fa8e49f8764cf87a56fb9049f65c5afb9b51fcb499fcc2adf9804afa9352f8704cf87456f54c5bfb9548f86d4df5435cfa8b49fcb282f8744cf86f4df7684dfb9348f8724cfa8c49fc9e47fca346fb9248fc9c47fca047f7644ef6535afa8b54fcab83f7664ef98454f33254f76b57fb976afdcaacfdd6bcf33653f65964f43d5dfef0edfee6e1f9886bfa834afdb14efba18bf76b58fef1e5f33454fba584f97b5cfa9b74fdcba3feeeeafa884af9949cf76672f98574f9784cf76f57f76758fa8d74f87061fef0ebfee5cffa9059fa8949fef1edf6574ff9824af7674efb9b47f33753fca74ff65d53fba890f5485bfcc2bef97f4bf98f92f8754ffef0eefeefeefef2edae422d56000006994944415478daacd9f95f53391000f007ae50db0a54a88502dd96ca2550caa51cbaae2887a2dc15590456141094431716656dfbaf3b33495e32e92badbb3bbff7fb4932c964f2eabc36a3f575ab8c7388c8790422154989184c0d521ccd52cc89e899eba118c6d87fb9e5dc31e206c62f1893939353538f1f3f7efa575757575bdbbd7bbf3d79d2dbddddfdfbd8d8f14e6565e5dae781cbcb8a8a8abdbdbdbb13138f86861edcbf1f8f3ffcf27cda79adb93b051e826de801d8db2bbd1df206d0bb766def2e80ca7bf865faa50992774380a5bd4bd77b84de7df49e1be00d36e1293561e54990bc4aeded698f266c8ed07b01b937b6632e207a77d9029aa097e731e1e2de43f4ae7f50a05ec07f9f10f014682fa087d77d6542c484af6bd033215d5e0959b31232c43cc7044b25e4b8544234e8e13d2d9a9081a20901cfe900b08c052c2f21e009909f10af1d3d567a470b2fa4c1ab1332564642000c21582c216d3f9d100334bcf3c58d4fd5e4a516c2e18db07764c29958e0959510f04e05689698c59696968d088e6fa185475ec70945f6dbb02831aee7827a01a736f0b7e3b880e1a2e0898c7c6ccbf2006cb512f2097f5b8d09f19518200c311b351302de691f80bc4657c310ffbc87315edacb66d7cd053c3d4d00689598a791f188dc31d50b6ea417d23a02e94020d098412f1bbb6e4e3841a07542ba0a4fc8da67d8d103468979001b663846e221f31458aae67beee8750277b5076050806c47a7aa23d68e9eef5ff4853732d1c0fc3573473711d8692c6022284056622829a6b719d529f125f7f40991a0a33d0d1a2586b64dc4f58e1659826fc50e9417d7a05ac0603038e3b44ef205a43d32ae6afe7cc6dad1f9b7ebaac41098eb645e6d1f82acc4b82024e4783e5cb80357d76549403007a09b1004679c73abc408504cf8c827bd68e0e2a2bff1add8cf1f0f448969428f40b580b5b504f29a2f41f48e1bc9bb159d17356b2bf0910e886f9f4a8c04b52741abe60b9012b22dc6d70ffb4feee8ed0cce3a9ba492d0849e005dcf05f58e5620ec68ac64f9967ebda387860edee211ceec634968ca09d0f408e4274e8058f38ff238c0283f21fdb4579a9ec309166073482684bc7a00ad9a2f41d8d1e334e16d5ef387a9267c9bd6a04e08780046ac4b53803b708417d0f3d997661a411f964005ea09d74b905dc2043ec3921045306d7731e27c7c00b05d80c6024a909718094289f99497296697f0ae2a5a4e93020daf7ec4895897b000b164f924c82ecd7883041d8746e86f36bd9b085a6da502d73e0bd0ee6208cc1d4249c011fa0974c777f32682bc8b9120146905b22e26ee82a176f4fccd3a21e021682c2096180942cd17a0ddc534504938848ae082b505a059f30508355f81bc8bc129e714e827d0f46a469c94d5150910bb1801da5d4c039d60004fdbb30432cf00558d1620de7112b4da4a01fe0a07aeddef82ae4720bf34e9527f86771c8149b98071d5c534e40a41e949d0ba3469849b0668b5951284132cc02a36c09aaf08b2b6926ae0269618044f9213565be98e5083864720bf84a9c46c5608f0249fb4db4a35c284007355cc93a0d997d31db28925c187d53969f7e5124c24dc114a8fc43a00ad3e3f2f4028093ebc4092fae52aba2202fd0006258827580db00e41ebe52a402c3102b4fb7c09c2817341c32390bf5c2508274e81accf3f6da092c040e9d52138eaa4ac3e5f8058627cab08c6adbefc904e3080b512c409d7a801de1e7506ad879204e1c405f0c67cc51f4aa1509f0f9458d0008d092bd07c7809104b564fe36af8225ed0e757c5fcb1c31928091ad41e82565bf9518074e286ed8797eca34509fc87f6e17be611c81f4a74e0a213451e4a461753fb8d46b86c2ca000ad976b1ac1d5f4ab9e9e61fa5ab4bfbfff07c4078c8e8e8ebe8e3e8ae5e5247931ed217806a0f5729db7de0d227232fc76bc9327440e9040ebe5dae87a27055e01181ba961de12803b569fdf43fbaf3c2ff39e7b67672bcea0fd72fd3e172d7b7cef5942ce0458f8f4ff7e11cd96b180b1772335d6009796569c59cf6f315b07189d079dbb9d9dbbbbcdbbcd1855cd55462cf31d4d0354a0f7a7842fee8efe3bc4db54798578786f007417f0aa4f096e9fcf2f4d5962b447e0951fc73c4e08aff9dc43b0f4b798c209d77b4c98bc17000e14fd5ae9edb101f2057cf3428025beed145f40db13e065c96f3145bd9a020fc0b981ff94903aee11f8bf2464497808965ec0721322c1e25f2b7f36211a2c95904479095160390909164bc86dee01c87674fcaa1d5d2421c6026238b35becff0b889710e246c24b8962a66f8662646684e22bc628c5cae88a19a33f0418008e8f0f813207cbb40000000049454e44ae426082130839313530303130384d41355542333248334e0d053032332d3836333331303639180d68747470733a2f2f7777772e696e636861696e2e6f7267fd0b0107e9878de5ba86e58db0e993bee7a791e68a80e69c89e99990e585ace58fb8e698afe4b880e5aeb6e4bba5e58cbae59d97e993bee68a80e69cafe9a9b1e58aa8e79a84e5889be696b0e59e8be4bc81e4b89aefbc8ce585b6e4b8bbe5afbce79a84e58cbae59d97e993bee7a4bee58cbae9a1b9e79bae496e636861696e2de58db0e993bee698afe4b880e4b8aae4bba5e998b2e4bcaae4b8bae59fbae7a180e4b89ae58aa1e79a84e585ace5bc80e5b9b3e58fb0efbc8ce4b8bae7a4bee4bc9ae59084e4bc81e4b89ae38081e69cbae69e84e38081e889bae69cafe5aeb6e7ad89e68f90e4be9be59381e7898ce38081e79fa5e8af86e4baa7e69d83e4bf9de68aa4e69c8de58aa1e3808202210336dd0959d701e9204636d347305a49617e128fd7a12594a4d3985896319048d121038076589493ea0604c70ed5c587367c1bd95f953d7779eefc852ebc30501c199c0221037d1cddb7bd34cb4a72c259c310aa7ec026fb12ba382fddcd35bf3a36aa6abd1721035ed83829f6a4eb9a93604eb556f31d507738fe623a757b830c04eadf597932dec9c220d936ad91a50e918ebb8376c7335fb7d16255333ea859d64d7d4d1f309052c8f8c314728ac45421c493b920d897ce8fb92b9db25215cd87463044022010d44f0833557213474201d7da4ce01924d0f3eb0b567aab5e0c1db9657b02dd02206ee0be5594237f060a2ca2baf02f11b0336a0e13954da64b1956d6429e29341c47304502210094d17d80e82b409345125fedbfeba0ed09929e4795115854208146669c23aa5502202cface8d34928ed216e876ca5c76fd5d7e0845f0c69d12606c97fe639a7393aeac"));
			
			certTx.verfify();
			//创世块里不验证签名，因为还没有账户交易，所以不检查
//			certTx.verfifyScript();
			
			txs.add(certTx);
			
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
