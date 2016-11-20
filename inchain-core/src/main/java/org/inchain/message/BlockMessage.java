package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParameters;
import org.inchain.store.BlockStore;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;

/**
 * 区块消息
 * @author ln
 *
 */
public class BlockMessage extends Message {

	private BlockStore blockStore;

	public BlockMessage(NetworkParameters network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public BlockMessage(NetworkParameters network, BlockStore blockStore) {
		super(network);
		
		this.blockStore = blockStore;
	}

	/**
	 * 序列化
	 */
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		//版本号
		Utils.uint32ToByteStreamLE(blockStore.getVersion(), stream);
		//上一区块hash
		stream.write(blockStore.getPreHash().getBytes());
		//梅克尔树根
		stream.write(blockStore.getMerkleHash().getBytes());
		//时间戳
		Utils.uint32ToByteStreamLE(blockStore.getTime(), stream);
		//高度
		Utils.uint32ToByteStreamLE(blockStore.getHeight(), stream);
		//交易数量
		stream.write(new VarInt(blockStore.getTxCount()).encode());
		//交易数量
		for (int i = 0; i < blockStore.getTxCount(); i++) {
			Transaction tx = blockStore.getTxs().get(i).getTransaction();
			stream.write(tx.baseSerialize());
		}
	}
	
	/**
	 * 反序列化
	 */
	@Override
	protected void parse() throws ProtocolException {
		
		BlockStore blockStore = new BlockStore(network);
		
		blockStore.setVersion(readUint32());
		blockStore.setPreHash(Sha256Hash.wrap(readBytes(32)));
		blockStore.setMerkleHash(Sha256Hash.wrap(readBytes(32)));
		blockStore.setTime(readUint32());
		blockStore.setHeight(readUint32());
		blockStore.setTxCount(readVarInt());
		
		ArrayList<TransactionStore> txs = new ArrayList<TransactionStore>();
		for (int i = 0; i < blockStore.getTxCount(); i++) {
			Transaction transaction = network.getDefaultSerializer().makeTransaction(payload, cursor);
			TransactionStore store = new TransactionStore(network, transaction);
			txs.add(store);
			cursor += transaction.getLength();
		}
		blockStore.setTxs(txs);
		
		this.blockStore = blockStore;
		
		length = cursor;
	}

	public BlockStore getBlockStore() {
		return blockStore;
	}
	
	@Override
	public String toString() {
		return "BlockMessage [blockStore=" + blockStore + "]";
	}
}
