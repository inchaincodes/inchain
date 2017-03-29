package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;

/**
 * 修改账户别名，需要消耗5个信用点
 * @author ln
 *
 */
public class UpdateAliasTransaction extends CommonlyTransaction {

	private byte[] alias;
	
	public UpdateAliasTransaction(NetworkParams network) {
		super(network);
		type = Definition.TYPE_UPDATE_ALIAS;
	}
	
	public UpdateAliasTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public UpdateAliasTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		//别名不能超过30 字节
		if(alias == null) {
			throw new VerificationException("别名不能为空");
		}
		if(alias.length > 30) {
			throw new VerificationException("别名不能超过30字节");
		}
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(new VarInt(alias.length).encode());
		stream.write(alias);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		alias = readBytes((int)readVarInt());
		length = cursor - offset;
	}

	public byte[] getAlias() {
		return alias;
	}

	public void setAlias(byte[] alias) {
		this.alias = alias;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RegAliasTransaction [alias=");
		builder.append(new String(alias));
		builder.append("]");
		return builder.toString();
	}
}
