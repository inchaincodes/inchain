package org.inchain.message;

import org.inchain.core.PeerAddress;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;

public class VerackMessage extends VersionMessage {

	public VerackMessage(NetworkParams params, byte[] payload) throws ProtocolException {
        super(params, payload);
    }
	
	public VerackMessage(NetworkParams params, long newBestHeight, PeerAddress remoteAddress) {
		super(params, newBestHeight, remoteAddress);
	}
	
}
