package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 除转账交易外的其他交易
 * 均继承该类
 * @author ln
 *
 */
public abstract class CommonlyTransaction extends BaseCommonlyTransaction {

		
	public CommonlyTransaction(NetworkParams network) {
		super(network);
	}
	
	public CommonlyTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public CommonlyTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	/**
	 * 验证交易脚本
	 */
	public void verifyScript() {
		verfifyCommonScript();
	}
	
	/**
	 * 序列化
	 */
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		//交易类型
		stream.write(type);
		//版本号
		Utils.uint32ToByteStreamLE(version, stream);
		//时间
		Utils.int64ToByteStreamLE(time, stream);
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
		
		this.type = readBytes(1)[0] & 0XFF;
		if(isCompatible()) {
			length = (int) readUint32();
		}
		
		this.version = readUint32();
		this.time = readInt64();
		this.scriptBytes = readBytes((int)readVarInt());
		this.scriptSig = new Script(this.scriptBytes);
		
		if(!isCompatible()) {
        	length = cursor - offset;
        }
	}
}
