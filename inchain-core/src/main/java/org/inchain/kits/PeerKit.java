package org.inchain.kits;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.Configure;
import org.inchain.core.Broadcaster;
import org.inchain.core.Peer;
import org.inchain.core.TimeHelper;
import org.inchain.core.TransactionBroadcast;
import org.inchain.listener.BlockChangedListener;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.listener.NewInConnectionListener;
import org.inchain.message.BlockMessage;
import org.inchain.message.Message;
import org.inchain.net.ClientConnectionManager;
import org.inchain.network.NetworkParams;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.transaction.Transaction;
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
public class PeerKit implements Broadcaster {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PeerKit.class);

	//默认最大节点连接数，这里指单向连接
	private static final int DEFAULT_MAX_CONNECTION = 10;
	
	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
	
	//网络
	@Autowired
	private NetworkParams network;
	//连接变化监听器
	private ConnectionChangedListener connectionChangedListener;
	
	//区块变化监听器
	private BlockChangedListener blockChangedListener;
	
	//被动连接节点
	private CopyOnWriteArrayList<Peer> inPeers = new CopyOnWriteArrayList<Peer>();
	//主动连接节点
	private CopyOnWriteArrayList<Peer> outPeers = new CopyOnWriteArrayList<Peer>();
	
	//连接管理器
	@Autowired
	private ClientConnectionManager connectionManager;
	//最大连接数
	private int maxConnectionCount = Configure.MAX_CONNECT_COUNT;
	
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
				
				connectionOnChange();
			}
			@Override
			public void connectionClosed(Peer peer) {
				inPeers.remove(peer);
				log.info("连接关闭{}，当前流入"+inPeers.size()+"个节点 ，最大允许"+PeerKit.this.maxConnectionCount+"个节点 ", peer.getPeerAddress().getSocketAddress());
				
				connectionOnChange();
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
	public void connectionOnChange() {
		if(connectionChangedListener != null) {
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
								connectionOnChange();
							}
							@Override
							public void connectionClosed() {
								super.connectionClosed();
								outPeers.remove(this);
								connectionOnChange();
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
	 * @param message  要广播的消息
	 * @return boolean 返回广播是否成功
	 */
	public boolean broadcastMessage(Message message) {
		if(inPeers.size() > 0 || outPeers.size() > 0) {
			for (Peer peer : inPeers) {
				peer.sendMessage(message);
			}
			for (Peer peer : outPeers) {
				peer.sendMessage(message);
			}
			return true;
		} else {
			log.warn("广播消息失败，没有可广播的节点");
		}
		return false;
	}

	/**
	 * 广播消息
	 * @param message  要广播的消息
	 * @param message  要排除的节点
	 * @return int 成功广播给几个节点
	 */
	public int broadcastMessage(Message message, Peer excludePeer) {
		int successCount = 0;
		if(inPeers.size() > 0 || outPeers.size() > 0) {
			for (Peer peer : inPeers) {
				if(excludePeer!= null && !peer.equals(excludePeer)) {
					peer.sendMessage(message);
					successCount ++;
				}
			}
			for (Peer peer : outPeers) {
				if(excludePeer!= null && !peer.equals(excludePeer)) {
					peer.sendMessage(message);
					successCount ++;
				}
			}
			return successCount;
		} else {
			log.warn("广播消息失败，没有可广播的节点");
		}
		if(log.isDebugEnabled()) {
			log.debug("成功广播给{}个节点，消息{}", successCount, message);
		}
		return successCount;
	}
	
	/**
	 * 广播区块
	 * @param block
	 */
	public void broadcastBlock(BlockMessage block) {
		//TODO
		if(inPeers.size() > 0 || outPeers.size() > 0) {
			for (Peer peer : inPeers) {
				peer.sendMessage(block);
			}
			for (Peer peer : outPeers) {
				peer.sendMessage(block);
			}
		} else {
			log.warn("广播新区块失败，没有可广播的节点");
		}
	}
	
	/**
	 * 广播交易消息
	 */
	@Override
	public TransactionBroadcast broadcastTransaction(Transaction tx) {
		//等待1分钟，如果还没有可用的连接，就结束返回失败
		int retryCount = 1;
		while(retryCount-- > 0) {
			if(inPeers.size() > 0 || outPeers.size() > 0) {
				for (Peer peer : inPeers) {
					peer.sendMessage(tx);
				}
				for (Peer peer : outPeers) {
					peer.sendMessage(tx);
				}
				return null;
			}
			try{
				Thread.sleep(1000l);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		log.info("消息发送失败，没有可广播的节点");
		return null;
	}

	/**
	 * 是否能进行广播
	 * @return boolean
	 */
	public boolean canBroadcast() {
		return inPeers.size() > 0 || outPeers.size() > 0;
	}
	
	public BlockChangedListener getBlockChangedListener() {
		return blockChangedListener;
	}

	public void setBlockChangedListener(BlockChangedListener blockChangedListener) {
		this.blockChangedListener = blockChangedListener;
	}
	
	public void setConnectionChangedListener(ConnectionChangedListener connectionChangedListener) {
		this.connectionChangedListener = connectionChangedListener;
	}
	
	public ConnectionChangedListener getConnectionChangedListener() {
		return connectionChangedListener;
	}
}
