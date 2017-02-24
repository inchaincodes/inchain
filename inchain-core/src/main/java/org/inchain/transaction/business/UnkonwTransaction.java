package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;

/**
 * 未知的交易，用于老版本兼容新协议
 * @author ln
 *
 */
public class UnkonwTransaction extends CommonlyTransaction {

	private byte[] content;
	
	public UnkonwTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params);
		VarInt varint = new VarInt(payloadBytes, offset + 1);
        this.content = Arrays.copyOfRange(payloadBytes, 0, (int)(offset + 1 + varint.getOriginalSizeInBytes() + varint.value));
        length = content.length;
	}

	@Override
	public void verfify() throws VerificationException {
	}
	
	@Override
	public void verfifyScript() {
	}
	
	@Override
	protected void parse() throws ProtocolException {
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(content);
	}
}
