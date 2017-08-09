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

	public static Long ASSET_ISSUE_ALL_MAX_VALUE = 999999999999L; //资产发行总限额
	public static Long ASSET_ISSUE_ONCE_MAX_VALUE = 10000000000L; //单次发行最大限额

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
		if(receiver == null || receiver.length != Address.LENGTH) {
			throw new VerificationException("接收人不正确");
		}
		if(amount <= 0) {
			throw new VerificationException("资产数量不正确");
		}
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		stream.write(assetsHash.getReversedBytes());
		stream.write(receiver);
		Utils.int64ToByteStreamLE(amount, stream);

	}
	
	@Override
	protected void parseBody() throws ProtocolException {
		assetsHash = readHash();
		receiver = readBytes(20);
		amount = readInt64();
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

	public String getReceiveAddress() {
		return Address.fromHashs(network, receiver).getBase58();
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
