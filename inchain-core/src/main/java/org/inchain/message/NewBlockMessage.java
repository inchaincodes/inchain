package org.inchain.message;

import org.inchain.network.NetworkParams;

/**
 * 新区块诞生消息
 * @author ln
 *
 */
public class NewBlockMessage extends Block {

	public NewBlockMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes);
	}
}
