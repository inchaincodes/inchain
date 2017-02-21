package org.inchain.network;

import java.util.List;

import org.inchain.core.PeerAddress;

/**
 * 节点发现服务接口
 * @author ln
 *
 */
public interface PeerDiscovery {
	
	/**
	 * 添加一个节点
	 * @param peerAddress
	 */
	public boolean add(PeerAddress peerAddress);
	
	/**
	 * 添加一个节点
	 * @param peerAddress
	 * @param hasVerify 是否已经验证
	 */
	public boolean add(PeerAddress peerAddress, boolean hasVerify);

	/**
	 * 批量添加节点，未经验证的
	 * @param addresses
	 */
	public void addBath(List<PeerAddress> addresses);

	/**
	 * 获取可用的节点列表，最大返回1024个
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getAvailablePeerAddress();
	
	/**
	 * 获取可用的节点列表
	 * @param maxCount	最多返回数量
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getAvailablePeerAddress(int maxCount);
	
	/**
	 * 获取可连接的节点列表
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getCanConnectPeerAddress();

	/**
	 * 获取可连接的节点列表
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getCanConnectPeerAddress(int maxCount);
	

	/**
	 * 节点是否已经存在（已被发现）
	 * @param peerAddress
	 * @return boolean
	 */
	public boolean hasExist(PeerAddress peerAddress);

	/**
	 * 是否已经连接过了
	 * @param peerAddress
	 * @return boolean
	 */
	public boolean hasConnected(PeerAddress peerAddress);

}
