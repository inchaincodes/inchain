package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 信用累积交易
 * 按照系统规则运行
 * @author ln
 *
 */
public class CreditTransaction extends CommonlyTransaction {
	
	/** 信用获得者 **/
	private byte[] ownerHash160;
	/** 信用值 **/
	private long credit;
	/** 变动原因，参考顶部的定义 **/
	private int reasonType;
	/** 是根据哪个交易做出的变动 **/
	private Sha256Hash reason;
	
	public CreditTransaction(NetworkParams params) throws ProtocolException {
		super(params);
		type = Definition.TYPE_CREDIT;
	}
	
	public CreditTransaction(NetworkParams params, byte[] ownerHash160, long credit, int reasonType, Sha256Hash reason) throws ProtocolException {
		super(params);
		type = Definition.TYPE_CREDIT;
		this.ownerHash160 = ownerHash160;
		this.credit = credit;
		this.reasonType = reasonType;
		this.reason = reason;
	}
	
	public CreditTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
	}

	public CreditTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		
		ownerHash160 = readBytes(Address.LENGTH);
		credit = readInt64();
		reasonType = readBytes(1)[0] & 0XFF;
		reason = readHash();
		
		length = cursor - offset;
	}
	
	/**
	 * 序列化
	 */
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		
		stream.write(ownerHash160);
		Utils.int64ToByteStreamLE(credit, stream);
		stream.write(reasonType);
		
		if(reason == null) {
			reason = Sha256Hash.ZERO_HASH;
		}
		stream.write(reason.getReversedBytes());
    }

	public long getCredit() {
		return credit;
	}

	public void setCredit(long credit) {
		this.credit = credit;
	}

	public byte[] getOwnerHash160() {
		return ownerHash160;
	}

	public void setOwnerHash160(byte[] ownerHash160) {
		this.ownerHash160 = ownerHash160;
	}

	public Sha256Hash getReason() {
		return reason;
	}

	public void setReason(Sha256Hash reason) {
		this.reason = reason;
	}

	public int getReasonType() {
		return reasonType;
	}

	public void setReasonType(int reasonType) {
		this.reasonType = reasonType;
	}
}
