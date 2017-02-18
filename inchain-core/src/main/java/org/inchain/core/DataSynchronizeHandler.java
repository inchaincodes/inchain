package org.inchain.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.message.BlockHeader;
import org.inchain.message.GetBlocksMessage;
import org.inchain.network.NetworkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据同步器
 * @author ln
 *
 */
@Service
public class DataSynchronizeHandler implements Runnable {
	
	private final static Logger log = LoggerFactory.getLogger(DataSynchronizeHandler.class);
	
	//下载锁，避免多节点重复下载
	private Lock locker = new ReentrantLock();

	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	
	//所有已连接的节点
	private CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<Peer>();
		
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	
	private int synchronousStatus = -1; //0等待同步，1同步中，2同步完成
	private boolean initSynchronous = true; //是否初始同步，也就是第一次程序启动的同步
	
	/**
	 * 初始化，监听节点连接变化
	 */
	@PostConstruct
	public void init() {
		//监听节点变化
		peerKit.addConnectionChangedListener(new ConnectionChangedListener() {
			@Override
			public void onChanged(int inCount, int outCount, CopyOnWriteArrayList<Peer> inPeers,
					CopyOnWriteArrayList<Peer> outPeers) {
				locker.lock();
				try {
					peers.clear();
					peers.addAll(inPeers);
					peers.addAll(outPeers);
					
					//当所有节点全部断开时，将重新启动监听，因为自动重新连上后需要同步断开这段时间的新区块
					if(inCount + outCount == 0) {
						synchronousStatus = -1;
						initSynchronous = true;
					} else if(synchronousStatus == -1) {
						synchronousStatus = 0;
					}
				} finally {
					locker.unlock();
				}
			}
		});
		//启动数据同步服务
		executor.scheduleWithFixedDelay(this , 5, 10, TimeUnit.SECONDS);
	}
	
	/**
	 * 新节点连接，如果高度比当前节点高，那么下载区块数据
	 * @param peer
	 */
	public void run() {
		locker.lock();
		try {
			//如果当前正在下载中或者已下载完成，则直接返回
			//节点数量至少到达3个才下载？ TODO
			if(synchronousStatus != 0 || peers.isEmpty()) {
				return;
			}
			startSynchronous();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			locker.unlock();
		}
	}

	//开始同步
	private void startSynchronous() {
		
		long localHeight = network.getBestBlockHeight();
		
		//大多数节点一致的高度，根据最新的hash来判断，以免恶意节点误导
		long bestHeight = getMostPeerBestHeight(peers, initSynchronous);
		
		if(peerKit.getBlockChangedListener() != null) {
			peerKit.getBlockChangedListener().onChanged(-1l, bestHeight, null, null);
		}
		
		if(localHeight >= bestHeight) {
			synchronousStatus = 2;
			//同步完成之后，检查区块是否是最优的，有没有被恶意节点误导引起分叉
			//TODO
			if(log.isDebugEnabled()) {
				log.debug("区块同步完成！");
			}
		} else {
			synchronousStatus = 1;
			startDownload(bestHeight);
		}
	}

	/*
	 * 下载区块
	 */
	private void startDownload(long bestHeight) {
		try {
			
			//开始下载区块
			if(log.isDebugEnabled()) {
				log.debug("开始同步区块...........");
			}
			
			BlockHeader blockHeader = network.getBestBlockHeader().getBlockHeader();
			if(bestHeight == blockHeader.getHeight()) {
				initSynchronous = false;
				startSynchronous();
				return;
			}
			//高度一致的节点
			Set<Peer> newestPeers = new HashSet<Peer>();
			
			for (Peer peer : peers) {
				if((initSynchronous && peer.getPeerVersionMessage().bestHeight == bestHeight) ||
						(!initSynchronous && peer.getBestBlockHeight() == bestHeight)) {
					newestPeers.add(peer);
				}
			}
			
			for (Peer peer : newestPeers) {
				try {
					while(true) {
						blockHeader = network.getBestBlockHeader().getBlockHeader();
						if((initSynchronous && peer.getPeerVersionMessage().bestHeight <= blockHeader.getHeight()) ||
								(!initSynchronous && peer.getBestBlockHeight() <= blockHeader.getHeight())) {
							break;
						}
						Sha256Hash startHash = blockHeader.getHash();
						Sha256Hash stopHash = Sha256Hash.ZERO_HASH;
						peer.sendMessage(new GetBlocksMessage(network, startHash, stopHash));
						peer.setListenerGetBlocks(startHash, stopHash);
						peer.waitBlockDownComplete();
					}
				} catch (Exception e) {
					log.warn("下载区块出错，更换节点下载", e);
				}
			}
			
			initSynchronous = false;
			startSynchronous();
			
		} catch (Exception e) {
			log.error("区块同步出错 {}", e.getMessage(), e);
		}
	}
	
	/**
	 * 同步是否完成
	 * @return boolean
	 */
	public boolean hasComplete() {
		return synchronousStatus == 2;
	}

	/**
	 * 大多数节点一致的高度
	 * @param peers
	 * @param equalAsHash 是否根据hash来判断
	 * @return long
	 */
	public static long getMostPeerBestHeight(CopyOnWriteArrayList<Peer> peers, boolean equalAsHash) {
		if(peers.size() == 0) {
			return 0l;
		}
		
		List<Item> list = new ArrayList<Item>();
		
		for (Peer peer : peers) {
			if(peer.getPeerVersionMessage() == null) {
				continue;
			}
			//根据hash判断，则获取初始连接时的版本信息里面的高度和hash
			if(equalAsHash) {
				long height = peer.getPeerVersionMessage().bestHeight;
				Sha256Hash hash = peer.getPeerVersionMessage().bestBlockHash;
				boolean exist = false;
				for (Item item : list) {
					if(item.getHeight() == height && item.getHash().equals(hash)) {
						item.addCount();
						exist = true;
						break;
					}
				}
				if(!exist) {
					list.add(new Item(height, hash).addCount());
				}
			} else {
				//不根据hash判断的话，则获取peer里面的最新高度记录，通常用户初始同步之后的少量新增块的同步
				long height = peer.getBestBlockHeight();
				boolean exist = false;
				for (Item item : list) {
					if(item.getHeight() == height) {
						item.addCount();
						exist = true;
						break;
					}
				}
				if(!exist) {
					list.add(new Item(height, null).addCount());
				}
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
		private Sha256Hash hash;
		private int count;
		
		public Item(long height, Sha256Hash hash) {
			this.height = height;
			this.hash = hash;
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
		
		public Sha256Hash getHash() {
			return hash;
		}

		public void setHash(Sha256Hash hash) {
			this.hash = hash;
		}

		@Override
		public String toString() {
			return String.valueOf(height) + " , " + hash + ", "+count;
		}
	}
}
