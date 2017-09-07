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
	 * 启动
	 */
	public void startSync();
	
	/**
	 * 程序关闭时，持久化内存里面的节点信息到文件
	 */
	public void shutdown();
	
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
	 * @return List<Seed>
	 */
	public List<Seed> getCanConnectPeerSeeds();

	/**
	 * 获取可连接的节点列表
	 * @return List<Seed>
	 */
	public List<Seed> getCanConnectPeerSeeds(int maxCount);
	
	/**
	 * 节点是否已经存在（已被发现）
	 * @param peerAddress
	 * @return boolean
	 */
	public boolean hasExist(PeerAddress peerAddress);

	/**
	 * 刷新节点的连接状态
	 * @param seed
	 */
	public void refreshSeedStatus(Seed seed);

	/**
	 * 检查本机服务是否对外提供，如果提供则上传
	 */
	public void checkMyserviceAndReport();

	/**
	 * 重置节点信息
	 * 该方法会清楚本地保存的节点，在重置本地数据时会调用
	 */
	public void reset();

	public List<Seed> getDnsSeeds(int maxCount);

	public List<Seed> getAllSeeds();
}
