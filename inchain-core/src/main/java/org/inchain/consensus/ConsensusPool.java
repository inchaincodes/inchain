package org.inchain.consensus;

/**
 * 共识池，维护所有参与共识的人，符合条件的人可随时加入，随时退出
 * @author ln
 *
 */
public interface ConsensusPool {

	/**
	 * 新增共识结点
	 * @param hash160
	 */
	public void add(byte[] hash160, byte[] pubkey);
	
	/**
	 * 移除共识节点
	 */
	public void delete(byte[] hash160);
	
	/**
	 * 判断是否是共识节点
	 * @param hash160
	 * @return boolean
	 */
	public boolean contains(byte[] hash160);
	
	/**
	 * 获取共识节点的公钥
	 * @param hash160
	 * @return byte[]
	 */
	public byte[] getPubkey(byte[] hash160);
}
