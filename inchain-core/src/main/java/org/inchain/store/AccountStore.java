package org.inchain.store;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;
import org.spongycastle.util.Arrays;

/**
 * 账户信息存储
 * @author ln
 *
 */
public class AccountStore extends Store {

	public enum AccountType {
		SYSTEM,	//系统普通账户
		CERT,	//认证账户
	}
	
	//账户类型
	private int type;
	private byte[] hash160;
	private byte[][] pubkeys;
	private long balance;
	private long lastModifyTime;
	private long createTime;
	private long cert;	//信用值
	private Sha256Hash infoTxid;
	private AccountBody accountBody;

	public AccountStore(NetworkParams network) {
		super(network);
	}
	
	public AccountStore(NetworkParams network, byte[] payload) {
		super(network, payload, 0);
	}
	
	public AccountStore(NetworkParams network, byte[] payload, int offset) {
		super(network, payload, offset);
	}

	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(type);
		stream.write(hash160);
		
		if(type == network.getSystemAccountVersion()) {
			stream.write(new VarInt(pubkeys[0].length).encode());
			stream.write(pubkeys[0]);
		} else {
			for (byte[] pubkey : pubkeys) {
				stream.write(new VarInt(pubkey.length).encode());
				stream.write(pubkey);
			}
			stream.write(infoTxid.getReversedBytes());
		}
		
		Utils.int64ToByteStreamLE(balance, stream);
		Utils.int64ToByteStreamLE(lastModifyTime, stream);
		Utils.int64ToByteStreamLE(createTime, stream);
		Utils.int64ToByteStreamLE(cert, stream);
		stream.write(accountBody.serialize());
	}
	
	@Override
	protected void parse() throws ProtocolException {
		type = readBytes(1)[0] & 0xff;
		hash160 = readBytes(Address.LENGTH);
		if(type == network.getSystemAccountVersion()) {
			pubkeys = new byte[][] {readBytes((int) readVarInt())};
		} else {
			pubkeys = new byte[][] {
				readBytes((int) readVarInt()),
				readBytes((int) readVarInt()),
				readBytes((int) readVarInt()),
				readBytes((int) readVarInt())
				};
			infoTxid = readHash();
		}
		balance = readInt64();
		lastModifyTime = readInt64();
		createTime = readInt64();
		cert = readInt64();
		
		if(cursor < payload.length) {
			accountBody = new AccountBody(Arrays.copyOfRange(payload, cursor, payload.length));
		} else {
			accountBody = AccountBody.empty();
		}
		
		length = cursor + accountBody.serialize().length - offset;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public byte[] getHash160() {
		return hash160;
	}

	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}

	public byte[][] getPubkeys() {
		return pubkeys;
	}

	public void setPubkeys(byte[][] pubkeys) {
		this.pubkeys = pubkeys;
	}

	public long getBalance() {
		return balance;
	}

	public void setBalance(long balance) {
		this.balance = balance;
	}

	public long getLastModifyTime() {
		return lastModifyTime;
	}

	public void setLastModifyTime(long lastModifyTime) {
		this.lastModifyTime = lastModifyTime;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getCert() {
		return cert;
	}

	public void setCert(long cert) {
		this.cert = cert;
	}

	public void setInfoTxid(Sha256Hash infoTxid) {
		this.infoTxid = infoTxid;
	}
	
	public Sha256Hash getInfoTxid() {
		return infoTxid;
	}

	public AccountBody getAccountBody() {
		return accountBody;
	}

	public void setAccountBody(AccountBody accountBody) {
		this.accountBody = accountBody;
	}
}
