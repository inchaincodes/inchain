package org.inchain.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParameters;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 区块提供服务
 * @author ln
 *
 */
public class BlockStoreProvider extends BaseStoreProvider {

	//区块锁，保证每次新增区块时，不会有并发问题，每次读取最新区块时始终会返回本地最新的一个块
	//当新增时也要检查要保存的块是否和最新的块能衔接上
	private final static Lock blockLock = new ReentrantLock();
	//最新区块标识
	private final static byte[] bestBlockKey = Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")).getBytes();
	
	private static BlockStoreProvider INSTACE;
	
	//单例
	public static BlockStoreProvider getInstace(String dir, NetworkParameters network) {
		return getInstace(dir, network, -1, -1);
	}
	public static BlockStoreProvider getInstace(String dir, NetworkParameters network, long leveldbReadCache, int leveldbWriteCache) {
		if(INSTACE == null) {
			synchronized (locker) {
				if(INSTACE == null)
					INSTACE = new BlockStoreProvider(dir, network);
			}
		}
		return INSTACE;
	}
	
	BlockStoreProvider(String dir, NetworkParameters network) {
		super(dir, network);
	}

	@Override
	protected byte[] toByte(Store store) {
		return null;
	}

	@Override
	protected Store pase(byte[] content) {
		return null;
	}
	
	/**
	 * 保存区块完整的区块信息
	 * @param block
	 * @throws IOException 
	 */
	public void saveBlock(BlockStore block) throws IOException {
		//最新的区块
		BlockHeaderStore bestBlockHeader = getBestBlockHeader();
		//判断当前要保存的区块，是否是在最新区块之后
		//保存创始块则不限制
		if (block.getPreHash() == null) {
			throw new VerificationException("要保存的区块缺少上一区块的引用");
		} else if(bestBlockHeader == null && Arrays.equals(bestBlockKey, block.getPreHash().getBytes()) && block.getHeight() == 0l) {
			//创世块则通过
		} else if(bestBlockHeader != null && bestBlockHeader.getHash().equals(block.getPreHash()) &&
				bestBlockHeader.getHeight() + 1 == block.getHeight()) {
			//要保存的块和最新块能连接上，通过
		} else {
			throw new VerificationException("错误的区块，保存失败");
		}
		
		blockLock.lock();
		try {
			//保存块头
			db.put(block.getHash().getBytes(), block.serializeHeader());
			//保存交易
			for (int i = 0; i < block.getTxCount(); i++) {
				TransactionStore tx = block.getTxs().get(i);
		        
				db.put(tx.getTransaction().getHash().getBytes(), tx.baseSerialize());
			}
			
			byte[] heightBytes = new byte[4]; 
			Utils.uint32ToByteArrayBE(block.getHeight(), heightBytes, 0);
			
			db.put(heightBytes, block.getKey());
			
			//更新最新区块
			db.put(bestBlockKey, block.getHash().getBytes());
		} finally {
			blockLock.unlock();
		}
	}

	/**
	 * 获取区块头信息
	 * @param hash
	 * @return BlockHeaderStore
	 */
	public BlockHeaderStore getHeader(byte[] hash) {
		byte[] content = db.get(hash);
		if(content == null) {
			return null;
		}
		BlockHeaderStore header = new BlockHeaderStore(network, content);
		header.setHash(Sha256Hash.wrap(hash));
		
		return header;
	}
	
	/**
	 * 获取区块头信息
	 * @param height
	 * @return BlockHeaderStore
	 */
	public BlockHeaderStore getHeaderByHeight(long height) {
		byte[] heightBytes = new byte[4]; 
		Utils.uint32ToByteArrayBE(height, heightBytes, 0);
		
		byte[] hash = db.get(heightBytes);
		if(hash == null) {
			return null;
		}
		return getHeader(hash);
	}

	/**
	 * 获取区块头信息
	 * @param height
	 * @return BlockStore
	 */
	public BlockStore getBlockByHeight(long height) {
		return getBlockByHeader(getHeaderByHeight(height));
	}
	
	/**
	 * 获取完整的区块信息
	 * @param hash
	 * @return BlockStore
	 */
	public BlockStore getBlock(byte[] hash) {
		
		BlockHeaderStore header = getHeader(hash);
		return getBlockByHeader(header);
	}
	
	/**
	 * 通过区块头获取区块的完整信息，主要是把交易详情查询出来
	 * @param header
	 * @return BlockStore
	 */
	public BlockStore getBlockByHeader(BlockHeaderStore header) {
		//交易列表
		List<TransactionStore> txs = new ArrayList<TransactionStore>();
		
		for (Sha256Hash txHash : header.getTxHashs()) {
			txs.add(getTransaction(txHash.getBytes()));
		}
		
		BlockStore block = new BlockStore(network);
		block.setTxs(txs);
		block.setVersion(header.getVersion());
		block.setHash(header.getHash());
		block.setHeight(header.getHeight());
		block.setKey(header.getKey());
		block.setMerkleHash(header.getMerkleHash());
		block.setPreHash(header.getPreHash());
		block.setTime(header.getTime());
		block.setTxCount(header.getTxCount());
		return block;
	}
	
	/**
	 * 获取一笔交易
	 * @param hash
	 * @return TransactionStore
	 */
	public TransactionStore getTransaction(byte[] hash) {
		byte[] content = db.get(hash);
		if(content == null) {
			return null;
		}
		TransactionStore store = new TransactionStore(network, content);
		store.setKey(hash);
		
		return store;
	}
	
	/**
	 * 获取最新块的头信息
	 * @return BlockHeaderStore
	 */
	public BlockHeaderStore getBestBlockHeader() {
		blockLock.lock();
		
		byte[] bestBlockHash = null;
		try {
			bestBlockHash = db.get(bestBlockKey);
		} finally {
			blockLock.unlock();
		}
		if(bestBlockHash == null) {
			return null;
		} else {
			return getHeader(bestBlockHash);
		}
	}
	
	/**
	 * 获取最新区块的完整信息
	 * @return BlockStore
	 */
	public BlockStore getBestBlock() {
		//获取最新的区块
		BlockHeaderStore header = getBestBlockHeader();
		
		return getBlockByHeader(header);
	}
}
