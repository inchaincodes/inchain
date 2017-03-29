package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Address;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;

/**
 * 认证账户关联子账户
 * 关联的账户必须是普通账户
 * 关联账户的权限是增加商家发行的验证流转信息
 * @author ln
 *
 */
public class RelevanceSubAccountTransaction extends CommonlyTransaction {

	//关联账户
	private byte[] relevanceHash160;
	//别名
	private byte[] alias;
	//描述
	private byte[] content;
	
	public RelevanceSubAccountTransaction(NetworkParams network, byte[] relevanceHash160, byte[] alias, byte[] content) {
		super(network);
		this.relevanceHash160 = relevanceHash160;
		this.alias = alias;
		this.content = content;
	}

	public RelevanceSubAccountTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public RelevanceSubAccountTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(relevanceHash160);
		
		if(alias == null) {
			stream.write(0);
		} else {
			stream.write(new VarInt(alias.length).encode());
			stream.write(alias);
		}
		
		if(content == null) {
			stream.write(0);
		} else {
			stream.write(new VarInt(content.length).encode());
			stream.write(content);
		}
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		relevanceHash160 = readBytes(20);
		
		alias = readBytes((int)readVarInt());
		content = readBytes((int)readVarInt());
		
		length = cursor - offset;
	}

	public byte[] getRelevanceHash160() {
		return relevanceHash160;
	}

	public void setRelevanceHash160(byte[] relevanceHash160) {
		this.relevanceHash160 = relevanceHash160;
	}
	
	public byte[] getAlias() {
		return alias;
	}

	public void setAlias(byte[] alias) {
		this.alias = alias;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public Address getAddress() {
		return new Address(network, relevanceHash160);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RelevanceSubAccountTransaction [relevanceHash160=");
		builder.append(Hex.encode(relevanceHash160));
		builder.append(", alias=");
		builder.append(new String(alias));
		builder.append(", content=");
		builder.append(new String(content));
		builder.append("]");
		return builder.toString();
	}
}
