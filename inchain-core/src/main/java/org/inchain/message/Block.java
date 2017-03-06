package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 区块消息
 * @author ln
 *
 */
public class Block extends BlockHeader {

	private static final Logger log = LoggerFactory.getLogger(Block.class);
	
	//交易列表
	private List<Transaction> txs;
		
	public Block(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public Block(NetworkParams network) {
		super(network);
	}

	/**
	 * 序列化
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		txHashs = null;
		super.serializeToStream(stream);
		
		//交易数量
		for (int i = 0; i < txs.size(); i++) {
			Transaction tx = txs.get(i);
			stream.write(tx.baseSerialize());
		}
	}
	
	/**
	 * 反序列化
	 */
	@Override
	public void parse() throws ProtocolException {
		
		version = readUint32();
		preHash = Sha256Hash.wrap(readBytes(32));
		merkleHash = Sha256Hash.wrap(readBytes(32));
		time = readInt64();
		height = readUint32();
		periodCount = (int) readVarInt();
		timePeriod = (int) readVarInt();
		periodStartPoint = readUint32();
		scriptBytes = readBytes((int) readVarInt());
		scriptSig = new Script(scriptBytes);
		txCount = readVarInt();
		
		txs = new ArrayList<Transaction>();
		for (int i = 0; i < txCount; i++) {
			Transaction transaction = network.getDefaultSerializer().makeTransaction(payload, cursor);
			txs.add(transaction);
			cursor += transaction.getLength();
		}
		
		length = cursor;
	}
	
	/**
	 * 计算区块的梅克尔树根
	 * @return Sha256Hash
	 */
	public Sha256Hash buildMerkleHash() {
		
		List<byte[]> tree = new ArrayList<byte[]>();
        for (Transaction t : txs) {
            tree.add(t.getHash().getBytes());
        }
        int levelOffset = 0;
        for (int levelSize = txs.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = Utils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = Utils.reverseBytes(tree.get(levelOffset + right));
                tree.add(Utils.reverseBytes(Sha256Hash.hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            levelOffset += levelSize;
        }
        Sha256Hash merkleHash = Sha256Hash.wrap(tree.get(tree.size() - 1));
        if(this.merkleHash == null) {
        	this.merkleHash = merkleHash;
        } else {
        	try {
        		Utils.checkState(this.merkleHash.equals(merkleHash), "the merkle hash is error " + this.merkleHash +"  !=  " + merkleHash+"   blcok:" + toString());
        	} catch (Exception e) {
        		log.warn(e.getMessage(), e);
        		throw e;
			}
        }
		return merkleHash;
	}

	/**
	 * 计算区块hash
	 * @return Sha256Hash
	 */
	public Sha256Hash getHash() {
		Sha256Hash id = Sha256Hash.twiceOf(baseSerialize());
		if(hash != null) {
			Utils.checkState(hash.equals(id), "区块信息不正确");
		} else {
			hash = id;
		}
		return id;
	}
	
	@Override
	public String toString() {
		return "Block [hash=" + getHash() + ", preHash=" + preHash + ", merkleHash=" + merkleHash + ", txCount="
				+ txCount + ", time=" + time + ", height=" + height + "]";
	}

	public boolean equals(BlockHeader other) {
		return hash.equals(other.hash) && preHash.equals(other.preHash) && merkleHash.equals(other.merkleHash) && 
				txCount == other.txCount && time == other.time && height == other.height;
	}

	public List<Transaction> getTxs() {
		return txs;
	}

	public void setTxs(List<Transaction> txs) {
		this.txs = txs;
	}
}
