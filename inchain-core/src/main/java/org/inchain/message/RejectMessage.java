package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;

/**
 * 拒绝消息
 * @author ln
 *
 */
public class RejectMessage extends Message {
	
	private Sha256Hash hash;
	
	public RejectMessage(NetworkParams network, Sha256Hash hash) {
        super();
        this.hash = hash;
    }
    
	public RejectMessage(NetworkParams network, byte[] payload) throws ProtocolException {
    	this(network, payload, 0);
    }
	
	public RejectMessage(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
    	super(network, payload, offset);
    }
	
	@Override
	protected void parse() throws ProtocolException {
		hash = readHash();
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(hash.getReversedBytes());
	}
}
