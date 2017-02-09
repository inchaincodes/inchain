package org.inchain.signers;

import java.util.List;

import org.inchain.account.RedeemData;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.TransactionSignature;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.script.ScriptChunk;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalTransactionSigner implements TransactionSigner {
	
    private static final Logger log = LoggerFactory.getLogger(LocalTransactionSigner.class);

    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 普通账户的签名
     */
    @Override
    public boolean signInputs(Transaction tx, ECKey key) {
        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = (TransactionInput) tx.getInput(i);
            if (txIn.getFrom() == null) {
                log.warn("缺少上次交易的引用,index:{}", i);
                continue;
            }

            RedeemData redeemData = txIn.getFrom().getRedeemData(key);

            if ((key = redeemData.getFullKey()) == null) {
                log.warn("No local key found for input {}", i);
                continue;
            }

            Script inputScript = txIn.getScriptSig();
            Script redeemScript = redeemData.redeemScript;
            
            byte[] script = redeemScript.getProgram();
            try {
                TransactionSignature signature = tx.calculateSignature(i, key, script, Transaction.SigHash.ALL);
                int sigIndex = 0;
                inputScript = redeemScript.getScriptSigWithSignature(inputScript, signature.encode(), sigIndex);
                txIn.setScriptSig(inputScript);
            } catch (ECKey.MissingPrivateKeyException e) {
                log.warn("No private key in keypair for input {}", i);
            }

        }
        return true;
    }

    /**
     * 认证账户的签名
     * @param tx
     * @param eckeys
     * @param txid
     * @param hash160
     */
	public boolean signCertAccountInputs(Transaction tx, ECKey[] eckeys, byte[] txid, byte[] hash160) {
		int numInputs = tx.getInputs().size();
		
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = (TransactionInput) tx.getInput(i);
            if (txIn.getFrom() == null) {
                log.warn("缺少上次交易的引用,index:{}", i);
                continue;
            }

            Script inputScript = txIn.getFromScriptSig();
            
            List<ScriptChunk> chunks = inputScript.getChunks();
            
            Utils.checkState(chunks.size() == 5);
            Utils.checkState(eckeys.length == 2);
            
            byte[][] signs = new byte[2][];
            for (int j = 0; j < eckeys.length; j++) {
	            TransactionSignature signature = tx.calculateSignature(i, eckeys[j], inputScript.getProgram(), Transaction.SigHash.ALL);
	            signs[j] = signature.encode();
			}
            txIn.setScriptSig(ScriptBuilder.createCertAccountInputScript(signs, txid, hash160));
        }
        return true;
	}

	@Override
	public byte[] serialize() {
		return null;
	}

	@Override
	public void deserialize(byte[] data) {
		
	}

}
