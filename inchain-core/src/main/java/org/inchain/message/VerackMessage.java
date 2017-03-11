package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;

import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

public class VerackMessage extends Message {
	
	private long time;
	private long nonce;

	public VerackMessage(NetworkParams params, byte[] payload) throws ProtocolException {
        super(params, payload, 0);
    }
	
	public VerackMessage(NetworkParams params, long time, long nonce) throws UnknownHostException {
		super(params);
		this.time = time;
		this.nonce = nonce;
	}

	@Override
	protected void parse() throws ProtocolException {
		time = readInt64();
		nonce = readInt64();
		length = cursor;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		Utils.int64ToByteStreamLE(time, stream);
		Utils.int64ToByteStreamLE(nonce, stream);
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
	}
	
}
