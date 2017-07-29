package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Hex;

/**
 * 防伪码转让，相当于商品所有权的转让
 * @author ln
 *
 */
public class AntifakeTransferTransaction extends CommonlyTransaction {

	//防伪码
	private byte[] antifakeCode;
	//接收人
	private byte[] receiveHashs;
	
	public AntifakeTransferTransaction(NetworkParams network) {
		super(network);
		
		type = Definition.TYPE_ANTIFAKE_TRANSFER;
	}
	
	public AntifakeTransferTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public AntifakeTransferTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		if(antifakeCode == null || antifakeCode.length != 20) {
			throw new VerificationException("防伪码不正确");
		}
		
		if(receiveHashs == null || receiveHashs.length != Address.HASH_LENGTH) {
			throw new VerificationException("接收人不正确");
		}
		
		//备注不能超过100 byte
		if(remark != null && remark.length > Configure.MAX_REMARK_LEN) {
			throw new VerificationException("备注不能超过100字节");
		}
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		stream.write(antifakeCode);
		stream.write(receiveHashs);
	}
	
	@Override
	protected void parseBody() throws ProtocolException {
		antifakeCode = readBytes(20);
		receiveHashs = readBytes(Address.HASH_LENGTH);
	}

	public byte[] getAntifakeCode() {
		return antifakeCode;
	}

	public void setAntifakeCode(byte[] antifakeCode) {
		this.antifakeCode = antifakeCode;
	}

	public byte[] getReceiveHashs() {
		return receiveHashs;
	}
	
	public byte[] getReceiveHash160() {
		return Arrays.copyOfRange(receiveHashs, 1, Address.LENGTH + 1);
	}

	public void setReceiveHashs(byte[] receiveHashs) {
		this.receiveHashs = receiveHashs;
	}
	
	public String getReceiveAddress() {
		return Address.fromHashs(network, receiveHashs).getBase58();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AntifakeTransferTransaction [antifakeCode=");
		builder.append(Hex.encode(antifakeCode));
		builder.append(", receiveHashs=");
		builder.append(Address.fromHashs(network, receiveHashs).getBase58());
		builder.append("]");
		return builder.toString();
	}
}
