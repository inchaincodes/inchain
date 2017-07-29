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
 * 资产交易
 */
public class AssetsTransferTransaction extends AssetsIssuedTransaction {

	public AssetsTransferTransaction(NetworkParams network) {
		super(network);
		type = Definition.TYPE_ASSETS_TRANSFER;
	}

	public AssetsTransferTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
	}

	public AssetsTransferTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	public AssetsTransferTransaction(NetworkParams params, Sha256Hash assetsHash, byte[] receiver, Long amount,byte[]remark) {
		super(params);
		this.amount = amount;
		this.assetsHash = assetsHash;
		this.receiver = receiver;
		this.remark = remark;
		type = Definition.TYPE_ASSETS_TRANSFER;
	}

	@Override
	public void verify() throws VerificationException {
		super.verify();
	}

	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
	}

	@Override
	protected void parseBody() throws ProtocolException {
		super.parseBody();
	}
}
