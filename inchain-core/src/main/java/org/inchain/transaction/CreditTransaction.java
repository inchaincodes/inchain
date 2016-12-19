package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Address;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 信用累积交易，只有在创世块里初始化有用
 * @author ln
 *
 */
public class CreditTransaction extends Transaction {

	private byte[] hash160;
	private long credit;
	
	public CreditTransaction(NetworkParams params) throws ProtocolException {
		super(params);
		type = TransactionDefinition.TYPE_INIT_CREDIT;
	}
	
	public CreditTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
	}

	public CreditTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
		type = TransactionDefinition.TYPE_INIT_CREDIT;
	}
	
	@Override
	protected void parse() throws ProtocolException {
		type = readBytes(1)[0];
		hash160 = readBytes(Address.LENGTH);
		credit = readUint32();
		
		length = cursor - offset;
	}
	
	/**
	 * 序列化
	 */
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(type);
		stream.write(hash160);
		Utils.uint32ToByteStreamLE(credit, stream);
    }

	public byte[] getHash160() {
		return hash160;
	}

	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}

	public long getCredit() {
		return credit;
	}

	public void setCredit(long credit) {
		this.credit = credit;
	}
}
