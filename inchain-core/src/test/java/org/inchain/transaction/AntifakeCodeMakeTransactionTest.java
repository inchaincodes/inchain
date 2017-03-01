package org.inchain.transaction;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.inchain.TestNetBaseTestCase;
import org.inchain.account.Account;
import org.inchain.core.BroadcastMakeAntifakeCodeResult;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
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
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.utils.Hex;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.validator.ValidatorResult;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 防伪码生产交易测试
 * @author ln
 *
 */
public class AntifakeCodeMakeTransactionTest extends TestNetBaseTestCase {

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
	
	/**
	 * 生产防伪码，调用accountKit里面封装好的方法
	 * @throws IOException 
	 * @throws VerificationException 
	 */
	@Test
	public void makeAntifakeCode() throws VerificationException, IOException {
		
		log.info("best block height {}", network.getBestBlockHeight());
		
		//解密我的账户
		Account account = accountKit.getDefaultAccount();
		account.decryptionTr("inchain123");
				
		String productTx = "f7c8a9589ce4a7ccc585a4b1114b5b3d5f3ef04181045d125d883d21a0558a73";
		Coin reward = Coin.COIN.multiply(3);
		BroadcastMakeAntifakeCodeResult result = accountKit.makeAntifakeCode(productTx, reward);
		
		log.info("broadcast result {}", result);
		
		log.info("antifake code is : {}", result.getAntifakeCode().base58Encode());
	}
	
	public void testMakeAntifakeCode() throws InterruptedException, ExecutionException, TimeoutException {
		
		log.info("best block height {}", network.getBestBlockHeight());
		
		//我的账户
		Account account = accountKit.getDefaultAccount();
		account.decryptionTr("inchain123");
		
		Sha256Hash productTx = Sha256Hash.wrap("f7c8a9589ce4a7ccc585a4b1114b5b3d5f3ef04181045d125d883d21a0558a73");
		
		AntifakeCodeMakeTransaction tx = new AntifakeCodeMakeTransaction(network, productTx);
		
		//选择输入
		Coin money = Coin.COIN;
		List<TransactionOutput> fromOutputs = accountKit.selectNotSpentTransaction(money, account.getAddress());
		
		Coin totalInputCoin = Coin.ZERO;
		for (TransactionOutput output : fromOutputs) {
			TransactionInput input = new TransactionInput(output);
			//创建一个输入的空签名
			if(account.getAccountType() == network.getSystemAccountVersion()) {
				//普通账户的签名
				input.setScriptSig(ScriptBuilder.createInputScript(null, account.getEcKey()));
			} else {
				//认证账户的签名
				input.setScriptSig(ScriptBuilder.createCertAccountInputScript(null, account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160()));
			}
			tx.addInput(input);
			
			totalInputCoin = totalInputCoin.add(Coin.valueOf(output.getValue()));
		}
		
		//交易输出
		Sha256Hash antifakeCode = null;
		try {
			antifakeCode = tx.getAntifakeHash();
		} catch (Exception e) {
			log.error("获取防伪码出错：{}", e);
			throw new RuntimeException("获取防伪码出错：" + e.getMessage());
		}
		
		Script out = ScriptBuilder.createAntifakeOutputScript(account.getAddress().getHash160(), antifakeCode);
		tx.addOutput(money, out);
		
		//是否找零
		if(totalInputCoin.compareTo(money) > 0) {
			tx.addOutput(totalInputCoin.subtract(money), account.getAddress());
		}
		
		//签名交易
		final LocalTransactionSigner signer = new LocalTransactionSigner();
		try {
			if(account.getAccountType() == network.getSystemAccountVersion()) {
				//普通账户的签名
				signer.signInputs(tx, account.getEcKey());
			} else {
				//认证账户的签名
				signer.signCertAccountInputs(tx, account.getTrEckeys(), account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		tx.sign(account);
		//验证交易是否合法
		ValidatorResult<TransactionValidatorResult> rs = transactionValidator.valDo(tx, null);
		if(!rs.getResult().isSuccess()) {
			throw new VerificationException(rs.getResult().getMessage());
		}
		
		
		log.info("tx {}", tx);
		
		tx.verfify();
		tx.verfifyScript();
		
		byte[] txContent = tx.baseSerialize();
		
		log.info("tx size {}", txContent.length);
		
		tx = new AntifakeCodeMakeTransaction(network, txContent);

		tx.verfify();
		tx.verfifyScript();
		
//		tx = new AntifakeCodeMakeTransaction(network, Hex.decode("160100000001def02ea3e5b87e22d4d7e03c49686dd994a117f09fcbce7e739bd4c11fa9c6d700000000c8473044022045b9006d6316d5d5a4a169bc11eb1a52bf9f73dc4187a46cb4cafca671e53db102207127a028882e78330e0b8953b214ee7dc81cda0769e1c82764beec12c30d083901483045022100ae2742dff777e5cd40eb7ea6c2ae66904fcc24593b2d50c5908fd43102c9966e022018e4fb1596bb1eebd11b873562397249bed82e7b1387e0e2f1fc6d374afb0a6c01c2209c890b3c4a3dd4599b445e3d40034bc5c289df7e37e391f2648124f2c273d60814a35e97d7189abb8274afa8e264b106a1e1041096ffffffff0200e1f5050000000000000000000000003a75c314a35e97d7189abb8274afa8e264b106a1e10410968820ce2efc6f9402ea928c3e8adbfbd78463342fb93f3fe97ae59055b1a151a18802ac002fafcee800000000000000000000001975c314a35e97d7189abb8274afa8e264b106a1e104109688ac6b11ec835a0100000000000000000000c8c2209c890b3c4a3dd4599b445e3d40034bc5c289df7e37e391f2648124f2c273d608c314a35e97d7189abb8274afa8e264b106a1e104109688463044022020901104daeb6dd20045452d23d2ca0abb87fd6d9146f80eb52d28a5e3f035890220472ab477376165fd49c7c75d4d5d3e60bce8893129c315e5fb245644740e20ca4630440220556e26db1212f0ebb3d7d55f8c756297145665d5d05d4ae9b954c67141e6c47302207d5e805bb2985202efce870b522bc1a2a3e42abb3027cd8f351e206a977a8c38ac58259ec8cfa0689899a68d449c420b85539c21cbb298a6299c8ec94875f5b76c0074c0cc1af5b802"));

		log.info("tx {}", tx);
		log.info("tx size {}", tx.baseSerialize().length);
		
		log.info("tx content : {}", Hex.encode(tx.baseSerialize()));
		
		
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
