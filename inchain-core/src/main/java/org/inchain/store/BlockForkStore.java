package org.inchain.store;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.exception.ProtocolException;
import org.inchain.message.Block;
import org.inchain.network.NetworkParams;

/**
 * 分叉块的存储
 * @author ln
 *
 */
public class BlockForkStore extends Store {

	//状态
	private int status;
	private Block block;
	
	public BlockForkStore(NetworkParams network) {
		super(network);
	}

	public BlockForkStore(NetworkParams network, byte[] content) {
		super(network, content, 0);
	}
	
	public BlockForkStore(NetworkParams network, Block block, int status) {
		super(network);
		this.block = block;
		this.status = status;
	}

	@Override
	public void parse() throws ProtocolException {
		
		block = new Block(network, payload);
		status = readBytes(1)[0] & 0xff;
		length = cursor;
	}

	/**
	 * 序列化区块
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		
		block.serializeToStream(stream);
		stream.write(status);
    }

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}
