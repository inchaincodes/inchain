package org.inchain.message;

import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;

/**
 * 拒绝消息
 * @author ln
 *
 */
public class RejectMessage extends Message {

	public RejectMessage(NetworkParams network) {
        super();
    }
    
	public RejectMessage(NetworkParams network, byte[] payload) throws ProtocolException {
    	this(network, payload, 0);
    }
	
	public RejectMessage(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
    	super(network, payload, offset);
    }
	
	@Override
	protected void parse() throws ProtocolException {
		//TODO
	}
}
