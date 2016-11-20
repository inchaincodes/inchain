package org.inchain.account;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.network.NetworkParameters;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 账户
 * @author ln
 *
 */
public class Account {

	public static enum AccountType {
		SYSTEM(1),		//系统帐户
		CONTRACT(2),	//合约帐户
		APP(3),			//应用账户
		CERT(9),		//认证帐户
		;
		
		private final int value;
        private AccountType(final int value) {
            this.value = value;
        }
        public byte value() {
            return (byte) this.value;
        }
	}
	
	//账户类型
	private AccountType accountType;
	//帐户状态
	private byte status;
	//帐户地址
	private Address address;
	//私匙种子
	private byte[] priSeed;
	//管理公匙
	private byte[][] mgPubkeys;
	//交易公匙
	private byte[][] trPubkeys;
	//帐户主体
	private byte[] body;
	//帐户信息签名
	private byte[][] signs;
	
	public Account() {
		accountType = AccountType.SYSTEM;
	}
	
	/**
	 * 序列化帐户信息
	 * @return byte[]
	 * @throws IOException
	 */
	public final byte[] serialize() throws IOException  {
		ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(size());
		try {
			bos.write(status);//状态，待激活
			bos.write(accountType.value());//类型
			bos.write(address.getHash160());
			bos.write(priSeed.length);
			bos.write(priSeed);
			for (byte[] mgPubkey : mgPubkeys) {
				bos.write(mgPubkey.length);
				bos.write(mgPubkey);
			}
			for (byte[] trPubkey : trPubkeys) {
				bos.write(trPubkey.length);
				bos.write(trPubkey);
			}
			//帐户主体
			Utils.uint32ToByteStreamLE(body.length, bos);
			bos.write(body);
			if(signs != null) {
				for (byte[] sign  : signs) {
					//签名
					bos.write(sign.length);
					bos.write(sign);
				}
			}
			return bos.toByteArray();
		} finally {
			bos.close();
		}
    }

	/**
	 * 反序列化
	 * @param datas
	 * @return Account
	 */
	public static Account parse(byte[] datas, NetworkParameters network) {
		Account account = new Account();
		
		int cursor = 0;
		//状态
		account.setStatus(datas[cursor]);
		cursor ++;
		//帐户类型
		byte type = datas[cursor];
		if(type == AccountType.SYSTEM.value()) {
			account.setAccountType(AccountType.SYSTEM);
		} else if(type == AccountType.CERT.value()) {
			account.setAccountType(AccountType.CERT);
		}
		cursor ++;
		
		//hash 160
		byte[] hash160 = readBytes(cursor, 20, datas);
		Address address = Address.fromP2PKHash(network, type, hash160);
		account.setAddress(address);
		cursor += 20;
		
		//私匙种子
		int length = datas[cursor];
		cursor ++;
		account.setPriSeed(readBytes(cursor, length, datas));
		cursor += length;
		
		//帐户管理公匙
		length = datas[cursor];
		cursor ++;
		byte[] mgPubkey1 = readBytes(cursor, length, datas);
		cursor += length;
		
		length = datas[cursor];
		cursor ++;
		byte[] mgPubkey2 = readBytes(cursor, length, datas);
		
		account.setMgPubkeys(new byte[][] {mgPubkey1, mgPubkey2});
		cursor += length;
		
		//交易公匙
		length = datas[cursor];
		cursor ++;
		byte[] trPubkey1 = readBytes(cursor, length, datas);
		cursor += length;
		
		length = datas[cursor];
		cursor ++;
		byte[] trPubkey2 = readBytes(cursor, length, datas);
		
		account.setTrPubkeys(new byte[][] {trPubkey1, trPubkey2});
		cursor += length;
		
		//主体
		length = (int) Utils.readUint32(datas, cursor);
		cursor += 4;
		account.setBody(readBytes(cursor, length, datas));
		cursor += length;
		
		//签名
		length = datas[cursor];
		cursor ++;
		byte[] sign1 = readBytes(cursor, length, datas);
		cursor += length;
		length = datas[cursor];
		cursor ++;
		byte[] sign2 = readBytes(cursor, length, datas);
		cursor += length;
		
		account.setSigns(new byte[][] {sign1, sign2});
		
		return account;
	}
	
	private static byte[] readBytes(int offset, int length, byte[] datas) {
		byte[] des = new byte[length];
		System.arraycopy(datas, offset, des, 0, length);
		return des;
	}

	/**
	 * 帐户信息大小
	 * @return int
	 */
	public final int size() {
		int size = 1+1+20; //状态+类型+hash160
		size += priSeed.length + 1;
		
		for (byte[] mgPubkey : mgPubkeys) {
			size += mgPubkey.length + 1;
		}
		for (byte[] trPubkey : trPubkeys) {
			size += trPubkey.length + 1;
		}
		
		size += body.length + 4;
		
		if(signs != null) {
			for (byte[] sign : signs) {
				size += sign.length + 1;
			}
		}
		return size;
	}

	/**
	 * 签名交易
	 * @return Script
	 */
	public Script signTreade(String pwd) {
		
		return null;
	}

	//签名帐户
	public void signAccount(ECKey mgkey1, ECKey mgkey2) throws IOException {
		//用户帐户管理私匙签名
		Sha256Hash hash = Sha256Hash.of(serialize());
		ECDSASignature signature1 = mgkey1.sign(hash);
		//签名结果
        byte[] signbs1 = signature1.encodeToDER();
        ECDSASignature signature2 = mgkey2.sign(hash);
        //签名结果
        byte[] signbs2 = signature2.encodeToDER();
        
        signs = new byte[][] {signbs1, signbs2};
	}

	/**
	 * 验证帐户的签名是否合法
	 * @throws IOException 
	 */
	public void verify() throws IOException {
		
		byte[] sign1 = signs[0];
		byte[] sign2 = signs[1];
		
		signs = null;
		
		ECKey key1 = ECKey.fromPublicOnly(mgPubkeys[0]);
		ECKey key2 = ECKey.fromPublicOnly(mgPubkeys[1]);
		
		byte[] hash = Sha256Hash.of(serialize()).getBytes();
		
		if(!key1.verify(hash, sign1) || !key2.verify(hash, sign2)) {
			throw new VerificationException("account verify fail");
		}
	}
	
	public AccountType getAccountType() {
		return accountType;
	}
	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public byte[] getBody() {
		return body;
	}
	public void setBody(byte[] body) {
		this.body = body;
	}

	public byte[][] getMgPubkeys() {
		return mgPubkeys;
	}

	public void setMgPubkeys(byte[][] mgPubkeys) {
		this.mgPubkeys = mgPubkeys;
	}

	public byte[][] getTrPubkeys() {
		return trPubkeys;
	}

	public void setTrPubkeys(byte[][] trPubkeys) {
		this.trPubkeys = trPubkeys;
	}

	public byte[][] getSigns() {
		return signs;
	}

	public void setSigns(byte[][] signs) {
		this.signs = signs;
	}

	public byte[] getPriSeed() {
		return priSeed;
	}

	public void setPriSeed(byte[] priSeed) {
		this.priSeed = priSeed;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}
}
