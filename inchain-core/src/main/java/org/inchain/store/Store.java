package org.inchain.store;

import org.inchain.core.exception.ProtocolException;
import org.inchain.message.Message;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NetworkParameters.ProtocolVersion;

public abstract class Store extends Message {
	
	protected byte[] key;
	
	public Store() {
	}
	
	protected Store(NetworkParameters network) {
        this.network = network;
        serializer = network.getDefaultSerializer();
    }
    
    protected Store(NetworkParameters network, byte[] payload, int offset) throws ProtocolException {
        this(network, payload, offset, network.getProtocolVersionNum(ProtocolVersion.CURRENT));
    }

    protected Store(NetworkParameters network, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        super(network, payload, offset, protocolVersion, network.getDefaultSerializer(), UNKNOWN_LENGTH);
    }
	
	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}
	
}
