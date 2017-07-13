package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Address;
import org.inchain.account.RedeemData;
import org.inchain.core.Coin;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.ScriptException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Message;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.utils.Utils;

/**
 * 交易输出，本次的输出是下次花费时的输入
 * @author ln
 *
 */
public class TransactionOutput extends Message implements Output {

	private Transaction parent;
	//下次的花费
	private TransactionInput spentBy;
	//交易金额
	private long value;
	//锁定时间
	private long lockTime;
	
    private byte[] scriptBytes;
    
    private Script script;
    //交易输出的索引
    private int index;

    public TransactionOutput() {
    	
    }

    public TransactionOutput(Transaction parent) {
    	this.parent = parent;
    }
	
	public TransactionOutput(NetworkParams network, Transaction parent, byte[] payload, int offset) {
		super(network, payload, offset);
		this.parent = parent;
	}
	

	public TransactionOutput(Transaction parent, Coin value, Address to) {
		this(parent, value, 0l, to);
	}
	public TransactionOutput(Transaction parent, Coin value, long lockTime, Address to) {
		this(parent, value, lockTime, ScriptBuilder.createOutputScript(to).getProgram());
	}
	public TransactionOutput(Transaction parent, Coin value, ECKey to) {
		this(parent, value, 0l, ScriptBuilder.createOutputScript(to).getProgram());
	}
	public TransactionOutput(Transaction parent, Coin value,  byte[] scriptBytes) {
		this(parent, value, 0l, scriptBytes);
	}
	public TransactionOutput(Transaction parent, Coin value, long lockTime, byte[] scriptBytes) {
		this.parent = parent;
		this.value = value.value;
		this.lockTime = lockTime;
        this.scriptBytes = scriptBytes;
        this.script = new Script(scriptBytes);
	}

	/**
	 * 序列化
	 * @param stream
	 * @throws IOException 
	 */
	public void serialize(OutputStream stream) throws IOException {
		Utils.int64ToByteStreamLE(value, stream);
		Utils.uint32ToByteStreamLE(lockTime, stream);
		stream.write(new VarInt(scriptBytes.length).encode());
		stream.write(scriptBytes);
	}

	/**
	 * 反序列化交易的输出部分
	 */
	@Override
	protected void parse() throws ProtocolException {
        value = readInt64();
        lockTime = readUint32();
        //赎回脚本名的长度
        int signLength = (int)readVarInt();
        scriptBytes = readBytes(signLength);
        script = new Script(scriptBytes);
        
        length = cursor - offset;
	}
	
	/**
	 * 获取交易的输出脚本
	 * @param key
	 * @return RedeemData
	 * @throws ScriptException
	 */
	public RedeemData getRedeemData(ECKey key) throws ScriptException {
		if (script.isSentToAddress()) {
	        return RedeemData.of(key, script);
	    } else {
            throw new ScriptException("Could not understand form of connected output script: " + script);
        }
	}

	public byte[] getKey() {
		byte[] key = new byte[Sha256Hash.LENGTH + 1];
		System.arraycopy(parent.getHash().getBytes(), 0, key, 0, key.length - 1);
		key[key.length - 1] = (byte) index;
		return key;
	}
	
	public Transaction getParent() {
		return parent;
	}
	public void setParent(Transaction parent) {
		this.parent = parent;
	}
	public TransactionInput getSpentBy() {
		return spentBy;
	}
	public void setSpentBy(TransactionInput spentBy) {
		this.spentBy = spentBy;
	}
	public Script getScript() {
		return script;
	}
	public void setScript(Script script) {
		this.script = script;
		this.scriptBytes = script.getProgram();
	}
	public byte[] getScriptBytes() {
		return scriptBytes;
	}
	public void setScriptBytes(byte[] scriptBytes) {
		this.scriptBytes = scriptBytes;
		this.script = new Script(scriptBytes);
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public long getValue() {
		return value;
	}
	public void setValue(long value) {
		this.value = value;
	}
	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}
	public long getLockTime() {
		return lockTime;
	}

	@Override
	public String toString() {
		return "TransactionOutput [index=" + index + ", value=" + value + ", lockTime=" + lockTime + ", script="
				+ script + "]";
	}
	
}
