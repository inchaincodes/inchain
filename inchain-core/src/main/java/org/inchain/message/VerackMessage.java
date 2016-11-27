package org.inchain.message;

import org.inchain.core.PeerAddress;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;

/**
 * <p>The verack message, sent by a client accepting the version message they
 * received from their peer.</p>
 * 
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class VerackMessage extends VersionMessage {

	public VerackMessage(NetworkParams params, byte[] payload) throws ProtocolException {
        super(params, payload);
    }
	
	public VerackMessage(NetworkParams params, int newBestHeight, PeerAddress remoteAddress) {
		super(params, newBestHeight, remoteAddress);
	}
	
}
