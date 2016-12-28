package org.inchain.message;

import org.inchain.network.NetworkParams;

/**
 * 下载数据，包括最新区块和交易信息
 * @author ln
 *
 */
public class GetDatasMessage extends InventoryMessage {

public static final long MAX_INVENTORY_ITEMS = 10000;
	
	public GetDatasMessage(NetworkParams network, InventoryItem item) {
		super(network, item);
	}
	
	public GetDatasMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes);
	}
}
