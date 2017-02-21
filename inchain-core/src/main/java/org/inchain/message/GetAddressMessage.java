package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 向对等体获取网络地址列表
 * @author ln
 *
 */
public class GetAddressMessage extends Message {

	private long time;
	
	public GetAddressMessage(NetworkParams network) {
		super(network);
	}
	
	public GetAddressMessage(NetworkParams network, long time) {
		super(network);
		this.time = time;
	}

	public GetAddressMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		Utils.int64ToByteStreamLE(time, stream);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		time = readInt64();
		length = cursor;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "GetAddressMessage [time=" + time + "]";
	}
}
