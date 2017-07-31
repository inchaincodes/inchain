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
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		//hash 160
		Utils.checkNotNull(revokeHash160);
		stream.write(revokeHash160);

		//superhash 160
		Utils.checkNotNull(hash160);
		stream.write(hash160);
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
