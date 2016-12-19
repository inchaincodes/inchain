package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 注册成为共识节点交易
 * @author ln
 *
 */
public class RegConsensusTransaction extends Transaction {

	//节点地址
	private byte[] hash160;
	//公钥
	private byte[] pubkey;
	//签名
	private byte[] scriptBytes;
	//签名验证脚本
	private Script scriptSig;

	public RegConsensusTransaction(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public RegConsensusTransaction(NetworkParams network, byte[] payloadBytes, int offset) {
		super(network, payloadBytes, offset);
	}
	
	public RegConsensusTransaction(NetworkParams network, long version, byte[] hash160, long time) {
		super(network);
		
		this.type = TransactionDefinition.TYPE_REG_CONSENSUS;
		this.version = version;
		this.hash160 = hash160;
		this.lockTime = time;
	}
	
	public void sign(ECKey key) {
		ECDSASignature ecSign = key.sign(getHash());
		byte[] sign = ecSign.encodeToDER();
		
		scriptSig = ScriptBuilder.createVerifyScript(key.getPubKey(true), sign);
		scriptBytes = scriptSig.getProgram();
		
		setHash(null);
	}

	/**
	 * 验证交易的合法性
	 */
	public void verfify() throws VerificationException {
		if(type != TransactionDefinition.TYPE_REG_CONSENSUS) {
			throw new VerificationException("交易类型错误");
		}
		if(scriptBytes == null || scriptSig == null) {
			throw new VerificationException("验证脚步不存在");
		}
		//验证公钥是否合法
		if(scriptSig == null) {
			scriptSig = new Script(scriptBytes);
		}
		byte[] pubkey = scriptSig.getChunks().get(0).data;
		Address address = AccountTool.newAddress(network, ECKey.fromPublicOnly(pubkey));
		if(!Arrays.equals(hash160, address.getHash160())) {
			throw new VerificationException("公钥不匹配");
		} else {
			this.pubkey = pubkey;
		}
	}

	/**
	 * 验证交易脚本
	 */
	public void verfifyScript() {
		//特殊的验证脚本
		RegConsensusTransaction temp = new RegConsensusTransaction(this.getNetwork(), this.baseSerialize());
		temp.setScriptBytes(null);
		temp.getScriptSig().runVerify(temp.getHash());
	}
	
	/**
	 * 序列化
	 */
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		//版本号
		stream.write(TransactionDefinition.TYPE_REG_CONSENSUS);
		Utils.uint32ToByteStreamLE(version, stream);
		//节点地址
		stream.write(hash160);
		//时间
		Utils.uint32ToByteStreamLE(lockTime, stream);
		//签名
		if(scriptBytes != null) {
			stream.write(new VarInt(scriptBytes.length).encode());
			stream.write(scriptBytes);
		}
	}
	
	/**
	 * 反序列化
	 */
	@Override
	protected void parse() throws ProtocolException {
		
		this.type = readBytes(1)[0];
		this.version = readUint32();
		this.hash160 = readBytes(Address.LENGTH);
		this.lockTime = readUint32();
		this.scriptBytes = readBytes((int)readVarInt());
		this.scriptSig = new Script(this.scriptBytes);
		
		length = cursor - offset;
	}
	
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public byte[] getHash160() {
		return hash160;
	}

	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}

	public byte[] getScriptBytes() {
		return scriptBytes;
	}

	public void setScriptBytes(byte[] scriptBytes) {
		this.scriptBytes = scriptBytes;
	}

	public Script getScriptSig() {
		return scriptSig;
	}

	public void setScriptSig(Script scriptSig) {
		this.scriptSig = scriptSig;
	}

	public byte[] getPubkey() {
		return pubkey;
	}
	
	@Override
	public String toString() {
		return "RegConsensusTransaction [hash160=" + Hex.encode(hash160) + ", scriptBytes="
				+ Hex.encode(scriptBytes) + ", scriptSig=" + scriptSig + ", lockTime=" + lockTime + "]";
	}
	
}
