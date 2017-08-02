package org.inchain.transaction.business;

import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 认证账户信息修改
 * @author ln
 *
 */
public class CertAccountRevokeTransaction extends CertAccountTransaction {

	private byte[] revokeHash160;

	public CertAccountRevokeTransaction(NetworkParams network, byte[] revokeHash160, byte[][] mgPubkeys, byte[][] trPubkeys, byte[] hash160,int level) {
		super(network);
		this.setVersion(Definition.VERSION);
		this.revokeHash160 = revokeHash160;
		this.hash160 = hash160;
		this.mgPubkeys = mgPubkeys;
		this.trPubkeys = trPubkeys;
		this.superhash160 = null;
		this.level = level;
		this.setType(Definition.TYPE_CERT_ACCOUNT_REVOKE);
	}

	public CertAccountRevokeTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }

	public CertAccountRevokeTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	/**
	 * 反序列化交易
	 */
	protected void parseBody() {
		revokeHash160 =  readBytes(Address.LENGTH);
		hash160 = readBytes(Address.LENGTH);

		//管理公钥
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
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		//hash 160
		Utils.checkNotNull(revokeHash160);
		stream.write(revokeHash160);

		//superhash 160
		Utils.checkNotNull(hash160);
		stream.write(hash160);

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
	}
	
	/**
	 * 验证交易的合法性
	 */
	@Override
	public void verify() throws VerificationException {
		super.verify();
	}

	public void setRevokeHash160(byte[] rhash160){
		this.revokeHash160 = rhash160;
	}

	public byte[]getRevokeHash160(){
		return this.revokeHash160;
	}
}
