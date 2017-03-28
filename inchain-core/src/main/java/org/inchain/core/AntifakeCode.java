package org.inchain.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.inchain.core.exception.VerificationException;
import org.inchain.utils.Base58;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 防伪码信息
 * @author ln
 *
 */
public class AntifakeCode {

	//防伪码
	private byte[] antifakeCode;
	//验证码
	private long verifyCode;
	
	public AntifakeCode() {
	}
	
	public AntifakeCode(byte[] antifakeCode, long verifyCode) {
		this.antifakeCode = antifakeCode;
		this.verifyCode = verifyCode;
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
			return parse(Base58.decode(content.trim()));
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
			stream.write(antifakeCode);
			Utils.int64ToByteStreamLE(verifyCode, stream);
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
		//防伪 码
		byte[] antifakeCode = null;
		//商家的签名信息
		long verifyCode = 0;
		
		try {
			int cusor = 0;
			//防伪码内容
			byte[] antifakeCodeByte = new byte[20];
			System.arraycopy(antifakeCodeContent, cusor, antifakeCodeByte, 0, antifakeCodeByte.length);
			cusor += antifakeCodeByte.length;
			
			antifakeCode = antifakeCodeByte;

			//验证码内容
			verifyCode = Utils.readInt64(antifakeCodeContent, cusor);
		} catch (Exception e) {
			throw new VerificationException("防伪码不正确");
		}
		
		if(antifakeCode == null || verifyCode == 0l) {
			throw new VerificationException("防伪码错误");
		}
		
		return new AntifakeCode(antifakeCode, verifyCode);
	}

	public byte[] getAntifakeCode() {
		return antifakeCode;
	}

	public void setAntifakeCode(byte[] antifakeCode) {
		this.antifakeCode = antifakeCode;
	}

	public long getVerifyCode() {
		return verifyCode;
	}

	public void setVerifyCode(long verifyCode) {
		this.verifyCode = verifyCode;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AntifakeCode [antifakeCode=");
		builder.append(Hex.encode(antifakeCode));
		builder.append(", verifyCode=");
		builder.append(verifyCode);
		builder.append("]");
		return builder.toString();
	}
}
