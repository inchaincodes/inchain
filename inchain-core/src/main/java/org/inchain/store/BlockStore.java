package org.inchain.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParameters;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;

/**
 * 区块完整信息
 * @author ln
 *
 */
public class BlockStore extends BlockHeaderStore {

	//交易列表
	private List<TransactionStore> txs;
	
	public BlockStore(NetworkParameters network) {
		super(network);
	}

	public BlockStore(NetworkParameters network, byte[] content) {
		super(network, content, 0);
	}

	@Override
	public void parse() throws ProtocolException {
		super.parse();
		
		txs = new ArrayList<TransactionStore>();
		for (int i = 0; i < txCount; i++) {
			TransactionStore store = new TransactionStore(network, payload, cursor);
			txs.add(store);
			cursor += store.getLength();
		}
		length = cursor;
	}
	
	public void parseHeader() throws ProtocolException {
		super.parse();
	}

	/**
	 * 序列化区块
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		if(txHashs != null) {
			txHashs = null;
		}
		super.serializeToStream(stream);
		//交易数量
		for (int i = 0; i < txCount; i++) {
			Transaction tx = txs.get(i).getTransaction();
			stream.write(tx.baseSerialize());
			Utils.uint32ToByteStreamLE(height, stream);
		}
    }
	
	/**
	 * 序列化区块头
	 */
	protected byte[] serializeHeader() throws IOException {
		if(txHashs == null) {
			txHashs = new ArrayList<Sha256Hash>();
			for (TransactionStore tx : txs) {
				txHashs.add(tx.getTransaction().getHash());
			}
		}
		ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(getHeaderSize());
		serializeHeaderToStream(stream);
        
		return stream.toByteArray();
    }

	/**
	 * 序列化区块头
	 */
	protected void serializeHeaderToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
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
		return hash;
	}

	/**
	 * 计算区块的梅克尔树根
	 * @return Sha256Hash
	 */
	public Sha256Hash buildMerkleHash() {
		
		List<byte[]> tree = new ArrayList<byte[]>();
        for (TransactionStore t : txs) {
            tree.add(t.getTransaction().getHash().getBytes());
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
        	Utils.checkState(this.merkleHash.equals(merkleHash), "the merkle hash is error");
        }
		return merkleHash;
	}
	
	public List<TransactionStore> getTxs() {
		return txs;
	}
	public void setTxs(List<TransactionStore> txs) {
		this.txs = txs;
	}

	@Override
	public String toString() {
		return "BlockStore [version=" + version + ", hash=" + getHash() + ", preHash=" + preHash + ", merkleHash="
				+ merkleHash + ", time=" + time + ", height=" + height + ", txCount=" + txCount + "]";
	}
}
