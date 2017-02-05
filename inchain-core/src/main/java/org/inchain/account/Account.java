package org.inchain.account;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账户
 * @author ln
 *
 */
public class Account {
	
	private static Logger log = LoggerFactory.getLogger(Account.class);
	
	//账户类型
	private int accountType;
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
	
	//解密后的管理私钥
	private ECKey[] mgEckeys;
	//解密后的交易私钥
	private ECKey[] trEckeys;
	
	public Account() {
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
			
			bos.write(address.getVersion());//类型
			bos.write(address.getHash160());
			
			bos.write(priSeed.length);
			bos.write(priSeed);
			

			if(mgPubkeys != null) {
				for (byte[] mgPubkey : mgPubkeys) {
					bos.write(mgPubkey.length);
					bos.write(mgPubkey);
				}
			}
			
			if(trPubkeys != null) {
				for (byte[] trPubkey : trPubkeys) {
					bos.write(trPubkey.length);
					bos.write(trPubkey);
				}
			}
			//帐户主体
			if(body != null) {
				Utils.uint32ToByteStreamLE(body.length, bos);
				bos.write(body);
			} else {
				Utils.uint32ToByteStreamLE(0l, bos);
			}
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
	public static Account parse(byte[] datas, NetworkParams network) {
		return parse(datas, 0, network);
	}
	
	/**
	 * 反序列化
	 * @param datas
	 * @param offset
	 * @return Account
	 */
	public static Account parse(byte[] datas, int offset, NetworkParams network) {
		Account account = new Account();
		
		int cursor = offset;
		//状态
		account.setStatus(datas[cursor]);
		cursor ++;
		//帐户类型
		int type = datas[cursor] & 0xff;
		account.setAccountType(type);
		cursor ++;
		
		//hash 160
		byte[] hash160 = readBytes(cursor, 20, datas);
		Address address = Address.fromP2PKHash(network, type, hash160);
		account.setAddress(address);
		cursor += 20;
		
		if(type == network.getSystemAccountVersion()) {
			//私匙
			int length = datas[cursor] & 0xff;
			cursor ++;
			account.setPriSeed(readBytes(cursor, length, datas));
			cursor += length;
			
			//公匙
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] mgPubkey1 = readBytes(cursor, length, datas);
			cursor += length;
			account.setMgPubkeys(new byte[][] {mgPubkey1});
			
			//主体
			cursor += 4;
			
			//签名
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] sign1 = readBytes(cursor, length, datas);
			cursor += length;
			
			account.setSigns(new byte[][] {sign1});
			
		} else if(type == network.getCertAccountVersion()) {
			//私匙种子
			int length = datas[cursor] & 0xff;
			cursor ++;
			account.setPriSeed(readBytes(cursor, length, datas));
			cursor += length;
			
			//帐户管理公匙
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] mgPubkey1 = readBytes(cursor, length, datas);
			cursor += length;
			
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] mgPubkey2 = readBytes(cursor, length, datas);
			
			account.setMgPubkeys(new byte[][] {mgPubkey1, mgPubkey2});
			cursor += length;
			
			//交易公匙
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] trPubkey1 = readBytes(cursor, length, datas);
			cursor += length;
			
			length = datas[cursor] & 0xff;
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
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] sign1 = readBytes(cursor, length, datas);
			cursor += length;
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] sign2 = readBytes(cursor, length, datas);
			cursor += length;
			
			account.setSigns(new byte[][] {sign1, sign2});
		}
		
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

		if(trPubkeys != null) {
			for (byte[] mgPubkey : mgPubkeys) {
				size += mgPubkey.length + 1;
			}
		}
		if(trPubkeys != null) {
			for (byte[] trPubkey : trPubkeys) {
				size += trPubkey.length + 1;
			}
		}
		
		size += body == null? 4:body.length + 4;
		
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
		if(mgkey1 == null && mgkey2 == null) {
			return;
		} else if(mgkey1 != null && mgkey2 == null) {
			//用户帐户管理私匙签名
			Sha256Hash hash = Sha256Hash.of(serialize());
			ECDSASignature signature1 = mgkey1.sign(hash);
			//签名结果
	        byte[] signbs1 = signature1.encodeToDER();
	        signs = new byte[][] {signbs1};
		} else if(mgkey1 != null && mgkey2 != null) {
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
	}

	/**
	 * 验证帐户的签名是否合法
	 * @throws IOException 
	 */
	public void verify() throws IOException {

		byte[][] tempSigns = signs;
		signs = null;
		
		for (int i = 0; i < tempSigns.length; i++) {
			byte[] sign = tempSigns[i];
			ECKey key1 = ECKey.fromPublicOnly(mgPubkeys[i]);
			byte[] hash = Sha256Hash.of(serialize()).getBytes();
			
			if(!key1.verify(hash, sign)) {
				throw new VerificationException("account verify fail");
			}
		}
		signs = tempSigns;
	}
	
	/**
	 * 解密管理私钥
	 * @return ECKey[]
	 */
	public ECKey[] decryptionMg(String mgPw) {

		ECKey seedPri = ECKey.fromPublicOnly(priSeed);
		byte[] seedPribs = seedPri.getPubKey(false);
		
		BigInteger mgPri1 = AccountTool.genPrivKey1(seedPribs, mgPw.getBytes());
		BigInteger mgPri2 = AccountTool.genPrivKey2(seedPribs, mgPw.getBytes());
		ECKey mgkey1 = ECKey.fromPrivate(mgPri1);
		ECKey mgkey2 = ECKey.fromPrivate(mgPri2);
		
		//验证密码是否正确
		if(Arrays.equals(mgkey1.getPubKey(true), mgPubkeys[0]) && Arrays.equals(mgkey2.getPubKey(true), mgPubkeys[1])) {
			mgEckeys = new ECKey[] {mgkey1, mgkey2};
			return mgEckeys;
		} else {
			log.error("解密管理私钥时出错，密码不正确");
			return null;
		}
	}
	
	/**
	 * 解密交易私钥
	 * @return ECKey[]
	 */
	public ECKey[] decryptionTr(String mgTr) {

		ECKey seedPri = ECKey.fromPublicOnly(priSeed);
		byte[] seedPribs = seedPri.getPubKey(false);
		
		BigInteger trPri1 = AccountTool.genPrivKey1(seedPribs, mgTr.getBytes());
		BigInteger trPri2 = AccountTool.genPrivKey2(seedPribs, mgTr.getBytes());
		ECKey trkey1 = ECKey.fromPrivate(trPri1);
		ECKey trkey2 = ECKey.fromPrivate(trPri2);
		
		//验证密码是否正确
		if(Arrays.equals(trkey1.getPubKey(true), trPubkeys[0]) && Arrays.equals(trkey2.getPubKey(true), trPubkeys[1])) {
			trEckeys = new ECKey[] {trkey1, trkey2};
			return trEckeys;
		} else {
			log.error("解密交易私钥时出错，密码不正确");
			return null;
		}
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

	public int getAccountType() {
		return accountType;
	}

	public void setAccountType(int accountType) {
		this.accountType = accountType;
	}
}
