package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 注册成为共识节点交易
 * @author ln
 *
 */
public class RegConsensusTransaction extends BaseCommonlyTransaction {

	//申请时的时段
	private long periodStartTime;
	//指定打包人
	private byte[] packager;

	public RegConsensusTransaction(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public RegConsensusTransaction(NetworkParams network, byte[] payloadBytes, int offset) {
		super(network, payloadBytes, offset);
	}
	
	public RegConsensusTransaction(NetworkParams network, long version, long periodStartTime, byte[] packager) {
		super(network);
		
		this.type = Definition.TYPE_REG_CONSENSUS;
		this.version = version;
		this.periodStartTime = periodStartTime;
		this.packager = packager;
	}

	/**
	 * 验证交易的合法性
	 */
	public void verify() throws VerificationException {
		
		super.verify();
		
		if(type != Definition.TYPE_REG_CONSENSUS) {
			throw new VerificationException("交易类型错误");
		}

		if(packager == null || packager.length != Address.LENGTH) {
			throw new VerificationException("指定共识人不正确");
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
		
		Utils.uint32ToByteStreamLE(periodStartTime, stream);

		stream.write(packager);
	}
	
	/**
	 * 反序列化
	 */
	@Override
	protected void parse() throws ProtocolException {
		super.parse();

		periodStartTime = readUint32();
		packager = readBytes(Address.LENGTH);

		length = cursor - offset;
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

	public byte[] getPackager() {
		return packager;
	}

	public void setScriptSig(Script scriptSig) {
		this.scriptSig = scriptSig;
	}
	public long getPeriodStartTime() {
		return periodStartTime;
	}

	public void setPeriodStartTime(long periodStartTime) {
		this.periodStartTime = periodStartTime;
	}

	@Override
	public String toString() {
		return "RegConsensusTransaction [scriptBytes=" + Hex.encode(scriptBytes) + ", scriptSig=" + scriptSig + ", periodStartTime=" + DateUtil.convertDate(new Date(periodStartTime * 1000))+ "]";
	}
	
}
