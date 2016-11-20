package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.VarInt;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 交易输入，本次的输入是上次的输出
 * @author ln
 *
 */
public class TransactionInput implements Input {

	public static final long NO_SEQUENCE = 0xFFFFFFFFL;
	
	private Transaction parent;
	//上次的输出
	private TransactionOutput from;

	private long sequence;
	private byte[] scriptBytes;
	private Script scriptSig;

	public TransactionInput() {
	}
	
	public TransactionInput(TransactionOutput from) {
		super();
		this.from = from;

        this.sequence = NO_SEQUENCE;
	}
	
	/**
	 * 序列化交易输入
	 * @param stream
	 * @throws IOException 
	 */
	public void serialize(OutputStream stream) throws IOException {
		//上一交易的引用
		if(parent.getType() != Transaction.TYPE_COINBASE) {
			Utils.checkNotNull(from);
			stream.write(from.getParent().getHash().getBytes());
			Utils.uint32ToByteStreamLE(from.getIndex(), stream);
		}
		//签名的长度
        stream.write(new VarInt(scriptBytes.length).encode());
        //签名
        stream.write(scriptBytes);
        //sequence，送者定义的交易版本，用于在交易被写入block之前更改交易
        Utils.uint32ToByteStreamLE(sequence, stream);
	}
	
	/**
	 * 清空输入脚本的签名，用在私匙签名之前
	 */
	public void clearScriptBytes() {
        scriptBytes = new byte[0];
        scriptSig = null;
    }
	
	public TransactionOutput getFrom() {
		return from;
	}
	public void setFrom(TransactionOutput from) {
		this.from = from;
	}
	public Transaction getParent() {
		return parent;
	}
	public void setParent(Transaction parent) {
		this.parent = parent;
	}
	public boolean hasSequence() {
        return sequence > 0;
    }
	public void setSequence(long sequence) {
		this.sequence = sequence;
	}
	public long getSequence() {
		return sequence;
	}
	public byte[] getScriptBytes() {
		return scriptBytes;
	}

	public void setScriptBytes(byte[] scriptBytes) {
		this.scriptBytes = scriptBytes;
		this.scriptSig = new Script(scriptBytes);
	}

	public Script getScriptSig() {
		return scriptSig;
	}

	public void setScriptSig(Script scriptSig) {
		this.scriptSig = scriptSig;
		this.scriptBytes = scriptSig.getProgram();
	}

}
