package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Hex;

/**
 * 注册成为共识节点交易
 * @author ln
 *
 */
public class RegConsensusTransaction extends CommonlyTransaction {

	public RegConsensusTransaction(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public RegConsensusTransaction(NetworkParams network, byte[] payloadBytes, int offset) {
		super(network, payloadBytes, offset);
	}
	
	public RegConsensusTransaction(NetworkParams network, long version, long time) {
		super(network);
		
		this.type = Definition.TYPE_REG_CONSENSUS;
		this.version = version;
		this.time = time;
	}

	/**
	 * 验证交易的合法性
	 */
	public void verify() throws VerificationException {
		
		super.verify();
		
		if(type != Definition.TYPE_REG_CONSENSUS) {
			throw new VerificationException("交易类型错误");
		}
	}

	/**
	 * 验证交易脚本
	 */
	public void verifyScript() {
		//特殊的验证脚本
		super.verifyScript();
	}
	
	/**
	 * 序列化
	 */
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
	}
	
	/**
	 * 反序列化
	 */
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
	}
	
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
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
	
	@Override
	public String toString() {
		return "RegConsensusTransaction [scriptBytes=" + Hex.encode(scriptBytes) + ", scriptSig=" + scriptSig + ", time=" + time + "]";
	}
	
}
