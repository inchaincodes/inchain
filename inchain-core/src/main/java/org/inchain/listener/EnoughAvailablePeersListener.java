package org.inchain.listener;

import java.util.List;

import org.inchain.core.Peer;

/**
 * 已连接的对等体数量达到一定值的监听
 * @author ln
 *
 */
public interface EnoughAvailablePeersListener {

	void callback(List<Peer> peers);
}
