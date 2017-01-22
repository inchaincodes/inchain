package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * 区块头信息
 * @author ln
 *
 */
public class BlockHeader extends Message {

	//区块版本
	protected long version;
	//区块hash
	protected Sha256Hash hash;
	//上一区块hash
	protected Sha256Hash preHash;
	//梅克尔树根节点hash
	protected Sha256Hash merkleHash;
	//时间戳
	protected long time;
	//区块高度
	protected long height;
	//交易数
	protected long txCount;
	
	protected List<Sha256Hash> txHashs;
	
	public BlockHeader(NetworkParams network) {
		super(network);
	}
	
	public BlockHeader(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
		super(network, payload, offset);
    }
	
	public BlockHeader(NetworkParams network, byte[] payload) throws ProtocolException {
		super(network, payload, 0);
    }
	
	public void parse() throws ProtocolException {
		version = readUint32();
		preHash = Sha256Hash.wrap(readBytes(32));
		merkleHash = Sha256Hash.wrap(readBytes(32));
		time = readUint32();
		height = readUint32();
		txCount = readVarInt();
		
		txHashs = new ArrayList<Sha256Hash>();
		for (int i = 0; i < txCount; i++) {
			txHashs.add(Sha256Hash.wrap(readBytes(32)));
		}
		
		length = cursor;
	}
	
	/**
	 * 序列化区块头
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		Utils.uint32ToByteStreamLE(version, stream);
		stream.write(preHash.getBytes());
		stream.write(merkleHash.getBytes());
		Utils.uint32ToByteStreamLE(time, stream);
		Utils.uint32ToByteStreamLE(height, stream);
		//交易数量
		stream.write(new VarInt(txCount).encode());
		if(txHashs != null) {
			for (int i = 0; i < txCount; i++) {
				stream.write(txHashs.get(i).getBytes());
			}
		}
    }
	
	@Override
	public String toString() {
		return "BlockHeader [hash=" + hash + ", preHash=" + preHash + ", merkleHash=" + merkleHash + ", txCount="
				+ txCount + ", time=" + time + ", height=" + height + "]";
	}

	public boolean equals(BlockHeader other) {
		return hash.equals(other.hash) && preHash.equals(other.preHash) && merkleHash.equals(other.merkleHash) && 
				txCount == other.txCount && time == other.time && height == other.height;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Sha256Hash getHash() {
		return hash;
	}

	public void setHash(Sha256Hash hash) {
		this.hash = hash;
	}

	public Sha256Hash getPreHash() {
		return preHash;
	}

	public void setPreHash(Sha256Hash preHash) {
		this.preHash = preHash;
	}

	public Sha256Hash getMerkleHash() {
		return merkleHash;
	}

	public void setMerkleHash(Sha256Hash merkleHash) {
		this.merkleHash = merkleHash;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public long getTxCount() {
		return txCount;
	}

	public void setTxCount(long txCount) {
		this.txCount = txCount;
	}

	public List<Sha256Hash> getTxHashs() {
		return txHashs;
	}

	public void setTxHashs(List<Sha256Hash> txHashs) {
		this.txHashs = txHashs;
	}
}
