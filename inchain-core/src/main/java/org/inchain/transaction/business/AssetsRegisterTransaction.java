package org.inchain.transaction.business;

import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * 资产登记
 */
public class AssetsRegisterTransaction extends BaseCommonlyTransaction {

	//资产名称
	private byte[] name;
	//资产描述
	private byte[] description;
	//资产代号
	private byte[] code;
	//资产图标
	private byte[] logo;

	public AssetsRegisterTransaction(NetworkParams network) {
		super(network);
		type = Definition.TYPE_ASSETS_REGISTER;
	}

	public AssetsRegisterTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }

	public AssetsRegisterTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	public AssetsRegisterTransaction(NetworkParams params, byte[] name, byte[] description, byte[] code, byte[] logo, byte[] remark)  throws ProtocolException{
		super(params);
		this.name = name;
		this.description = description;
		this.code = code;
		this.logo = logo;
		this.remark = remark;
		type = Definition.TYPE_ASSETS_REGISTER;
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		//别名不能超过30 字节
		if(name == null) {
			throw new VerificationException("资产名称不能为空");
		}
		if(name.length > 30) {
			throw new VerificationException("资产名称不能超过30字节");
		}
		//资产描述不能为空
		if(name == null) {
			throw new VerificationException("资产名称不能为空");
		}
		//资产描述不能超过500字节
		if(description.length > 500) {
			throw new VerificationException("资产描述不能超过500字节");
		}
		//资产编码不能为空
		if(code == null) {
			throw new VerificationException("资产编码不能为空");
		}
		//资产编码不能超过20字节
		if(code.length > 20) {
			throw new VerificationException("资产编码不能超过20字节");
		}
		//资产图标不能为空
		if(logo == null) {
			throw new VerificationException("资产图标不能为空");
		}
		//资产图标不能超过500字节
		if(logo.length > 500) {
			throw new VerificationException("资产图标不能超过500字节");
		}
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {

		stream.write(new VarInt(name.length).encode());
		stream.write(name);

		stream.write(new VarInt(description.length).encode());
		stream.write(description);

		stream.write(new VarInt(code.length).encode());
		stream.write(code);

		stream.write(new VarInt(logo.length).encode());
		stream.write(logo);

	}
	
	@Override
	protected void parseBody() throws ProtocolException {
		name = readBytes((int)readVarInt());
		description = readBytes((int)readVarInt());
		code = readBytes((int)readVarInt());
		logo = readBytes((int)readVarInt());
	}

	public byte[] getName() {
		return name;
	}

	public void setName(byte[] name) {
		this.name = name;
	}

	public byte[] getDescription() {
		return description;
	}

	public void setDescription(byte[] description) {
		this.description = description;
	}

	public byte[] getCode() {
		return code;
	}

	public void setCode(byte[] code) {
		this.code = code;
	}

	public byte[] getLogo() {
		return logo;
	}

	public void setLogo(byte[] logo) {
		this.logo = logo;
	}

	@Override
	public String toString() {
		return "AssetsRegisterTransaction{" +
				"name=" + Arrays.toString(name) +
				", description=" + Arrays.toString(description) +
				", no=" + Arrays.toString(code) +
				", logo=" + Arrays.toString(logo) +
				'}';
	}
}
