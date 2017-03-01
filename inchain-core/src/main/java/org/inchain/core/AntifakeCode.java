package org.inchain.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.utils.Base58;

/**
 * 防伪码信息
 * @author ln
 *
 */
public class AntifakeCode {

	//防伪码hash
	private Sha256Hash antifakeTx;
	//商家账户信息hash
	private Sha256Hash certAccountTx;
	//对防伪码的签名
	private byte[][] signs;
	
	public AntifakeCode() {
	}
	
	public AntifakeCode(Sha256Hash antifakeTx, Sha256Hash certAccountTx, byte[][] signs) {
		this.antifakeTx = antifakeTx;
		this.certAccountTx = certAccountTx;
		this.signs = signs;
	}
	
	/**
	 * base58编码
	 * @return String
	 * @throws VerificationException 
	 */
	public String base58Encode() throws IOException, VerificationException {
		byte[] content = serialize();
		return Base58.encode(content);
	}
	
	/**
	 * base58解码
	 * @return AntifakeCode
	 * @throws VerificationException 
	 */
	public static AntifakeCode base58Decode(String content) throws VerificationException {
		try {
			return parse(Base58.decode(content));
		} catch (Exception e) {
			throw new VerificationException("防伪码不正确");
		}
	}
	
	/**
	 * 序列化
	 * @return byte[]
	 * @throws IOException 
	 */
	public byte[] serialize() throws IOException, VerificationException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			stream.write(antifakeTx.getBytes());
			stream.write(certAccountTx.getBytes());
			for (byte[] sign : signs) {
				stream.write(new VarInt(sign.length).encode());
				stream.write(sign);
			}
			return stream.toByteArray();
		} finally {
			stream.close();
		}
	}
	
	/**
	 * 反序列化
	 * @param antifakeCodeContent
	 * @return AntifakeCode
	 * @throws IOException 
	 */
	public static AntifakeCode parse(byte[] antifakeCodeContent) throws VerificationException {
		//防伪码
		Sha256Hash hash = null;
		//商家账户信息Id
		Sha256Hash certAccountTxHash = null;
		//商家的签名信息
		byte[][] signs = null;
		
		try {
			int cusor = 0;
			//防伪码hash
			byte[] hashByte = new byte[Sha256Hash.LENGTH];
			System.arraycopy(antifakeCodeContent, cusor, hashByte, 0, Sha256Hash.LENGTH);
			
			cusor += Sha256Hash.LENGTH;
			
			//商家账户信息hash
			byte[] certAccountTxHashByte = new byte[Sha256Hash.LENGTH];
			System.arraycopy(antifakeCodeContent, cusor, certAccountTxHashByte, 0, Sha256Hash.LENGTH);
			cusor += Sha256Hash.LENGTH;
			
			//签名长度
			VarInt signLeng = new VarInt(antifakeCodeContent, cusor);
			cusor += signLeng.getOriginalSizeInBytes();
			
			//签名内容
			byte[] sign1 = new byte[(int) signLeng.value];
			System.arraycopy(antifakeCodeContent, cusor, sign1, 0, sign1.length);
			cusor += sign1.length;
			
			//签名长度2
			signLeng = new VarInt(antifakeCodeContent, cusor);
			cusor += signLeng.getOriginalSizeInBytes();
			
			//签名内容
			byte[] sign2 = new byte[(int) signLeng.value];
			System.arraycopy(antifakeCodeContent, cusor, sign2, 0, sign2.length);
			cusor += sign2.length;
			
			hash = Sha256Hash.wrap(hashByte);
			certAccountTxHash = Sha256Hash.wrap(certAccountTxHashByte);
			signs = new byte[][] {sign1, sign2};
		} catch (Exception e) {
			throw new VerificationException("防伪码不正确");
		}
		
		if(hash == null || certAccountTxHash == null || signs == null) {
			throw new VerificationException("防伪码错误");
		}
		
		return new AntifakeCode(hash, certAccountTxHash, signs);
	}
	
	public Sha256Hash getAntifakeTx() {
		return antifakeTx;
	}

	public void setAntifakeTx(Sha256Hash antifakeTx) {
		this.antifakeTx = antifakeTx;
	}

	public Sha256Hash getCertAccountTx() {
		return certAccountTx;
	}

	public void setCertAccountTx(Sha256Hash certAccountTx) {
		this.certAccountTx = certAccountTx;
	}

	public byte[][] getSigns() {
		return signs;
	}

	public void setSigns(byte[][] signs) {
		this.signs = signs;
	}

	@Override
	public String toString() {
		return "AntifakeCode [antifakeTx=" + antifakeTx + ", certAccountTx=" + certAccountTx + ", signs="
				+ Arrays.toString(signs) + "]";
	}
}
