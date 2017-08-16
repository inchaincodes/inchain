package org.inchain.kits;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Broadcaster;
import org.inchain.core.Peer;
import org.inchain.core.PeerAddress;
import org.inchain.core.TimeService;
import org.inchain.listener.BlockChangedListener;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.listener.EnoughAvailablePeersListener;
import org.inchain.listener.NewInConnectionListener;
import org.inchain.message.Message;
import org.inchain.net.ClientConnectionManager;
import org.inchain.network.NetworkParams;
import org.inchain.network.PeerDiscovery;
import org.inchain.network.Seed;
import org.inchain.utils.IpUtil;
import org.inchain.utils.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * 节点管理
 * @author ln
 *
 */
@Service
public class PeerKit {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PeerKit.class);

	//默认最大节点连接数，这里指单向连接，被动连接的数量
	private static final int DEFAULT_MAX_IN_CONNECTION = 2000;
	
	private static final Set<String> LOCAL_ADDRESS = IpUtil.getIps();
	
	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);
	
	//最小节点连接数，只要达到这个数量之后，节点才开始同步与监听数据，并提供网络服务
	private int minConnectionCount = Configure.MIN_CONNECT_COUNT;
	//最大连接数，主动连接的数量
	private int maxConnectionCount = Configure.MAX_CONNECT_COUNT;
	//连接变化监听器
	private CopyOnWriteArrayList<ConnectionChangedListener> connectionChangedListeners = new CopyOnWriteArrayList<ConnectionChangedListener>();
	//是否初始化网络时间
	private boolean initTimeOffset;
	
	//区块变化监听器
	private BlockChangedListener blockChangedListener;
	
	//被动连接节点
	private CopyOnWriteArrayList<Peer> inPeers = new CopyOnWriteArrayList<Peer>();
	//主动连接节点
	private CopyOnWriteArrayList<Peer> outPeers = new CopyOnWriteArrayList<Peer>();
	
	//连接管理器
	@Autowired
	private ClientConnectionManager connectionManager;
	//网络
	@Autowired
	private NetworkParams network;
	//广播器
	@Autowired
	private Broadcaster<Message> broadcaster;
	@Autowired
	private PeerDiscovery peerDiscovery;
	
	public PeerKit() {
		
	}

	//启动服务
	private void start() {
		
		Utils.checkNotNull(network);
		Utils.checkNotNull(connectionManager);
		
		init();
		
		//初始化连接器
		connectionManager.start();
		
		peerDiscovery.startSync();
		
		//初始化节点
		initPeers();
		
		//ping task
		startPingTask();
		
	}

	//异步启动
	public void startSyn() {
		new Thread(){
			public void run() {
				PeerKit.this.start();
			}
		}.start();
	}
	
	//停止服务
	public void stop() throws IOException {
		//关闭服务
		executor.shutdown();
		//关闭连接器
		connectionManager.stop();
		//启动节点发现
		peerDiscovery.shutdown();
	}
	
	private void init() {
		connectionManager.setNewInConnectionListener(new NewInConnectionListener() {
			@Override
			public boolean allowConnection(InetSocketAddress inetSocketAddress) {
				//如果已经主动连接了，就不接收该节点的被动连接,节点探测除外
				//一个IP最多只允许2个被动连接
				int count = 0;
				for (Peer peer : outPeers) {
					if(peer.getAddress().getAddr().getHostAddress().equals(inetSocketAddress.getAddress().getHostAddress())) {
						count++;
					}
				}
				if(count >= 2) {
					return false;
				}
				return inPeers.size() < DEFAULT_MAX_IN_CONNECTION;
			}
			@Override
			public void connectionOpened(Peer peer) {
				inPeers.add(peer);
				log.info("新连接{}，当前流入"+inPeers.size()+"个节点 ，最大允许"+PeerKit.this.maxConnectionCount+"个节点 ", peer.getPeerAddress().getSocketAddress());
				
				connectionOnChange(true);
			}
			@Override
			public void connectionClosed(Peer peer) {
				inPeers.remove(peer);
				log.info("连接关闭{}，当前流入"+inPeers.size()+"个节点 ，最大允许"+PeerKit.this.maxConnectionCount+"个节点 ", peer.getPeerAddress().getSocketAddress());
				
				connectionOnChange(false);
			}
		});
	}

	//ping task
	private void startPingTask() {
		executor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				for (Peer peer : outPeers) {
					try {
						peer.ping().get(5, TimeUnit.SECONDS);
					} catch (Exception e) {
						//无法Ping通的就断开吧
						log.info("节点{}无法Ping通，{}", peer.getAddress(), TimeService.currentTimeMillis());
//						if(!network.blockIsNewestStatus()) {
//							peer.close();
//						}
					}
				}
			}
		}, 0, 30, TimeUnit.SECONDS);
	}

	//初始化节点
	private void initPeers() {
		//启动节点探测服务
		executor.scheduleWithFixedDelay(new PeerStatusManager(), 2, 10, TimeUnit.SECONDS);
		
		//检查本机是否需要上报地址
		waitForPeers(minConnectionCount, checkBroadcastAddrListener);
	}
	
	/**
	 * 重置所以节点
	 */
	public void resetPeers() {
		
		for (Peer peer : outPeers) {
			peer.close();
		}
		for (Peer peer : inPeers) {
			peer.close();
		}
		outPeers.clear();
		inPeers.clear();
		peerDiscovery.shutdown();
		try {
			Thread.sleep(1000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		peerDiscovery.startSync();
	}
	
	/*
	 * 检查本机是否需要上报地址到P2p网络中
	 */
	private final EnoughAvailablePeersListener checkBroadcastAddrListener = new EnoughAvailablePeersListener() {
		@Override
		public void callback(List<Peer> peers) {
			new Thread() {
				public void run() {
					//等待10秒，根据对等节点返回的外网地址进行判断是否可以对外提供服务
					try {
						Thread.sleep(10000l);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					peerDiscovery.checkMyserviceAndReport();
				}
			}.start();
		}
	};
	
	/**
	 * 验证节点是否可用
	 * @return boolean
	 */
	public boolean verifyPeer(PeerAddress peerAddress) {
		try {
			final SettableListenableFuture<Boolean> result = new SettableListenableFuture<Boolean>();
			//探测我是否能为外网提供服务
			Peer peer = new Peer(network, peerAddress) {
				@Override
				public void connectionOpened() {
					try {
						ping();
						result.set(true);
					} catch (Exception e) {
						if(log.isDebugEnabled()) {
							log.debug("{} 服务不可用, {}", peerAddress, e.getMessage());
						}
						result.set(false);
					} finally {
						close();
					}
				}
				@Override
				public void connectionClosed() {
					if(!result.isDone()) {
						result.set(false);
					}
				}
			};
			connectionManager.openConnection(new InetSocketAddress(peerAddress.getAddr(), peerAddress.getPort()), peer);
			
			return result.get();
		} catch (Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("error to verify peer with exception {}", e.getMessage());
			}
			return false;
		}
	}
	
	/**
	 * 节点变化
	 */
	public void connectionOnChange(boolean isOpen) {
		for (ConnectionChangedListener connectionChangedListener : connectionChangedListeners) {
			if(executor.isShutdown()) {
				return;
			}
			executor.execute(new Thread(){
				@Override
				public void run() {
					connectionChangedListener.onChanged(inPeers.size(), outPeers.size(), inPeers, outPeers);
				}
			});
		}
	}
	
	//节点状态管理
	public class PeerStatusManager implements Runnable {
		@Override
		public void run() {
			try {
				int availablePeersCount = getAvailablePeersCount();
				if(availablePeersCount >= maxConnectionCount) {
					return;
				}
				
				List<Seed> seedList = peerDiscovery.getCanConnectPeerSeeds(maxConnectionCount - availablePeersCount);
				if(seedList != null && seedList.size() > 0) {
					for (final Seed seed : seedList) {
						//排除与自己的连接
						if(LOCAL_ADDRESS.contains(seed.getAddress().getAddress().getHostAddress())) {
							seed.setStaus(Seed.SEED_CONNECT_FAIL);
							seed.setRetry(false);
							continue;
						}
						
						//判断是否已经进行过连接，和一个ip只保持一个连接
						if(hasConnected(seed.getAddress().getAddress())) {
							seed.setStaus(Seed.SEED_CONNECT_SUCCESS);
							continue;
						}

						Peer peer = new Peer(network, seed.getAddress()) {
							@Override
							public void connectionOpened() {
								super.connectionOpened();
								//连接状态设置为成功
								seed.setStaus(Seed.SEED_CONNECT_SUCCESS);
								peerDiscovery.refreshSeedStatus(seed);
								
								//加入主动连接列表
								outPeers.add(this);
								connectionOnChange(true);
							}
							@Override
							public void connectionClosed() {
								super.connectionClosed();
								//连接状态设置为成功
								if(seed.getStaus() == Seed.SEED_CONNECT_SUCCESS) {
									//连接成功过，那么断开连接
									seed.setStaus(Seed.SEED_CONNECT_CLOSE);
								} else {
									//连接失败
									seed.setStaus(Seed.SEED_CONNECT_FAIL);
								}
								peerDiscovery.refreshSeedStatus(seed);
								//从主动连接列表中移除
								outPeers.remove(this);
								connectionOnChange(false);
							}
						};
						seed.setLastTime(TimeService.currentTimeMillis());
						connectionManager.openConnection(seed.getAddress(), peer);
					}
				}
			} catch (Exception e) {
				log.error("error init peer", e);
			}
		}
	}
	
	/**
	 * 节点是否已经建立对等连接
	 * @param inetAddress
	 * @return boolean
	 */
	public boolean hasConnected(InetAddress inetAddress) {
		boolean hasConnected = false;
		for (Peer peer : inPeers) {
			if(peer.getAddress().getAddr().getHostAddress().equals(inetAddress.getHostAddress())) {
				hasConnected = true;
				break;
			}
		}
		if(!hasConnected) {
			for (Peer peer : outPeers) {
				if(peer.getAddress().getAddr().getHostAddress().equals(inetAddress.getHostAddress())) {
					hasConnected = true;
					break;
				}
			}
		}
		return hasConnected;
	}

	/**
	 * 广播消息
	 * @param message  			要广播的消息
	 * @return BroadcastResult 	广播结果
	 */
	public BroadcastResult broadcast(Message message) {
		return broadcaster.broadcast(message);
	}

	/**
	 * 广播消息，无需等待接收回应
	 * @param message  			要广播的消息
	 * @return int 				通过几个节点广播消息出去
	 */
	public int broadcastMessage(Message message) {
		return broadcaster.broadcastMessage(message);
	}
	
	/**
	 * 广播消息
	 * @param message  			要广播的消息
	 * @param excludePeer  		要排除的节点
	 * @return int	 			通过几个节点广播消息出去
	 */
	public int broadcastMessage(Message message, Peer excludePeer) {
		return broadcaster.broadcastMessage(message, excludePeer);
	}

	/**
	 * 是否能进行广播
	 * @return boolean
	 */
	public boolean canBroadcast() {
		//握手成功可以进行数据交换的节点数量
		int count = 0;
		for (Peer peer : inPeers) {
			if(peer.isHandshake()) {
				count ++;
			}
		}
		for (Peer peer : outPeers) {
			if(peer.isHandshake()) {
				count ++;
			}
		}
		return count > 0;
	}
	
	/**
	 * 获取已连接的节点
	 * @return List<Peer>
	 */
	public List<Peer> getConnectedPeers() {
		List<Peer> peers = new ArrayList<Peer>();
		peers.addAll(inPeers);
		peers.addAll(outPeers);
		return peers;
	}

	/**
	 * 对等体节点连接数量达到一定数量的监听
	 * 达到minConnections时，将调用EnoughAvailablePeersListener.addCallback
	 * @param minConnections
	 * @param enoughAvailablePeersListener 
	 */
	public void waitForPeers(final int minConnections, final EnoughAvailablePeersListener enoughAvailablePeersListener) {
		if(enoughAvailablePeersListener == null) {
			return;
		}
		List<Peer> foundPeers = findAvailablePeers();
		//如果已连接的节点达到所需，则直接执行回调
		if (foundPeers.size() >= minConnections) {
			enoughAvailablePeersListener.callback(foundPeers);
			return;
        }
		addConnectionChangedListener(new ConnectionChangedListener() {
			@Override
			public void onChanged(int inCount, int outCount, CopyOnWriteArrayList<Peer> inPeers,
					CopyOnWriteArrayList<Peer> outPeers) {
				List<Peer> peers = findAvailablePeers();
				if(peers.size() >= minConnections) {
					removeConnectionChangedListener(this);
					enoughAvailablePeersListener.callback(peers);
				}
			}
		});
	}
	
	/**
	 * 已连接的节点列表
	 * 此处不直接调用getConnectedPeers，是因为以后会扩展协议版本节点过滤
	 * @return List<Peer>
	 */
	public List<Peer> findAvailablePeers() {
		List<Peer> results = new ArrayList<Peer>();
		for (Peer peer : inPeers) {
			if(peer.isHandshake()) {
				results.add(peer);
			}
		}
		for (Peer peer : outPeers) {
			if(peer.isHandshake()) {
				results.add(peer);
			}
		}
        return results;
	}
	
	/**
	 * 已连接并完成握手的节点数量
	 * @return int
	 */
	public int getAvailablePeersCount() {
		int count = 0;
		for (Peer peer : inPeers) {
			if(peer.isHandshake()) {
				count++;
			}
		}
		for (Peer peer : outPeers) {
			if(peer.isHandshake()) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * 已连接并完成握手的节点数量
	 * @return int[]
	 */
	public int[] getAvailablePeersCounts() {
		int[] counts = new int[2];
		for (Peer peer : inPeers) {
			if(peer.isHandshake()) {
				counts[0]++;
			}
		}
		for (Peer peer : outPeers) {
			if(peer.isHandshake()) {
				counts[1]++;
			}
		}
		return counts;
	}

	/**
	 * 获取最小的广播连接数
	 * @return int
	 */
	public int getBroadcasterMinConnectionCount() {
		int nowCount = getAvailablePeersCount();
		if(nowCount <= 1) {
			return 1;
		} else {
			return Math.max(1, (int)(nowCount * 0.8));
		}
	}
	
	/**
	 * 已连接的节点数量
	 * @return int
	 */
	public int getConnectedCount() {
        return inPeers.size() + outPeers.size();
	}
	
	/**
	 * 初始化网络时间
	 * 规则：
	 * 	1、只要有一个节点的时间和本地时间差距不超过2s，则以本地时间为准，这样能有效的防止恶意节点的欺骗
	 * 	2、当所有连接的节点时间偏移超过2s，则取多数时间相近的节点时间作为网络时间
	 * @return boolean
	 */
	public boolean hasInitTimeOffset() {
		return initTimeOffset;
	}
	
	
	/**
	 * 处理时间偏移
	 * 本地时间和已连接的时间对比 
	 * 只要有一个节点的时间和本地时间差距不超过2s，则以本地时间为准，这样能有效的防止恶意节点的欺骗
	 * 当所有连接的节点时间偏移超过2s，则取多数时间相近的节点时间作为网络时间
	 * 
	 * @param time
	 * @param timeOffset
	 */
	public void processTimeOffset(long time, long timeOffset) {
		//当完成连接的节点数量小于2时，以本地时间为准
		if(initTimeOffset || getAvailablePeersCount() < 1) {
			return;
		}
		//是否存在时间相似节点
		boolean existsSimilar = false;
		for (Peer peer : inPeers) {
			if(peer.isHandshake() && Math.abs(peer.getTimeOffset()) < TimeService.TIME_OFFSET_BOUNDARY) {
				existsSimilar = true;
				break;
			}
		}
		for (Peer peer : outPeers) {
			if(peer.isHandshake() && Math.abs(peer.getTimeOffset()) < TimeService.TIME_OFFSET_BOUNDARY) {
				existsSimilar = true;
				break;
			}
		}
		if(existsSimilar) {
			initTimeOffset = true;
			return;
		}
		//所有节点的时间偏移都大于零界点，重设本地时间
		long mostTimeOffset = getNetTimeOffsetFromPeers();
		
		TimeService.setNetTimeOffset(mostTimeOffset);
		initTimeOffset = true;
	}
	
	/**
	 * 大多数时间相近节点的时间偏移
	 * @return long
	 */
	private long getNetTimeOffsetFromPeers() {
		List<Peer> peers = findAvailablePeers();
		
		List<TimeItem> list = new ArrayList<TimeItem>();
		
		for (Peer peer : peers) {
			boolean exist = false;
			for (TimeItem item : list) {
				//偏差在设定的 TIME_OFFSET_BOUNDARY 内，则认为相近
				if(Math.abs(item.getTimeOffset() - peer.getTimeOffset()) <= TimeService.TIME_OFFSET_BOUNDARY) {
					item.addCount();
					exist = true;
					break;
				}
			}
			if(!exist) {
				list.add(new TimeItem(peer.getTimeOffset()).addCount());
			}
		}
		
		list.sort(new Comparator<TimeItem>() {
			@Override
			public int compare(TimeItem o1, TimeItem o2) {
				int v1 = o1.getCount();
				int v2 = o2.getCount();
				if(v1 == v2) {
					return 0;
				} else if(v1 > v2) {
					return -1;
				} else {
					return 1;
				}
			}
		});
		return list.get(0).getTimeOffset();
	}
	
	public static class TimeItem {
		private long timeOffset;
		private int count;
		
		public TimeItem(long timeOffset) {
			this.timeOffset = timeOffset;
		}

		public TimeItem addCount() {
			count++;
			return this;
		}

		public long getTimeOffset() {
			return timeOffset;
		}

		public void setTimeOffset(long timeOffset) {
			this.timeOffset = timeOffset;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("TimeItem [timeOffset=");
			builder.append(timeOffset);
			builder.append(", count=");
			builder.append(count);
			builder.append("]");
			return builder.toString();
		}
	}
	
	public BlockChangedListener getBlockChangedListener() {
		return blockChangedListener;
	}

	public void setBlockChangedListener(BlockChangedListener blockChangedListener) {
		this.blockChangedListener = blockChangedListener;
	}
	
	public void addConnectionChangedListener(ConnectionChangedListener connectionChangedListener) {
		this.connectionChangedListeners.add(connectionChangedListener);
	}

	private void removeConnectionChangedListener(ConnectionChangedListener connectionChangedListener) {
		this.connectionChangedListeners.remove(connectionChangedListener);
	}
	
	public void setMinConnectionCount(int minConnectionCount) {
		this.minConnectionCount = minConnectionCount;
	}
	
	public int getMinConnectionCount() {
		return minConnectionCount;
	}
}
