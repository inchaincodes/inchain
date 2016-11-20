package org.inchain.kits;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.core.Peer;
import org.inchain.core.TransactionBroadcast;
import org.inchain.core.TransactionBroadcaster;
import org.inchain.message.BlockMessage;
import org.inchain.net.ClientConnectionManager;
import org.inchain.net.ConnectionListener;
import org.inchain.net.NewInConnectionListener;
import org.inchain.net.NioClientManager;
import org.inchain.network.NetworkParameters;
import org.inchain.network.Seed;
import org.inchain.network.SeedManager;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;
import org.slf4j.LoggerFactory;

/**
 * 节点管理
 * @author ln
 *
 */
public class PeerKit implements TransactionBroadcaster {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PeerKit.class);

	//默认最大节点连接数，这里指单向连接
	private static final int DEFAULT_MAX_CONNECTION = 10;
	
	//网络
	private NetworkParameters network;
	//连接变化监听器
	private ConnectionListener connectionListener;
	//被动连接节点
	private CopyOnWriteArrayList<Peer> inPeers = new CopyOnWriteArrayList<Peer>();
	//主动连接节点
	private CopyOnWriteArrayList<Peer> outPeers = new CopyOnWriteArrayList<Peer>();
	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
	
	//连接管理器
	private final ClientConnectionManager connectionManager;
	//最大连接数
	private int maxConnectionCount;
	
	public PeerKit(NetworkParameters params) {
		this(params, DEFAULT_MAX_CONNECTION);
	}
	
	public PeerKit(NetworkParameters params,int maxConnectionCount) {
		this.network = params;
		this.maxConnectionCount = maxConnectionCount;
		this.connectionManager = new NioClientManager(params, true, params.getPort());
		connectionManager.setNewInConnectionListener(new NewInConnectionListener() {
			@Override
			public boolean allowConnection() {
				return inPeers.size() < PeerKit.this.maxConnectionCount;
			}
			@Override
			public void connectionOpened(Peer peer) {
				inPeers.add(peer);
				log.info("新连接{}，当前流入"+inPeers.size()+"个节点 ，最大允许"+PeerKit.this.maxConnectionCount+"个节点 ", peer.getPeerAddress().getSocketAddress());
			}
			@Override
			public void connectionClosed(Peer peer) {
				inPeers.remove(peer);
				log.info("连接关闭{}，当前流入"+inPeers.size()+"个节点 ，最大允许"+PeerKit.this.maxConnectionCount+"个节点 ", peer.getPeerAddress().getSocketAddress());
			}
		});
	}

	//启动服务
	private void start() {
		
		Utils.checkNotNull(network);
		Utils.checkNotNull(connectionManager);
		
		//初始化连接器
		connectionManager.start();
		
		//初始化节点
		initPeers();
		
		//ping task
		startPingTask();
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
		executor.scheduleWithFixedDelay(new PeerStatusManager(), 0, 10, TimeUnit.SECONDS);
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
		//关闭连接器
		connectionManager.stop();
		//关闭服务
		executor.shutdown();
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
							}
							@Override
							public void connectionClosed() {
								super.connectionClosed();
								outPeers.remove(this);
							}
						};
						seed.setLastTime(System.currentTimeMillis());
						connectionManager.openConnection(seed, peer);
					}
				}
			} catch (Exception e) {
				log.error("error init peer", e);
			}
		}
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
	
	public void setConnectionListener(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}
}
