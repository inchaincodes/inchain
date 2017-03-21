package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 未知的交易，用于老版本兼容新协议
 * @author ln
 *
 */
public class UnkonwTransaction extends CommonlyTransaction {

	private byte[] content;
	
	public UnkonwTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params);
		long bodyLength = Utils.readUint32(payloadBytes, 1);
        this.content = Arrays.copyOfRange(payloadBytes, 0, (int)bodyLength);
        length = content.length;
	}

	@Override
	public void verify() throws VerificationException {
	}
	
	@Override
	public void verifyScript() {
	}
	
	@Override
	protected void parse() throws ProtocolException {
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(content);
	}
}
