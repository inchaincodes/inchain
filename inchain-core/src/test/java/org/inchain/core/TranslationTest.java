package org.inchain.core;

import java.math.BigInteger;
import java.util.EnumSet;

import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParameters;
import org.inchain.network.TestNetworkParameters;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.LocalTransactionSigner;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.utils.Hex;
import org.junit.Test;

public class TranslationTest {

	@Test
	public void testTranslation() {
		
		NetworkParameters network = TestNetworkParameters.get();
		
        Address addr = Address.fromP2PKHash(network, Address.VERSION_TEST_PK, Hex.decode("ffdf74c494d27474def57c5cb4b41a5455705956"));

		//上次交易
		Transaction out = new Transaction(network);
		out.setHash(Sha256Hash.wrap(Hex.decode("75d58fffca9a69ba47056e435f7a5a2347a11d0093b50b415aa28e973d70640b")));
		
		Transaction tx = new Transaction(network);
		
		Script script = ScriptBuilder.createOutputScript(addr);
		
		TransactionOutput output = new TransactionOutput(out, Coin.COIN, script.getProgram());

		out.addOutput(output);
		
		//本次输入
		TransactionInput input = tx.addInput(output);

		//输出到该地址
		ECKey key = ECKey.fromPrivate(new BigInteger("16426823946378490801614451355554969482806436503112915489322677953633742147003"));
		
		Address to = AccountTool.newAddress(network, Address.VERSION_TEST_PK, key);
		//添加输出
		TransactionOutput newOutput = new TransactionOutput(tx, Coin.COIN, to);
		tx.addOutput(newOutput);
		//交易类型
		tx.setVersion(to.getVersion());
		
		//签名交易
		//创建一个输入的空签名
		input.setScriptSig(ScriptBuilder.createInputScript(null, key));

		//
		final LocalTransactionSigner signer = new LocalTransactionSigner();
		signer.signInputs(tx, key);
		
		byte[] txBytes = tx.baseSerialize();
		System.out.println(Hex.encode(txBytes));
		
		Transaction verfyTx = network.getDefaultSerializer().makeTransaction(txBytes, null);
		verfyTx.verfify();
		
		verfyTx.getInput(0).getScriptSig().correctlySpends(verfyTx, 0, 
				((TransactionInput)verfyTx.getInput(0)).getFrom().getScript(), EnumSet.of(Script.VerifyFlag.DERSIG, Script.VerifyFlag.P2SH));

	}
}
