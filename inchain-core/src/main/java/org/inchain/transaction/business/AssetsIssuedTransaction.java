package org.inchain.transaction.business;

import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * 资产发行
 */
public class AssetsIssuedTransaction extends CommonlyTransaction {

	//注册资产
	protected Sha256Hash assetsHash;
	//接收人
	protected byte[] receiver;
	//数量
	protected long amount;

	public AssetsIssuedTransaction(NetworkParams network) {
		super(network);
		type = Definition.TYPE_ASSETS_ISSUED;
	}

	public AssetsIssuedTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }

	public AssetsIssuedTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	public AssetsIssuedTransaction(NetworkParams params, Sha256Hash assetsHash, byte[] receiver, Long amount,byte[]remark) {
		super(params);
		this.amount = amount;
		this.assetsHash = assetsHash;
		this.receiver = receiver;
		this.remark = remark;
		type = Definition.TYPE_ASSETS_ISSUED;
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		//验证接收人
		if(receiver == null || receiver.length != 25) {
			throw new VerificationException("接收人不正确");
		}
		if(amount <= 0) {
			throw new VerificationException("资产数量不正确");
		}
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);

		stream.write(assetsHash.getReversedBytes());

		stream.write(receiver);

		Utils.int64ToByteStreamLE(amount, stream);

		//备注
		if(remark == null || remark.length == 0) {
			stream.write(0);
		} else {
			stream.write(new VarInt(remark.length).encode());
			stream.write(remark);
		}
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();

		assetsHash = readHash();
		receiver = readBytes(25);
		amount = readInt64();
		remark = readBytes((int)readVarInt());

		length = cursor - offset;
	}

	public byte[] getReceiver() {
		return receiver;
	}

	public void setReceiver(byte[] receiver) {
		this.receiver = receiver;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public Sha256Hash getAssetsHash() {
		return assetsHash;
	}

	public void setAssetsHash(Sha256Hash assetsHash) {
		this.assetsHash = assetsHash;
	}


	@Override
	public String toString() {
		return "AssetsIssuedTransaction{" +
				"receiver=" + Arrays.toString(receiver) +
				", amount=" + amount +
				", remark=" + Arrays.toString(remark) +
				'}';
	}
}
