package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;

/**
 * 获取区块信息消息，收到该消息的节点必须应答，返回相应的区块inv消息
 * @author ln
 *
 */
public class GetBlocksMessage extends Message {

	//开始区块的hash
	private Sha256Hash startHash;
	//结束区块的hash
	private Sha256Hash stopHash;
	
	public GetBlocksMessage(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public GetBlocksMessage(NetworkParams network, Sha256Hash startHash, Sha256Hash stopHash) {
		this.startHash = startHash;
		this.stopHash = stopHash;
	}

	/**
	 * 序列化
	 */
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(startHash.getReversedBytes());
		stream.write(stopHash.getReversedBytes());
	}
	
	/**
	 * 反序列化
	 */
	@Override
	protected void parse() throws ProtocolException {
		this.startHash = readHash();
		this.stopHash = readHash();
		length = cursor;
	}

	@Override
	public String toString() {
		return "GetBlockMessage [startHash=" + startHash + ", stopHash=" + stopHash + "]";
	}

	public Sha256Hash getStartHash() {
		return startHash;
	}

	public void setStartHash(Sha256Hash startHash) {
		this.startHash = startHash;
	}

	public Sha256Hash getStopHash() {
		return stopHash;
	}

	public void setStopHash(Sha256Hash stopHash) {
		this.stopHash = stopHash;
	}
}
