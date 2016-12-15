package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 获取区块信息消息，收到该消息的节点必须应答，返回相应的区块数据
 * @author ln
 *
 */
public class GetBlockMessage extends Message {

	//开始区块高度
	private long startBlockHeight;
	//需要下载的区块数量
	private long count;
	
	public GetBlockMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public GetBlockMessage(NetworkParams network, long startBlockHeight, long count) {
		this.startBlockHeight = startBlockHeight;
		this.count = count;
	}

	/**
	 * 序列化
	 */
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		Utils.uint32ToByteStreamLE(startBlockHeight, stream);
		Utils.uint32ToByteStreamLE(count, stream);
	}
	
	/**
	 * 反序列化
	 */
	@Override
	protected void parse() throws ProtocolException {
		this.startBlockHeight = readUint32();
		this.count = readUint32();
		length = cursor;
	}

	@Override
	public String toString() {
		return "GetBlockMessage [startBlockHeight=" + startBlockHeight + ", count=" + count + "]";
	}

	public long getStartBlockHeight() {
		return startBlockHeight;
	}

	public void setStartBlockHeight(long startBlockHeight) {
		this.startBlockHeight = startBlockHeight;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}
	
}
