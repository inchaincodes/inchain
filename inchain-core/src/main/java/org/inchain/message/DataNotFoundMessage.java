package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;

/**
 * 数据没有找到消息，用户回应GetDatasMessage
 * @author ln
 *
 */
public class DataNotFoundMessage extends Message {
	
	private Sha256Hash hash;
	
	public DataNotFoundMessage(NetworkParams network, Sha256Hash hash) {
        super();
        this.hash = hash;
    }
    
	public DataNotFoundMessage(NetworkParams network, byte[] payload) throws ProtocolException {
    	this(network, payload, 0);
    }
	
	public DataNotFoundMessage(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
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

	@Override
	public String toString() {
		return "DataNotFoundMessage [hash=" + hash + "]";
	}
}
