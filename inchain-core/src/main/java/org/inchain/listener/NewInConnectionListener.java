package org.inchain.listener;

import java.net.InetSocketAddress;

import org.inchain.core.Peer;

/**
 * 新连接监听器，用于询问是否允许连接
 * @author ln
 *
 */
public interface NewInConnectionListener {

	boolean allowConnection(InetSocketAddress socketAddress);
	
	void connectionOpened(Peer peer);
	
	void connectionClosed(Peer peer);
}
