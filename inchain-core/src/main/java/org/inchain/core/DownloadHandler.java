package org.inchain.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.kits.PeerKit;
import org.inchain.message.GetBlocksMessage;
import org.inchain.network.NetworkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 区块同步器
 * @author ln
 *
 */
@Service
public class DownloadHandler {
	
	private final static Logger log = LoggerFactory.getLogger(DownloadHandler.class);
	
	//下载锁，避免多节点重复下载
	private Lock locker = new ReentrantLock();

	private Set<Peer> peers = new ConcurrentSkipListSet<Peer>();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	
	private int synchronousStatus; //1同步中，2同步完成
	private boolean monitorRuning;
	private long currentHeight;
	
	/**
	 * 新节点连接，如果高度比当前节点高，那么下载区块数据
	 * @param peer
	 */
	public void newPeer(final Peer peer) {
		long localHeight = network.getBestBlockHeight();
		
		if(peer.getPeerVersionMessage().bestHeight > localHeight) {

			peers.add(peer);
			
			//当比自己高度更新的节点达到3个及以上 TODO
			if(peers.size() >= 1) {
				//启动下载
				new Thread() {
					public void run() {
						//大多数节点一致的高度
						long bestHeight = getMostPeerBestHeight(peers);
						
						if(synchronousStatus == 0) {
							if(peerKit.getBlockChangedListener() != null) {
								peerKit.getBlockChangedListener().onChanged(-1l, bestHeight, null, null);
							}
							startDownload(bestHeight);
						}
					};
				}.start();
			}
		}
	}

	/*
	 * 下载区块
	 */
	private void startDownload(long bestHeight) {
		locker.lock();
		
		try {
			synchronousStatus = 1;
			
			if(!monitorRuning) {
				monitorRuning = true;
				startMonitor();
			}
			
			//开始下载区块
			if(log.isDebugEnabled()) {
				log.debug("开始同步区块...........");
			}
			
			currentHeight = bestHeight;
			
			long localBestHeight = network.getBestBlockHeight();
			if(bestHeight == localBestHeight) {
				return;
			}
			//高度一致的节点
			Set<Peer> newestPeers = new HashSet<Peer>();
			
			for (Peer peer : peers) {
				if(peer.getPeerVersionMessage().bestHeight == bestHeight) {
					newestPeers.add(peer);
				}
			}
			
			for (Peer peer : newestPeers) {
				try {
					//远程节点的高度
					long remoteBestHeight = peer.getPeerVersionMessage().bestHeight;
					localBestHeight = network.getBestBlockHeight();
					
					int downCount = 0;
					while(remoteBestHeight > localBestHeight) {
						long diffHeight = remoteBestHeight - localBestHeight;
						long count = diffHeight >= 10 ? 10: diffHeight;
						
						peer.sendMessage(new GetBlocksMessage(network, localBestHeight, count));
						
						localBestHeight += count;
						
						downCount += 10;
						if(downCount % 100 == 0) {
//							Thread.sleep(1000l);
						}
					}
				} catch (Exception e) {
					log.warn("下载区块出错，更换节点下载", e);
				}
//				if(localBestHeight == bestHeight) {
//					//下载完成
//					if(peer.getPeerVersionMessage().bestHeight == bestHeight) {
//						break;
//					} else {
//						bestHeight = peer.getPeerVersionMessage().bestHeight;
//					}
//				}
			}
		} finally {
			synchronousStatus = 2;
			locker.unlock();
		}
	}

	private void startMonitor() {
		new Thread() {
			public void run() {
				int synchronousCount = 0;
				while(true) {
					if(network.getBestHeight() > currentHeight && synchronousStatus == 2) {
						new Thread() {
							public void run() {
								startDownload(network.getBestHeight());
							};
						}.start();
					} else if(network.getBestHeight() == currentHeight) {
						synchronousCount++;
						if(synchronousCount > 60) {
							if(log.isDebugEnabled()) {
								log.debug("====================区块同步完成=================");
							}
							break;
						}
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	/**
	 * 大多数节点一致的高度
	 * @param peers
	 * @return long
	 */
	public static long getMostPeerBestHeight(Set<Peer> peers) {
		if(peers.size() == 0) {
			return 0l;
		}
		
		List<Item> list = new ArrayList<Item>();
		
		for (Peer peer : peers) {
			if(peer.getPeerVersionMessage() == null) {
				continue;
			}
			long height = peer.getPeerVersionMessage().bestHeight;
			boolean exist = false;
			for (Item item : list) {
				if(item.getHeight() == height) {
					item.addCount();
					exist = true;
					break;
				}
			}
			if(!exist) {
				list.add(new Item(height).addCount());
			}
		}
		
		if(list.size() == 0) {
			return 0l;
		}
		
		list.sort(new Comparator<Item>() {
			@Override
			public int compare(Item o1, Item o2) {
				return o1.getCount() > o2.getCount() ? 1:-1;
			}
		});
		return list.get(0).getHeight();
	}
	
	public static class Item {
		private long height;
		private int count;
		
		public Item(long height) {
			this.height = height;
		}

		public Item addCount() {
			count++;
			return this;
		}
		
		public long getHeight() {
			return height;
		}
		public void setHeight(long height) {
			this.height = height;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		
		@Override
		public String toString() {
			return String.valueOf(height);
		}
	}
}
