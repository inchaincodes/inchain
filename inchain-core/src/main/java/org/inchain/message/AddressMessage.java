package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.inchain.core.PeerAddress;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 网络节点地址列表消息,注意与系统地址区分
 * @author ln
 *
 */
public class AddressMessage extends Message {
	
	//一次性交互的最大地址数量
	public static final int MAX_ADDRESSES = 1024;
	
    private List<PeerAddress> addresses;
	
	public AddressMessage(NetworkParams network) {
		super(network);
		addresses = new ArrayList<PeerAddress>();
		length = 0;
	}

	public AddressMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		if (addresses == null)
            return;
        stream.write(new VarInt(addresses.size()).encode());
        for (PeerAddress addr : addresses) {
            addr.serializeToStream(stream);
        }
	}
	
	@Override
	protected void parse() throws ProtocolException {
		long numAddresses = readVarInt();
        if (numAddresses > MAX_ADDRESSES)
            throw new ProtocolException("Address message too large.");
        addresses = new ArrayList<PeerAddress>((int) numAddresses);
        for (int i = 0; i < numAddresses; i++) {
            PeerAddress addr = new PeerAddress(network, payload, cursor, protocolVersion);
            addresses.add(addr);
            cursor += addr.getMessageSize();
        }
        length = new VarInt(addresses.size()).getSizeInBytes();
        length += addresses.size() * PeerAddress.MESSAGE_SIZE;
	}

	public List<PeerAddress> getAddresses() {
		return Collections.unmodifiableList(addresses);
    }

    public void addAddress(PeerAddress address) {
	    addresses.add(address);
	    if (length == UNKNOWN_LENGTH)
	        getMessageSize();
	    else
	        length += address.getMessageSize();
    }

    public void removeAddress(int index) {
        PeerAddress address = addresses.remove(index);
        if (length == UNKNOWN_LENGTH)
            getMessageSize();
        else
            length -= address.getMessageSize();
    }

    @Override
    public String toString() {
        return "addr: " + Utils.join(addresses);
    }
}
