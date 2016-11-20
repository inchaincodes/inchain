package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParameters;
import org.inchain.utils.Utils;

/**
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class PingMessage extends Message {
    private long nonce;
    
    public PingMessage(NetworkParameters params, byte[] payloadBytes) throws ProtocolException {
        super(params, payloadBytes, 0);
    }
    
    public PingMessage(long nonce) {
        this.nonce = nonce;
    }
    
    @Override
    public void serializeToStream(OutputStream stream) throws IOException {
        Utils.int64ToByteStreamLE(nonce, stream);
    }

    @Override
    protected void parse() throws ProtocolException {
        nonce = readInt64();
        length = 8;
    }
    
    public long getNonce() {
        return nonce;
    }

	@Override
	public String toString() {
		return "PingMessage [nonce=" + nonce + "]";
	}
}
