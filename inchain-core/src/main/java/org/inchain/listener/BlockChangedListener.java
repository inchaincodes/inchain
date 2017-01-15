package org.inchain.listener;

import org.inchain.crypto.Sha256Hash;

/**
 * 区块变化监听器
 * @author ln
 *
 */
public interface BlockChangedListener {

	/**
	 * 当区块发生变化时，告知当前本地最新区块，网络最新区块，和本地区块hash，网络最新区块hash
	 * @param localNewestHeight 本地最新区块高度
	 * @param netNewestHeight	网络最新区块高度
	 * @param localNewestHash	本地最新区块hash
	 * @param netNewestHash		网络最新区块hash
	 */
	void onChanged(long localNewestHeight, long netNewestHeight, Sha256Hash localNewestHash, Sha256Hash netNewestHash);
}
