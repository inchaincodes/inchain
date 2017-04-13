package org.inchain.service;

import java.util.List;

import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;

/**
 * 块分叉处理
 * @author ln
 *
 */
public interface BlockForkService {

	void startSyn();
	
	void start();
	
	void stop();
	
	/**
	 * 添加分叉块
	 * @param block
	 */
	void addBlockFork(Block block);

	/**
	 * 添加到待处罚列表
	 * @param block
	 * @param type 违规类型, 1 重复出块, 2大量垃圾块攻击, 3打包不合法的交易
	 */
	void addBlockInPenalizeList(Block block, int type);
	
	/**
	 * 获取并移除待处罚列表
	 * @return List<BlockHeader>
	 */
	List<BlockHeader> getAndRemovePenalize();
	
	/**
	 * 获取一个块
	 * @param hash
	 * @return Block
	 */
	Block getBlock(Sha256Hash hash);
}
