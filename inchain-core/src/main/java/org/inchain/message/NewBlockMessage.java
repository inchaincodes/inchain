package org.inchain.message;

import org.inchain.network.NetworkParams;
import org.inchain.store.BlockStore;

/**
 * 新区块诞生消息
 * @author ln
 *
 */
public class NewBlockMessage extends BlockMessage {

	public NewBlockMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes);
	}
	
	public NewBlockMessage(NetworkParams network, BlockStore blockStore) {
		super(network, blockStore);
	}

}
