package org.inchain.signers;

import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.RedeemData;
import org.inchain.core.Definition;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.crypto.TransactionSignature;
import org.inchain.crypto.ECKey.ECDSASignature;
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
            if (txIn.getFroms() == null || txIn.getFroms().size() == 0) {
                log.warn("缺少上次交易的引用,index:{}", i);
                continue;
            }

            RedeemData redeemData = txIn.getFroms().get(0).getRedeemData(key);

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
     * 普通账户的签名
     */
    @Override
    public boolean signOneInputs(Transaction tx, ECKey key,int inputIndex) {
        int numInputs = tx.getInputs().size();
        if(numInputs<inputIndex+1){
            log.warn("交易输入index越界");
            return false;
        }

        TransactionInput txIn = (TransactionInput) tx.getInput(inputIndex);
        if (txIn.getFroms() == null || txIn.getFroms().size() == 0) {
            log.warn("缺少上次交易的引用,index:{}", inputIndex);
            return false;
        }

        RedeemData redeemData = txIn.getFroms().get(0).getRedeemData(key);

        if ((key = redeemData.getFullKey()) == null) {
            log.warn("No local key found for input {}", inputIndex);
            return false;
        }

        Script inputScript = txIn.getScriptSig();
        Script redeemScript = redeemData.redeemScript;

        byte[] script = redeemScript.getProgram();
        try {
            TransactionSignature signature = tx.calculateSignature(inputIndex, key, script, Transaction.SigHash.ALL);
            int sigIndex = 0;
            inputScript = redeemScript.getScriptSigWithSignature(inputScript, signature.encode(), sigIndex);
            txIn.setScriptSig(inputScript);
        } catch (ECKey.MissingPrivateKeyException e) {
            log.warn("No private key in keypair for input {}", inputIndex);
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
            if (txIn.getFroms() == null || txIn.getFroms().size() == 0) {
                log.warn("缺少上次交易的引用,index:{}", i);
                continue;
            }

            Script inputScript = txIn.getFromScriptSig();
            
            List<ScriptChunk> chunks = inputScript.getChunks();
            
            Utils.checkState(chunks.size() == 5);
            Utils.checkState(eckeys.length == 1);
            
            byte[][] signs = new byte[1][];
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

	
	/**
     * 账户对指定内容签名
	 * 如果账户已加密的情况，则需要先解密账户
     * 如果是认证账户，默认使用交易的签名，如需使用账户管理签名，则调用sign(account, TransactionDefinition.TX_VERIFY_MG)
     * @param account
     * @param hash
     * @return byte[][]
     */
	public static byte[][] signHash(Account account, Sha256Hash hash) {
		return signHash(account, hash, Definition.TX_VERIFY_TR);
	}
	
	/**
	 * 账户对指定内容签名
	 * 如果账户已加密的情况，则需要先解密账户
	 * @param account
     * @param hash
	 * @param type TransactionDefinition.TX_VERIFY_MG利用管理私钥签名，TransactionDefinition.TX_VERIFY_TR利用交易私钥签名
	 */
	public static byte[][] signHash(Account account, Sha256Hash hash, int type) {
		
		if(account.isCertAccount()) {
			//认证账户
			if(account.getAccountTransaction() == null) {
				throw new VerificationException("签名失败，认证账户没有对应的信息交易");
			}
			
			ECKey[] keys = null;
			if(type == Definition.TX_VERIFY_MG) {
				keys = account.getMgEckeys();
			} else {
				keys = account.getTrEckeys();
			}
			
			if(keys == null) {
				throw new VerificationException("账户没有解密？");
			}
			
			ECDSASignature ecSign = keys[0].sign(hash);
			byte[] sign1 = ecSign.encodeToDER();
			
			ecSign = keys[1].sign(hash);
			byte[] sign2 = ecSign.encodeToDER();
			
			return new byte[][] {sign1, sign2};
		} else {
			//普通账户
			ECKey key = account.getEcKey();
			ECDSASignature ecSign = key.sign(hash);
			byte[] sign = ecSign.encodeToDER();

			return new byte[][] {sign};
		}
		
	}
}
