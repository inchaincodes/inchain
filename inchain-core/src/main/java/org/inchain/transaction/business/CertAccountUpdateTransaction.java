package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.AccountBody;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;

/**
 * 认证账户信息修改
 * @author ln
 *
 */
public class CertAccountUpdateTransaction extends CertAccountRegisterTransaction {
	
	public CertAccountUpdateTransaction(NetworkParams network, byte[] hash160, byte[][] mgPubkeys, byte[][] trPubkeys, AccountBody body,byte[] superhash160,int superlevel) {
		super(network, hash160, mgPubkeys, trPubkeys, body,superhash160,superlevel);
		this.setType(Definition.TYPE_CERT_ACCOUNT_UPDATE);
	}
	
	public CertAccountUpdateTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public CertAccountUpdateTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	/**
	 * 反序列化交易
	 */
	protected void parse() {
		super.parse();
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
	}
	
	/**
	 * 验证交易的合法性
	 */
	@Override
	public void verify() throws VerificationException {
		super.verify();
	}
}
