package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;

/**
 * 认证账户解除跟子账户的关联
 * @author ln
 *
 */
public class RemoveSubAccountTransaction extends CommonlyTransaction {

	//关联账户
	private byte[] relevanceHash160;
	//交易id
	private Sha256Hash txhash;
	
	public RemoveSubAccountTransaction(NetworkParams network, byte[] relevanceHash160, Sha256Hash txhash) {
		super(network);
		this.relevanceHash160 = relevanceHash160;
		this.txhash = txhash;
	}

	public RemoveSubAccountTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public RemoveSubAccountTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(relevanceHash160);
		stream.write(txhash.getReversedBytes());
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		relevanceHash160 = readBytes(20);
		txhash = readHash();
		length = cursor - offset;
	}

	public byte[] getRelevanceHash160() {
		return relevanceHash160;
	}

	public void setRelevanceHash160(byte[] relevanceHash160) {
		this.relevanceHash160 = relevanceHash160;
	}

	public Sha256Hash getTxhash() {
		return txhash;
	}

	public void setTxhash(Sha256Hash txhash) {
		this.txhash = txhash;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoveSubAccountTransaction [relevanceHash160=");
		builder.append(Hex.encode(relevanceHash160));
		builder.append(", txhash=");
		builder.append(txhash);
		builder.append("]");
		return builder.toString();
	}
}
