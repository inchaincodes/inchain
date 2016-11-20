package org.inchain.net;

import org.inchain.core.Peer;

/**
 * 新连接监听器，用于询问是否允许连接
 * @author ln
 *
 */
public interface NewInConnectionListener {

	boolean allowConnection();
	
	void connectionOpened(Peer peer);
	
	void connectionClosed(Peer peer);
}
