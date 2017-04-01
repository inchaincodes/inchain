package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;

/**
 * 认证账户解除跟子账户的关联
 * @author ln
 *
 */
public class RemoveSubAccountTransaction extends CommonlyTransaction {

	//关联账户
	private byte[] relevanceHashs;
	//交易id
	private Sha256Hash txhash;
	
	public RemoveSubAccountTransaction(NetworkParams network, byte[] relevanceHashs, Sha256Hash txhash) {
		super(network);
		this.relevanceHashs = relevanceHashs;
		this.txhash = txhash;
		
		type = Definition.TYPE_REMOVE_SUBACCOUNT;
	}

	public RemoveSubAccountTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public RemoveSubAccountTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		if(relevanceHashs == null || relevanceHashs.length != Address.HASH_LENGTH) {
			throw new VerificationException("关联者错误");
		}
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(relevanceHashs);
		stream.write(txhash.getReversedBytes());
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		relevanceHashs = readBytes(25);
		txhash = readHash();
		length = cursor - offset;
	}

	public byte[] getRelevanceHashs() {
		return relevanceHashs;
	}

	public void setRelevanceHashs(byte[] relevanceHashs) {
		this.relevanceHashs = relevanceHashs;
	}

	public Sha256Hash getTxhash() {
		return txhash;
	}

	public void setTxhash(Sha256Hash txhash) {
		this.txhash = txhash;
	}
	
	public Address getAddress() {
		return Address.fromHashs(network, relevanceHashs);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoveSubAccountTransaction [relevanceHashs=");
		builder.append(Address.fromHashs(network, relevanceHashs).getBase58());
		builder.append(", txhash=");
		builder.append(txhash);
		builder.append("]");
		return builder.toString();
	}
}
