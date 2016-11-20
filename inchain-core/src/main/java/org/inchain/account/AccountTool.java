package org.inchain.account;

import java.math.BigInteger;

import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParameters;
import org.inchain.utils.Utils;

/**
 * 帐户工具
 * @author ln
 *
 */
public final class AccountTool {

	/**
	 * 生成一个新的私匙/公匙对
	 * @return ECKey
	 */
	public final static ECKey newPriKey() {
		return new ECKey();
	}
	
	/**
	 * 生成一个新的地址
	 * @param network
	 * @return Address
	 */
	public final static Address newAddress(NetworkParameters network) {
		return newAddress(network, Address.VERSION_DEFAULT);
	}
	
	public final static Address newAddress(NetworkParameters network, ECKey key) {
		return newAddress(network, Address.VERSION_DEFAULT, key);
	}
	
	public final static Address newAddress(NetworkParameters network, int version) {
		ECKey key = newPriKey();
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddress(NetworkParameters network, int version, ECKey key) {
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddressFromPrikey(NetworkParameters network, int version, BigInteger pri) {
		ECKey key = ECKey.fromPrivate(pri);
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddressFromKey(NetworkParameters network, ECKey key) {
		return Address.fromP2PKHash(network, Address.VERSION_DEFAULT, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddressFromKey(NetworkParameters network, int version, ECKey key) {
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	/**
	 * 根据种子私匙，和密码，生成对应的帐户管理私匙或者交易私匙
	 */
	public final static BigInteger genPrivKey1(byte[] priSeed, byte[] pw) {
		byte[] privSeedSha256 = Sha256Hash.hash(priSeed);
		//取种子私匙的sha256 + pw的sha256，然后相加的结果再sha256为帐户管理的私匙
		byte[] pwSha256 = Sha256Hash.hash(pw);
		//把privSeedSha256 与 pwPwSha256 混合加密
		byte[] pwPriBytes = new byte[privSeedSha256.length + pwSha256.length];
		for (int i = 0; i < pwPriBytes.length; i+=2) {
			int index = i / 2;
			pwPriBytes[index] = privSeedSha256[index];
			pwPriBytes[index+1] = pwSha256[index];
		}
		//生成账户管理的私匙
		return new BigInteger(Sha256Hash.hash(pwPriBytes));
	}
	
	/**
	 * 根据种子私匙，和密码，生成对应的帐户管理私匙或者交易私匙
	 */
	public final static BigInteger genPrivKey2(byte[] priSeed, byte[] pw) {
		byte[] privSeedSha256 = Sha256Hash.hash(priSeed);
		//取种子私匙的sha256 + pw的sha256，然后相加的结果再sha256为帐户管理的私匙
		byte[] pwSha256 = Sha256Hash.hash(pw);
		//把privSeedSha256 与 pwPwSha256 混合加密
		byte[] pwPriBytes = new byte[privSeedSha256.length + pwSha256.length];
		for (int i = 0; i < pwPriBytes.length; i+=2) {
			int index = i / 2;
			pwPriBytes[index] = pwSha256[index];
			pwPriBytes[index+1] = privSeedSha256[index];
		}
		//生成账户管理的私匙
		return new BigInteger(Sha256Hash.hash(pwPriBytes));
	}
}
