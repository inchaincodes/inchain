package org.inchain.account;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;

import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.crypto.EncryptedData;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账户
 * @author ln
 *
 */
public class Account implements Cloneable {

	private static Logger log = LoggerFactory.getLogger(Account.class);

	private NetworkParams network;

	//账户类型
	private int accountType;
	//帐户状态
	private byte status;
	//帐户地址
	private Address address;
	//认证账户签发上级hash160
	private byte[] supervisor;
	private int level;
	//私匙种子
	private byte[] priSeed;
	//管理公匙
	private byte[][] mgPubkeys;
	//交易公匙
	private byte[][] trPubkeys;
	//帐户主体
	private AccountBody body;
	//帐户信息签名
	private byte[][] signs;

	//解密后的管理私钥
	private ECKey[] mgEckeys;
	//解密后的交易私钥
	private ECKey[] trEckeys;

	//eckey，只有系统类型的账户才会有
	private ECKey ecKey;

	//认证账户对应最新的交易信息
	private Transaction accountTransaction;
	private Sha256Hash txhash;

	public Account() {
	}

	public Account(NetworkParams network) {
		this.network = network;
		this.supervisor = new byte[20];
		this.level = 0;
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
			if(address.getVersion() == network.getCertAccountVersion() ) {
				bos.write(supervisor);
				bos.write(level);
			}

			if(priSeed !=null) {
				bos.write(priSeed.length);
				bos.write(priSeed);
			}else{
				bos.write(0);
			}


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
				byte[] bodyContent = body.serialize();
				Utils.uint32ToByteStreamLE(bodyContent.length, bos);
				bos.write(bodyContent);
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

		account.network = network;

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
			if(length>0) {
				account.setPriSeed(readBytes(cursor, length, datas));
				cursor += length;
			}else {
				account.setPriSeed(new byte[0]);
			}

			//公匙
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] pubkey = readBytes(cursor, length, datas);
			cursor += length;
			account.setMgPubkeys(new byte[][] {pubkey});

			//主体
			cursor += 4;

			//签名
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] sign1 = readBytes(cursor, length, datas);
			cursor += length;

			account.setSigns(new byte[][] {sign1});

			//eckey
			account.resetKey();
		} else if(type == network.getCertAccountVersion()) {
			byte[] supervisor = readBytes(cursor, 20, datas);
			account.setSupervisor(supervisor);
			cursor += 20;

			int level = datas[cursor] & 0xff;
			account.setlevel(level);
			cursor++;
			//私匙种子
			int length = datas[cursor] & 0xff;
			cursor ++;
			if(length>0) {
				account.setPriSeed(readBytes(cursor, length, datas));
				cursor += length;
			}else {
			account.setPriSeed(new byte[0]);
			}

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
			account.setTrPubkeys(new byte[][] {trPubkey1});
			/*
			length = datas[cursor] & 0xff;
			cursor ++;
			byte[] trPubkey2 = readBytes(cursor, length, datas);
			cursor += length;
			account.setTrPubkeys(new byte[][] {trPubkey1, trPubkey2});
			*/
			account.setTrPubkeys(new byte[][] {trPubkey1});

			//主体
			length = (int) Utils.readUint32(datas, cursor);
			cursor += 4;
			account.setBody(new AccountBody(readBytes(cursor, length, datas)));
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

	public void resetKey() {
		if(isCertAccount()) {
			mgEckeys = null;
			trEckeys = null;
		} else {
			resetKey(null);
		}
	}

	public void resetKey(String password) {
		if(accountType != network.getSystemAccountVersion()) {
			return;
		}
		byte[] pubkey = mgPubkeys[0];
		if(!isEncrypted()) {
			//未加密的账户
			setEcKey(ECKey.fromPrivate(new BigInteger(getPriSeed())));
		} else {
			byte[] iv = null;
			if(password == null) {
				iv = Arrays.copyOf(Sha256Hash.hash(pubkey), 16);
			} else {
				iv = Arrays.copyOf(AccountTool.genPrivKey1(pubkey, password.getBytes()).toByteArray(), 16);
			}
			//加密账户
			if(ecKey == null || ecKey.getEncryptedPrivateKey() == null) {
				setEcKey(ECKey.fromEncrypted(new EncryptedData(iv, getPriSeed()), pubkey));
			} else {
				EncryptedData encryptData = ecKey.getEncryptedPrivateKey();
				encryptData.setInitialisationVector(iv);
				setEcKey(ECKey.fromEncrypted(encryptData, pubkey));
			}
		}
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
		if (priSeed == null)
			size+=1;
		else
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

		size += body == null? 4:body.serialize().length + 4;

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

	/**
	 * 签名账户
	 * @throws IOException
	 */
	public void signAccount() throws IOException {
		if(isCertAccount()) {
			signAccount(mgEckeys[0], mgEckeys[1]);
		} else {
			signAccount(ecKey, null);
		}
	}

	/**
	 * 签名账户
	 * @param mgkey1
	 * @param mgkey2
	 * @throws IOException
	 */
	public void signAccount(ECKey mgkey1, ECKey mgkey2) throws IOException {
		signs = null;
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
	public ECKey[] decryptionTr(String trPw) {

		ECKey seedPri = ECKey.fromPublicOnly(priSeed);
		byte[] seedPribs = seedPri.getPubKey(false);

		BigInteger trPri1 = AccountTool.genPrivKey1(seedPribs, trPw.getBytes());
		BigInteger trPri2 = AccountTool.genPrivKey2(seedPribs, trPw.getBytes());
		ECKey trkey1 = ECKey.fromPrivate(trPri1);
		ECKey trkey2 = ECKey.fromPrivate(trPri2);

		if(trPubkeys.length==2){
		//验证密码是否正确
			if(Arrays.equals(trkey1.getPubKey(true), trPubkeys[0]) && Arrays.equals(trkey2.getPubKey(true), trPubkeys[1])) {
				trEckeys = new ECKey[]{trkey1, trkey2};
				return trEckeys;
			}
		}
		if(trPubkeys.length==1){
			if(Arrays.equals(trkey1.getPubKey(true), trPubkeys[0])) {
				trEckeys = new ECKey[]{trkey1};
				return trEckeys;
			}
		}
		log.error("解密交易私钥时出错，密码不正确");
		return null;
	}

	/**
	 * 账户是否已加密
	 * @return boolean
	 */
	public boolean isEncrypted() {
		if(accountType == network.getSystemAccountVersion()) {
			//普通账户
			//没有私钥也代表已加密
			if(ecKey == null) {
				return false;
			}
			try {
				ecKey.getPrivKey();
			} catch (Exception e) {
				return true;
			}
			//公钥
			byte[] pubkey = mgPubkeys[0];
			//公钥相同则代表未加密
			if(Arrays.equals(ecKey.getPubKey(), pubkey)) {
				return false;
			} else {
				return true;
			}
		} else {
			//认证账户，调用isEncryptedOfMg和isEncryptedOfTr判断
			return true;
		}
	}

	/**
	 * 认证账户管理私钥是否已加密
	 * @return boolean
	 */
	public boolean isEncryptedOfMg() {
		Utils.checkState(accountType == network.getCertAccountVersion());
		//认证账户
		if(mgEckeys == null) {
			return true;
		} else {
			boolean result = false;
			for (int i = 0; i < mgEckeys.length; i++) {
				ECKey key = mgEckeys[i];
				if(!Arrays.equals(key.getPubKey(), mgPubkeys[i])) {
					result = true;
					break;
				}
			}
			return result;
		}
	}

	/**
	 * 认证账户交易私钥是否已加密
	 * @return boolean
	 */
	public boolean isEncryptedOfTr() {
		Utils.checkState(accountType == network.getCertAccountVersion());
		//认证账户
		if(trEckeys == null) {
			return true;
		} else {
			boolean result = false;
			for (int i = 0; i < trEckeys.length; i++) {
				ECKey key = trEckeys[i];
				if(!Arrays.equals(key.getPubKey(), trPubkeys[i])) {
					result = true;
					break;
				}
			}
			return result;
		}
	}

	/**
	 * 账户是否是认证账户
	 * @return boolean
	 */
	public boolean isCertAccount() {
		return accountType == network.getCertAccountVersion();
	}

	/**
	 * 认证账户，获取账户信息最新交易
	 * @return Transaction
	 */
	public Transaction getAccountTransaction() {
		return accountTransaction;
	}

	/**
	 * 设置认证账户信息对应最新交易
	 * @param accountTransaction
	 */
	public void setAccountTransaction(Transaction accountTransaction) {
		this.accountTransaction = accountTransaction;
		if(accountTransaction!=null){
			this.txhash = accountTransaction.getHash();
		}
	}

	@Override
	public Account clone() throws CloneNotSupportedException {
		return (Account) super.clone();
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
		this.accountType = address.getVersion();
	}


	public AccountBody getBody() {
		return body;
	}

	public void setBody(AccountBody body) {
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

	public void setEcKey(ECKey ecKey) {
		this.ecKey = ecKey;
	}

	public ECKey getEcKey() {
		return ecKey;
	}

	public NetworkParams getNetwork() {
		return network;
	}
	public void setNetwork(NetworkParams network) {
		this.network = network;
	}

	public ECKey[] getMgEckeys() {
		return mgEckeys;
	}

	public void setMgEckeys(ECKey[] mgEckeys) {
		this.mgEckeys = mgEckeys;
	}

	public ECKey[] getTrEckeys() {
		return trEckeys;
	}

	public void setTrEckeys(ECKey[] trEckeys) {
		this.trEckeys = trEckeys;
	}

	public Sha256Hash getTxhash() {
		return txhash;
	}

	public void setTxhash(Sha256Hash txhash) {
		this.txhash = txhash;
	}

	public void setSupervisor(byte[] supervisor){
		this.supervisor = supervisor;
	}

	public byte[] getSupervisor(){
		return this.supervisor;
	}

	public void setlevel(int level){
		this.level=level;
	}

	public int getLevel(){
		return this.level;
	}
}
