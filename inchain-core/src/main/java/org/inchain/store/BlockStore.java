package org.inchain.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;

/**
 * 区块完整信息
 * @author ln
 *
 */
public class BlockStore extends Store {

	//下一区块hash
	private Sha256Hash nextHash;
	private Block block;
	
	public BlockStore(NetworkParams network) {
		super(network);
	}

	public BlockStore(NetworkParams network, byte[] content) {
		super(network, content, 0);
	}
	
	public BlockStore(NetworkParams network, Block block) {
		super(network);
		this.block = block;
		nextHash = Sha256Hash.ZERO_HASH;
	}

	@Override
	public void parse() throws ProtocolException {
		
		block = new Block(network, payload);
		nextHash = Sha256Hash.wrap(readBytes(32));
		length = cursor;
	}

	/**
	 * 序列化区块
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		
		block.serializeToStream(stream);
		stream.write(nextHash.getBytes());
    }
	
	/**
	 * 序列化头信息
	 */
	public byte[] serializeHeaderToBytes() throws IOException {
		ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream();
		
		try {
			Utils.uint32ToByteStreamLE(block.getVersion(), stream);
			stream.write(block.getPreHash().getBytes());
			stream.write(block.getMerkleHash().getBytes());
			Utils.uint32ToByteStreamLE(block.getTime(), stream);
			Utils.uint32ToByteStreamLE(block.getHeight(), stream);

			stream.write(new VarInt(block.getPeriodCount()).encode());
			stream.write(new VarInt(block.getTimePeriod()).encode());
			Utils.uint32ToByteStreamLE(block.getPeriodStartTime(), stream);

			stream.write(new VarInt(block.getScriptBytes().length).encode());
			stream.write(block.getScriptBytes());
			//交易数量
			stream.write(new VarInt(block.getTxCount()).encode());
			for (Transaction tx : block.getTxs()) {
				stream.write(tx.getHash().getBytes());
			}
			stream.write(nextHash.getBytes());
			return stream.toByteArray();
		} finally {
			stream.close();
		}
	}
	
	public Sha256Hash getNextHash() {
		return nextHash;
	}

	public void setNextHash(Sha256Hash nextHash) {
		this.nextHash = nextHash;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}
	
}
