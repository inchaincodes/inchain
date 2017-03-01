package org.inchain.transaction;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.LocalTransactionSigner;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.utils.Hex;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.validator.ValidatorResult;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 防伪码验证测试
 * @author ln
 *
 */
public class AntifakeCodeVerifyTransactionTest extends TestNetBaseTestCase {

	@Autowired
	private NetworkParams network;
	@Autowired
	private AppKit appKit;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private AccountKit accountKit;
	@Autowired
	private TransactionValidator transactionValidator;
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
	@PostConstruct
	public void init() throws IOException {
		appKit.start();
	}
	
	@Before
	public void waitAminute() {
		try {
			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testVerifyAntifakeCode() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		
		log.info("best block height {}", network.getBestBlockHeight());
		
		//防伪码产生的交易hash
		Sha256Hash codeTx = Sha256Hash.wrap("da812a18e2e7a707c9267fa56cd309df8f2bd479bcc6885e069a9e131afff8b1");
		
		//判断验证码是否存在
		TransactionStore txStore = blockStoreProvider.getTransaction(codeTx.getBytes());
		
		//必须存在
		assert(txStore != null);
		
		Transaction fromTx = txStore.getTransaction();
		
		//交易类型必须是防伪码生成交易
		assert(fromTx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE);
		
		AntifakeCodeMakeTransaction codeMakeTx = (AntifakeCodeMakeTransaction) fromTx;
		
		//认证商家对防伪码的签名
		Account account = accountKit.getDefaultAccount();
		account.decryptionTr("inchain123");
		
		Sha256Hash antifakeHash = codeMakeTx.getAntifakeHash();
		byte[][] signs = LocalTransactionSigner.signHash(account, antifakeHash);
		
		Script inputSig = ScriptBuilder.createAntifakeInputScript(account.getAccountTransaction().getHash(), signs);
		
		TransactionInput input = new TransactionInput((TransactionOutput) codeMakeTx.getOutput(0));
		input.setScriptSig(inputSig);
		
		AntifakeCodeVerifyTransaction tx = new AntifakeCodeVerifyTransaction(network, input);
		
		//验证账户，不能是认证账户

		Account systemAccount = accountKit.getSystemAccount();
		assert(systemAccount != null);
		
		//添加奖励输出
		Coin rewardCoin = codeMakeTx.getRewardCoin();
		if(rewardCoin != null && rewardCoin.isGreaterThan(Coin.ZERO)) {
			tx.addOutput(rewardCoin, systemAccount.getAddress());
		}
		
		tx.sign(systemAccount);

		byte[] txContent = tx.baseSerialize();
		
		log.info("tx size {}", txContent.length);
		log.info("tx {}", tx);
		log.info("tx content : {}", Hex.encode(tx.baseSerialize()));
		
		tx.verfify();
		tx.verfifyScript();
		
		//验证交易是否合法
		ValidatorResult<TransactionValidatorResult> rs = transactionValidator.valDo(tx, null);
		if(!rs.getResult().isSuccess()) {
			throw new VerificationException(rs.getResult().getMessage());
		}
		
//		tx = new AntifakeCodeMakeTransaction(network, Hex.decode("160100000001def02ea3e5b87e22d4d7e03c49686dd994a117f09fcbce7e739bd4c11fa9c6d700000000c8473044022045b9006d6316d5d5a4a169bc11eb1a52bf9f73dc4187a46cb4cafca671e53db102207127a028882e78330e0b8953b214ee7dc81cda0769e1c82764beec12c30d083901483045022100ae2742dff777e5cd40eb7ea6c2ae66904fcc24593b2d50c5908fd43102c9966e022018e4fb1596bb1eebd11b873562397249bed82e7b1387e0e2f1fc6d374afb0a6c01c2209c890b3c4a3dd4599b445e3d40034bc5c289df7e37e391f2648124f2c273d60814a35e97d7189abb8274afa8e264b106a1e1041096ffffffff0200e1f5050000000000000000000000003a75c314a35e97d7189abb8274afa8e264b106a1e10410968820ce2efc6f9402ea928c3e8adbfbd78463342fb93f3fe97ae59055b1a151a18802ac002fafcee800000000000000000000001975c314a35e97d7189abb8274afa8e264b106a1e104109688ac6b11ec835a0100000000000000000000c8c2209c890b3c4a3dd4599b445e3d40034bc5c289df7e37e391f2648124f2c273d608c314a35e97d7189abb8274afa8e264b106a1e104109688463044022020901104daeb6dd20045452d23d2ca0abb87fd6d9146f80eb52d28a5e3f035890220472ab477376165fd49c7c75d4d5d3e60bce8893129c315e5fb245644740e20ca4630440220556e26db1212f0ebb3d7d55f8c756297145665d5d05d4ae9b954c67141e6c47302207d5e805bb2985202efce870b522bc1a2a3e42abb3027cd8f351e206a977a8c38ac58259ec8cfa0689899a68d449c420b85539c21cbb298a6299c8ec94875f5b76c0074c0cc1af5b802"));

		ValidatorResult<TransactionValidatorResult> valResult = transactionValidator.valDo(tx);
		log.info("val result : {}", valResult.getResult());
		
		assert(valResult.getResult().isSuccess());
		

		//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
		boolean success = MempoolContainer.getInstace().add(tx);
		
		assert(success);
		
		BroadcastResult result = peerKit.broadcast(tx).get();
		log.info("broadcast result : {}", result);
		//等待广播回应
		if(result.isSuccess()) {
			//更新交易记录
			transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
		}
		
		assert(result.isSuccess());
	}
}
