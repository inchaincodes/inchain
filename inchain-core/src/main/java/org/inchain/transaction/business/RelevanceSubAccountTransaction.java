package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;

/**
 * 认证账户关联子账户
 * 关联的账户必须是普通账户
 * 关联账户的权限是增加商家发行的验证流转信息
 * @author ln
 *
 */
public class RelevanceSubAccountTransaction extends CommonlyTransaction {

	//关联账户
	private byte[] relevanceHashs;
	//别名
	private byte[] alias;
	//描述
	private byte[] content;
	
	public RelevanceSubAccountTransaction(NetworkParams network, byte[] relevanceHashs, byte[] alias, byte[] content) {
		super(network);
		this.relevanceHashs = relevanceHashs;
		this.alias = alias;
		this.content = content;
		
		type = Definition.TYPE_RELEVANCE_SUBACCOUNT;
	}

	public RelevanceSubAccountTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public RelevanceSubAccountTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		if(relevanceHashs == null || relevanceHashs.length != Address.HASH_LENGTH) {
			throw new VerificationException("地址不正确");
		}
		
		if(alias == null) {
			throw new VerificationException("标题不能为空");
		}
		
		if(content == null) {
			throw new VerificationException("说明不能为空");
		}
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(relevanceHashs);
		
		stream.write(new VarInt(alias.length).encode());
		stream.write(alias);
		
		stream.write(new VarInt(content.length).encode());
		stream.write(content);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		relevanceHashs = readBytes(Address.HASH_LENGTH);
		
		alias = readBytes((int)readVarInt());
		content = readBytes((int)readVarInt());
		
		length = cursor - offset;
	}
	
	public byte[] getRelevanceHashs() {
		return relevanceHashs;
	}

	public void setRelevanceHashs(byte[] relevanceHashs) {
		this.relevanceHashs = relevanceHashs;
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
		return Address.fromHashs(network, relevanceHashs);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RelevanceSubAccountTransaction [relevanceHashs=");
		builder.append(Address.fromHashs(network, relevanceHashs).getBase58());
		builder.append(", alias=");
		builder.append(new String(alias));
		builder.append(", content=");
		builder.append(new String(content));
		builder.append("]");
		return builder.toString();
	}
}
