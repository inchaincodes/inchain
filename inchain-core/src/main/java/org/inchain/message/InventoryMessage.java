package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;

/**
 * 向量清单消息
 * @author ln
 *
 */
public class InventoryMessage extends Message {
	
	public static final long MAX_INVENTORY_ITEMS = 10000;
	
	protected long size;
	protected List<InventoryItem> invs;
	
	public InventoryMessage(NetworkParams network, InventoryItem item) {
		super(network);
		invs = new ArrayList<InventoryItem>();
		invs.add(item);
	}
	
	public InventoryMessage(NetworkParams network, List<InventoryItem> items) {
		super(network);
		invs = items;
	}
	
	public InventoryMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		size =  readVarInt();
		if (size > MAX_INVENTORY_ITEMS) {
            throw new ProtocolException("超过 inv 消息最大条数限制: " + size);
		}
		invs = new ArrayList<InventoryItem>();
		for (int i = 0; i < size; i++) {
            if (cursor + InventoryItem.MESSAGE_LENGTH > payload.length) {
                throw new ProtocolException("Ran off the end of the INV");
            }
            int typeCode = (int) readBytes(1)[0];
            InventoryItem.Type type = InventoryItem.Type.from(typeCode);
            InventoryItem item = new InventoryItem(type, readHash());
            invs.add(item);
        }
		length = cursor - offset;
        payload = null;
	}

	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(new VarInt(invs.size()).encode());
		for (InventoryItem item : invs) {
			stream.write(item.getType().ordinal());
            stream.write(item.getHash().getReversedBytes());
        }
	}
	
	public void addItem(InventoryItem item) {
		invs.add(item);
	}
	
	public List<InventoryItem> getInvs() {
		return invs;
	}
	
	@Override
	public String toString() {
		return invs.toString();
	}
}
