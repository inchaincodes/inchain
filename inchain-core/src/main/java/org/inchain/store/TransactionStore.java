package org.inchain.store;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParameters;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;

public class TransactionStore extends Store {
	
	//转出未确认
	public final static int STATUS_SEND_UNCONFIRMED = 1;
	//转入未确认
	public final static int STATUS_RECE_UNCONFIRMED = 2;
	//转出已确认
	public final static int STATUS_SEND_CONFIRMED = 3;
	//转入已确认
	public final static int STATUS_RECE_CONFIRMED = 4;
	
	private byte[] key;
	//交易内容
	private Transaction transaction;
	//交易状态
	private byte status;
	//区块高度
	private long height;
	
	
	public TransactionStore(NetworkParameters network, byte[] content) {
		super(network, content, 0);
	}
	
	public TransactionStore(NetworkParameters network, byte[] content, int offset) {
		super(network, content, offset);
	}
	
	public TransactionStore(NetworkParameters network, Transaction transaction) {
		super(network);
		this.transaction = transaction;
	}

	public TransactionStore(NetworkParameters network, Transaction transaction, long height, int status) {
		super(network);
		this.transaction = transaction;
		this.height = height;
		this.status = (byte) status;
	}
	
	@Override
	protected void parse() throws ProtocolException {
		status = readBytes(1)[0];
		height = readUint32();
		
		transaction = network.getDefaultSerializer().makeTransaction(payload, 5);
		
		length = 5 + transaction.getLength();
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(status);
		Utils.uint32ToByteStreamLE(height, stream);
		stream.write(transaction.baseSerialize());
	}
	
	public Transaction getTransaction() {
		return transaction;
	}
	
	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}
	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}
	@Override
	public byte[] getKey() {
		if(key == null) {
			key = transaction.getHash().getBytes();
		}
		return key;
	}

	@Override
	public void setKey(byte[] key) {
		this.key = key;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}
	
}
