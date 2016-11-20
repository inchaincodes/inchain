package org.inchain.core;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.utils.Base58;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

public class SignTest {

	public static void main(String[] args) throws NoSuchAlgorithmException {
		//生成私钥
		SecureRandom random = SecureRandom.getInstanceStrong();
		byte[] priBytes = new byte[32];
		random.nextBytes(priBytes);
//		BigInteger priNumber = new BigInteger(priBytes);
		BigInteger priNumber = new BigInteger(Hex.decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725"));
		System.out.println("pri number : "+priNumber);
		//椭圆曲线加密生成公匙
		ECKey key = ECKey.fromPrivate(new BigInteger(Hex.decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")));
		//
		System.out.println("prikey : "+key.getPrivateKeyAsHex());
		String pubkey = key.getPublicKeyAsHex();
		System.out.println("pubkey : "+pubkey);
		
		byte[] hash160 = Utils.sha256hash160(key.getPubKey());
		
		System.out.println("hash160 : "+Hex.encode(hash160));
		
		
		
		byte[] hash160withver = new byte[hash160.length + 1];
		hash160withver[0] = 0;
		
		System.arraycopy(hash160, 0, hash160withver, 1, hash160.length);
		
		byte[] addbytes = Sha256Hash.hashTwice(hash160withver);

		System.out.println(Hex.encode(addbytes));
		
		byte[] ta = new byte[4];
		System.arraycopy(addbytes, 0, ta, 0, 4);
		System.out.println(Hex.encode(ta));
		
		byte[] address = new byte[hash160withver.length + ta.length];
		System.arraycopy(hash160withver, 0, address, 0, hash160withver.length);
		System.arraycopy(ta, 0, address, hash160withver.length, ta.length);
		
		System.out.println(Base58.encode(address));
		
		//地址生成测试完成//
		System.out.println("==============address test end ===========");
		
		//测试签名
		
		//待签名的消息
		String msg = "this is a test messsage!";
		
		Sha256Hash hash = Sha256Hash.twiceOf(msg.getBytes());
		
		ECDSASignature signature = key.sign(hash);
		
        byte[] signbs = signature.encodeToDER();
        
        System.out.println(Hex.encode(signbs));
        
        System.out.println(new String(Base64.getEncoder().encode(signbs)));
        
        key = ECKey.fromPublicOnly(Hex.decode("0250863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b2352"));
        
        boolean result = key.verify(hash.getBytes(), Hex.decode("304402203879629cd7c6b4233b0ec5edf3229963ec52c63b30bc1a03b2eb8b6a431f94a802201edabbc70ed2251b98920f11dbbc27e8c353fc761fad32eb3a51de49187005ac"));
        System.out.println("result is "+result);
        
	}
}
