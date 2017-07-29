package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;

/**
 * 防伪码流转信息交易
 * @author ln
 *
 */
public class CirculationTransaction extends CommonlyTransaction {
	/**
	 * 子账户最多添加流转信息上限
	 */
	public final static int SUB_ACCOUNT_MAX_SIZE = 5;
	
	
	/**
	 * tag的最大限制，单位字节
	 */
	public final static int TAG_MAX_SIZE = 100;
	/**
	 * 内容的最大限制，单位字节
	 */
	public final static int CONTENT_MAX_SIZE = 300;
	
	//防伪码
	private byte[] antifakeCode;
	//标签
	private byte[] tag;
	//内容
	private byte[] content;

	public CirculationTransaction(NetworkParams network) {
		super(network);
		
		type = Definition.TYPE_ANTIFAKE_CIRCULATION;
	}
	
	public CirculationTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public CirculationTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		if(antifakeCode == null) {
			throw new VerificationException("缺少防伪码");
		}
		
		if(antifakeCode.length != 20) {
			throw new VerificationException("防伪码不正确");
		}
		
		if(tag == null) {
			throw new VerificationException("缺少标签");
		}
		//标签不能超过限制
		if(tag.length > TAG_MAX_SIZE) {
			throw new VerificationException("标签不能超过" + TAG_MAX_SIZE + "字节");
		}
		
		
		if(content == null) {
			throw new VerificationException("缺少说明");
		}
		
		//内容不能超过限制
		if(tag.length > CONTENT_MAX_SIZE) {
			throw new VerificationException("内容不能超过" + CONTENT_MAX_SIZE + "字节");
		}
	}

	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		stream.write(new VarInt(antifakeCode.length).encode());
		stream.write(antifakeCode);
		
		stream.write(new VarInt(tag.length).encode());
		stream.write(tag);
		
		stream.write(new VarInt(content.length).encode());
		stream.write(content);
	}
	
	@Override
	protected void parseBody() throws ProtocolException {
		antifakeCode = readBytes((int) readVarInt());
		tag = readBytes((int) readVarInt());
		content = readBytes((int) readVarInt());
	}

	public byte[] getAntifakeCode() {
		return antifakeCode;
	}

	public void setAntifakeCode(byte[] antifakeCode) {
		this.antifakeCode = antifakeCode;
	}

	public byte[] getTag() {
		return tag;
	}

	public void setTag(byte[] tag) {
		this.tag = tag;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CirculationTransaction [antifakeCode=");
		builder.append(Hex.encode(antifakeCode));
		builder.append(", tag=");
		try {
			builder.append(new String(tag, "utf-8"));
			builder.append(", content=");
			builder.append(new String(content, "utf-8"));
		} catch (UnsupportedEncodingException e) {
		}
		builder.append("]");
		return builder.toString();
	}
}
