package org.inchain.message;

import java.net.UnknownHostException;

import org.inchain.core.PeerAddress;
import org.inchain.core.TimeService;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;

public class VerackMessage extends VersionMessage {

	public VerackMessage(NetworkParams params, byte[] payload) throws ProtocolException {
        super(params, payload);
    }
	
	public VerackMessage(NetworkParams params, long bestHeight, Sha256Hash bestBlockHash, PeerAddress myAddress, PeerAddress remoteAddress) throws UnknownHostException {
		super(params, bestHeight, bestBlockHash, myAddress, remoteAddress);
		//给对方最新的网络时间
		time = TimeService.currentTimeMillis();
	}
	
}
