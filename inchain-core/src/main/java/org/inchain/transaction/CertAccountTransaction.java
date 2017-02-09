package org.inchain.transaction;

import org.inchain.account.Address;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.script.ScriptOpCodes;

/**
 * 认证账户类的交易
 * @author ln
 *
 */
public abstract class CertAccountTransaction extends Transaction {

	//帐户信息
	protected byte[] hash160;
	//管理公匙
	protected byte[][] mgPubkeys;
	//交易公匙
	protected byte[][] trPubkeys;
	//签名脚本
	protected byte[] scriptBytes;
	protected Script script;
	
	public CertAccountTransaction(NetworkParams params) throws ProtocolException {
		super(params);
    }
	
	public CertAccountTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public CertAccountTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	/**
	 * 验证交易的合法性
	 */
	public void verfify() throws VerificationException {
		if(hash160 == null || hash160.length != Address.LENGTH) {
			throw new VerificationException("hash160 错误");
		}
		
		if(mgPubkeys == null || mgPubkeys.length != 2) {
			throw new VerificationException("账户管理公钥个数不正确");
		}
		
		if(trPubkeys == null || trPubkeys.length != 2) {
			throw new VerificationException("交易公钥个数不正确");
		}
		
		if(scriptBytes == null) {
			throw new VerificationException("缺少签名信息");
		}
	}
	
	/**
	 * 验证签名
	 */
	@Override
	public void verfifyScript() {
		script.runCertAccountSign(this);
	}
	
	
	/**
	 * 更新交易签名，这个交易应该是特定账号的，接入商家用
	 * 虽然是这个账号有接入商家的权利，但实际上通过提供接口调用，控制权在第三方专业认证审核的公司那里
	 * @param txid   公钥交易引用
	 * @param key1
	 * @param key2
	 */
	public void calculateSignature(Sha256Hash txid, ECKey key1, ECKey key2) {
		Sha256Hash hash = Sha256Hash.of(baseSerialize());
		
		//签名
		ECDSASignature ecSign1 = key1.sign(hash);
		byte[] sign1 = ecSign1.encodeToDER();
		
		ECDSASignature ecSign2 = key2.sign(hash);
		byte[] sign2 = ecSign2.encodeToDER();
		
		script = ScriptBuilder.createCertAccountSign(ScriptOpCodes.OP_VERTR, txid, network.getCertAccountManagerHash160(), sign1, sign2);
		scriptBytes = script.getProgram();
	}
	
	/**
	 * 清楚交易验证脚本
	 */
	public void cleanScripts() {
		this.script = null;
		this.scriptBytes = null;
	}

	public byte[] getHash160() {
		return hash160;
	}

	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}

	public byte[][] getMgPubkeys() {
		return mgPubkeys;
	}

	public void setMgPubkeys(byte[][] mgPubkeys) {
		this.mgPubkeys = mgPubkeys;
	}

	public byte[][] getTrPubkeys() {
		return trPubkeys;
	}

	public void setTrPubkeys(byte[][] trPubkeys) {
		this.trPubkeys = trPubkeys;
	}

	public byte[] getScriptBytes() {
		return scriptBytes;
	}

	public void setScriptBytes(byte[] scriptBytes) {
		this.scriptBytes = scriptBytes;
	}

	public Script getScript() {
		return script;
	}

	public void setScript(Script script) {
		this.script = script;
	}
	
}
