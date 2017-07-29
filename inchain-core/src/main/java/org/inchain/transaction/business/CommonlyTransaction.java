package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.Configure;
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
		serializeHeadToStream(stream);
		serializeBodyToStream(stream);
		serializeRemarkToStream(stream);
	}
	protected void serializeHeadToStream(OutputStream stream) throws IOException {
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
		}else{
			stream.write(new VarInt(0).encode());
		}
	}

	protected void serializeBodyToStream(OutputStream stream) throws IOException {

	}

	protected void serializeRemarkToStream(OutputStream stream)throws IOException {

		if(remark == null || remark.length == 0) {
			stream.write(0);
		}
		if(remark.length> Configure.MAX_REMARK_LEN){
			stream.write(new VarInt(Configure.MAX_REMARK_LEN).encode());
			stream.write(remark,0,Configure.MAX_REMARK_LEN);
		}
		else {
			stream.write(new VarInt(remark.length).encode());
			stream.write(remark);
		}
	}

	/**
	 * remark字段作为可扩展字段需要增加parseRemark方法
	 */
	@Override
	protected void  parse() throws ProtocolException {
		parseHead();
		parseBody();
		parseRemark();
		length = cursor - offset;
	}

	/**
	 * CommonlyTransaction 不涉及代币交易，所以parseHead()方法重写,
	 * */
	@Override
	protected void parseHead()throws ProtocolException{
		this.type = readBytes(1)[0] & 0XFF;
		this.version = readUint32();
		if(isCompatible()) {
			length = (int) readUint32();
		}
		this.time = readInt64();
		int scriptLen = (int)readVarInt();
		if(scriptLen>0) {
			this.scriptBytes = readBytes(scriptLen);
			this.scriptSig = new Script(this.scriptBytes);
		}else {
			this.scriptBytes = null;
			this.scriptSig = null;
		}
	}

	/**
	 * 反序列化
	 */
	@Override
	protected void parseBody() throws ProtocolException {

	}

	protected void parseRemark() throws ProtocolException {
		int remarkLen = (int)readVarInt();
		if(remarkLen>0){
			remark = readBytes(remarkLen);
		}else {
			remark = null;
		}
	}
}
