package org.inchain.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	private Set<Peer> peers = new HashSet<Peer>();
	
	@Autowired
	private NetworkParams network;
	
	/**
	 * 新节点连接，如果高度比当前节点高，那么下载区块数据
	 * @param peer
	 */
	public void newPeer(final Peer peer) {
		long localHeight = network.getBestBlockHeight();
		
		if(peer.getPeerVersionMessage().bestHeight > localHeight) {
			peers.add(peer);
			
			//当比自己高度更新的节点达到3个及以上
			if(peers.size() >= 1) {
				//启动下载
				new Thread() {
					public void run() {
						log.info("开始同步区块数据...........");
						startDownload();
					};
				}.start();
			}
		}
	}

	/*
	 * 下载区块
	 */
	private void startDownload() {
		locker.lock();
		
		try {
			
			//开始下载区块
			log.info("开始下载...........");
			
			//大多数节点一致的高度
			long bestHeight = getMostPeerBestHeight();
			
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
							Thread.sleep(1000l);
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
			locker.unlock();
		}
	}

	/*
	 * 大多数节点一致的高度
	 */
	private long getMostPeerBestHeight() {
		if(peers.size() == 0) {
			return 0l;
		}
		
		List<Item> list = new ArrayList<Item>();
		
		for (Peer peer : peers) {
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
		
		list.sort(new Comparator<Item>() {
			@Override
			public int compare(Item o1, Item o2) {
				return o1.getCount() > o2.getCount() ? 1:-1;
			}
		});
		return list.get(0).getHeight();
	}
	
	public class Item {
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
