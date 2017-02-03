package org.inchain.kits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.Configure;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Broadcaster;
import org.inchain.core.Peer;
import org.inchain.core.TimeHelper;
import org.inchain.listener.BlockChangedListener;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.listener.EnoughAvailablePeersListener;
import org.inchain.listener.NewInConnectionListener;
import org.inchain.message.Message;
import org.inchain.net.ClientConnectionManager;
import org.inchain.network.NetworkParams;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.utils.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 节点管理
 * @author ln
 *
 */
@Service
public class PeerKit {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PeerKit.class);

	//默认最大节点连接数，这里指单向连接
	private static final int DEFAULT_MAX_CONNECTION = 10;
	
	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
	
	//最大连接数
	private int maxConnectionCount = Configure.MAX_CONNECT_COUNT;
	//连接变化监听器
	private CopyOnWriteArrayList<ConnectionChangedListener> connectionChangedListeners = new CopyOnWriteArrayList<ConnectionChangedListener>();
	
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
	
	public PeerKit() {
		
	}

	//启动服务
	private void start() {
		
		Utils.checkNotNull(network);
		Utils.checkNotNull(connectionManager);
		
		init();
		
		//初始化连接器
		connectionManager.start();
		
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
	}
	
	private void init() {
		connectionManager.setNewInConnectionListener(new NewInConnectionListener() {
			@Override
			public boolean allowConnection() {
				return inPeers.size() < PeerKit.this.maxConnectionCount;
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
					peer.ping();
				}
			}
		}, 0, 30, TimeUnit.SECONDS);
	}

	//初始化节点
	private void initPeers() {
		executor.scheduleWithFixedDelay(new PeerStatusManager(), 2, 10, TimeUnit.SECONDS);
	}
	
	/*
	 * 节点变化
	 */
	public void connectionOnChange(boolean isOpen) {
		for (ConnectionChangedListener connectionChangedListener : connectionChangedListeners) {
			connectionChangedListener.onChanged(inPeers.size(), outPeers.size(), inPeers, outPeers);
		}
	}
	
	//节点状态管理
	public class PeerStatusManager implements Runnable {
		@Override
		public void run() {
			try {
				if(outPeers.size() >= maxConnectionCount) {
					return;
				}
				//是否有更多的种子
				SeedManager seedManager = network.getSeedManager();
				Utils.checkNotNull(seedManager);
				
				if(seedManager.hasMore()) {
					List<Seed> seeds = network.getSeedManager().getSeedList(maxConnectionCount);
					for (Seed seed : seeds) {
						Peer peer = new Peer(network, seed.getAddress()) {
							@Override
							public void connectionOpened() {
								super.connectionOpened();
								outPeers.add(this);
								connectionOnChange(true);
							}
							@Override
							public void connectionClosed() {
								super.connectionClosed();
								outPeers.remove(this);
								connectionOnChange(false);
							}
						};
						seed.setLastTime(TimeHelper.currentTimeMillis());
						connectionManager.openConnection(seed, peer);
					}
				}
			} catch (Exception e) {
				log.error("error init peer", e);
			}
		}
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
		return inPeers.size() > 0 || outPeers.size() > 0;
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
	 * @return EnoughAvailablePeersListener
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
					enoughAvailablePeersListener.callback(foundPeers);
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
			results.add(peer);
		}
		for (Peer peer : outPeers) {
			results.add(peer);
		}
        return results;
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
}
