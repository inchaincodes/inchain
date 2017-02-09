package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.core.TimeHelper;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 认证账户注册交易
 * @author ln
 *
 */
public class CertAccountRegisterTransaction extends CertAccountTransaction {
	
	//主体信息最大长度 10k
	private static final int MAX_BODY_LENGTH = 10 * 1024;
	
	//帐户主体
	private AccountBody body;
	
	public CertAccountRegisterTransaction(NetworkParams network, byte[] hash160, byte[][] mgPubkeys, byte[][] trPubkeys, AccountBody body) {
		super(network);
		this.setVersion(TransactionDefinition.VERSION);
		this.setType(TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER);
		this.hash160 = hash160;
		this.mgPubkeys = mgPubkeys;
		this.trPubkeys = trPubkeys;
		this.body = body;
		this.time = TimeHelper.currentTimeMillis();
	}
	
	public CertAccountRegisterTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public CertAccountRegisterTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	/**
	 * 反序列化交易
	 */
	protected void parse() {
		type = readBytes(1)[0] & 0xff;
		version = readUint32();
		time = readInt64();
		hash160 = readBytes(Address.LENGTH);
		
		//主体
		body = new AccountBody(readBytes((int) readVarInt()));
		
		//账户管理公钥
		int mgPubkeysCount = readBytes(1)[0] & 0xff;
		mgPubkeys = new byte[mgPubkeysCount][];
		for (int i = 0; i < mgPubkeysCount; i++) {
			mgPubkeys[i] = readBytes((int) readVarInt());
		}
		
		//交易公匙
		int trPubkeysCount = readBytes(1)[0] & 0xff;
		trPubkeys = new byte[trPubkeysCount][];
		for (int i = 0; i < trPubkeysCount; i++) {
			trPubkeys[i] = readBytes((int) readVarInt());
		}

		//签名
		scriptBytes = readBytes((int) readVarInt());
		script = new Script(scriptBytes);
		
		length = cursor - offset;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		//交易类型
		stream.write(type);
		//版本
		Utils.uint32ToByteStreamLE(version, stream);
		//交易时间
		Utils.int64ToByteStreamLE(time, stream);

		//hash 160
		Utils.checkNotNull(hash160);
		stream.write(hash160);
		
		//主体
		Utils.checkNotNull(body);
		byte[] bodyContent = body.serialize();
        stream.write(new VarInt(bodyContent.length).encode());
		stream.write(bodyContent);
		
		//帐户管理公匙
		Utils.checkNotNull(mgPubkeys);
		//公钥个数
        stream.write(mgPubkeys.length);
		for (byte[] bs : mgPubkeys) {
			//公钥长度
	        stream.write(new VarInt(bs.length).encode());
			stream.write(bs);
		}
		
		//交易公匙
		Utils.checkNotNull(trPubkeys);
        stream.write(trPubkeys.length);
		for (byte[] bs : trPubkeys) {
			//公钥长度
	        stream.write(new VarInt(bs.length).encode());
			stream.write(bs);
		}
		
		if(scriptBytes != null) {
			//签名的长度
	        stream.write(new VarInt(scriptBytes.length).encode());
			//签名
			stream.write(scriptBytes);
		}
	}
	
	/**
	 * 验证交易的合法性
	 */
	@Override
	public void verfify() throws VerificationException {
		super.verfify();
		if(body == null || body.serialize().length > MAX_BODY_LENGTH) {
			throw new VerificationException("主体信息错误");
		}
	}

	public AccountBody getBody() {
		return body;
	}

	public void setBody(AccountBody body) {
		this.body = body;
	}
	
}
