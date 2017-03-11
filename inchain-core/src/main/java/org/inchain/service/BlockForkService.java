package org.inchain.service;

import org.inchain.message.Block;

/**
 * 块分叉处理
 * @author ln
 *
 */
public interface BlockForkService {

	void start();
	
	void stop();
	
	/**
	 * 添加分叉块
	 * @param block
	 */
	void addBlockFork(Block block);
	
}
