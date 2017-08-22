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

import org.inchain.consensus.ConsensusMeeting;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.listener.BlockDownendListener;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.message.GetBlocksMessage;
import org.inchain.msgprocess.BlockMessageProcess;
import org.inchain.msgprocess.GetBlocksMessageProcess;
import org.inchain.msgprocess.MessageProcessResult;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.SettableListenableFuture;

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
	//区块同步完成监听器
	private List<BlockDownendListener> blockDownendListeners = new CopyOnWriteArrayList<BlockDownendListener>();
		
	@Autowired
	private NetworkParams network;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private BlockMessageProcess blockMessageProcess;
	
	private int synchronousStatus = -1; //0等待同步，1同步中，2同步完成
	private boolean initSynchronous = true; //是否初始同步，也就是第一次程序启动的同步
	
	//下载消息
	private GetBlocksMessage downingMessage;
	//同步开始高度
	private long startHeight;
	//同步停止高度
	private long stopHeight;
	//最后同步块hash
	private Sha256Hash lastDownHash;
	//本地最新块是否是分叉块
	private boolean localBestBlockIsFork;
	//下载监听
	private SettableListenableFuture<Boolean> downloadFuture;
	
	/**
	 * 初始化，监听节点连接变化
	 */
	@PostConstruct
	public void init() {
		//监听节点变化
		peerKit.addConnectionChangedListener(new ConnectionChangedListener() {
			@Override
			public void onChanged(int inCount, int outCount, int superCount ,CopyOnWriteArrayList<Peer> inPeers,
					CopyOnWriteArrayList<Peer> outPeers,CopyOnWriteArrayList<Peer> superPeers) {
				
				executor.execute(new Runnable() {
					@Override
					public void run() {
						locker.lock();
						try {
							peers.clear();
							peers.addAll(superPeers);
							//peers.addAll(inPeers);
							//peers.addAll(outPeers);
							
							//当所有节点全部断开时，将重新启动监听，因为自动重新连上后需要同步断开这段时间的新区块
							if(inCount + outCount + superCount == 0) {
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
			}
		});
		//启动数据同步服务
		executor.scheduleWithFixedDelay(this , 3, 10, TimeUnit.SECONDS);
	}
	
	/**
	 * 执行下同步监听
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
		network.setBestHeight(bestHeight);
		
		if(peerKit.getBlockChangedListener() != null) {
			peerKit.getBlockChangedListener().onChanged(-1l, bestHeight, null, null);
		}
		
		if(localHeight >= bestHeight) {
			//本地最新区块
			BlockHeader bestBlockHeader = network.getBestBlockHeader();
			//触发监听器
			for (BlockDownendListener blockDownendListener : blockDownendListeners) {
				blockDownendListener.downend(localHeight);
			}
			//同步完成之后，检查区块是否是最优的，有没有被恶意节点误导引起分叉
			//TODO
			//重置当前轮共识会议
			consensusMeeting.resetCurrentMeetingItem();
			//验证共识人数是否正确
			int currentMeetingPeriodCount = consensusMeeting.getCurrentMeetingPeriodCount();
			if(currentMeetingPeriodCount != bestBlockHeader.getPeriodCount()) {
				//共识队列有问题，需要重置
				blockStoreProvider.resetConsensusQueue();
				consensusMeeting.resetCurrentMeetingItem();
			}
			synchronousStatus = 2;
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
			
			BlockHeader blockHeader = network.getBestBlockHeader();
			if(bestHeight <= blockHeader.getHeight()) {
				initSynchronous = false;
				return;
			}
			//高度一致的节点
			Set<Peer> newestPeers = new HashSet<Peer>();
			
			for (Peer peer : peers) {
				if((initSynchronous && peer.getPeerVersionMessage() != null && peer.getPeerVersionMessage().getBestHeight() == bestHeight) ||
						(!initSynchronous && peer.getBestBlockHeight() == bestHeight)) {
					newestPeers.add(peer);
				}
			}
			List<Boolean> results = new ArrayList<Boolean>();
			
			//假设本地最新区块时分叉块，在同步过程中只要发现一个能衔接上的，就不是分叉了
			localBestBlockIsFork = true;
			
			for (Peer peer : newestPeers) {
				try {
					while(true) {
						blockHeader = network.getBestBlockHeader();
						if((initSynchronous && peer.getPeerVersionMessage().getBestHeight() <= blockHeader.getHeight()) ||
								(!initSynchronous && peer.getBestBlockHeight() <= blockHeader.getHeight())) {
							break;
						}
						Sha256Hash startHash = blockHeader.getHash();
						
						Sha256Hash stopHash = Sha256Hash.ZERO_HASH;
						
						downingMessage = new GetBlocksMessage(network, startHash, stopHash);
						startHeight = blockHeader.getHeight();
						stopHeight = bestHeight;
						downloadFuture = new SettableListenableFuture<Boolean>();
						
						peer.sendMessage(downingMessage);
						
						try {
							boolean result = downloadFuture.get(600, TimeUnit.SECONDS);
							if(result) {
								results.add(result);
							} else {
								results.add(result);
								break;
							}
						} catch (Exception e) {
							results.add(false);
							break;
						}
						
//						boolean result = peer.waitBlockDownComplete(startHash);
//						results.add(result);
//						if(!result) {
//							break;
//						}
						
					}
				} catch (Exception e) {
					results.add(false);
					log.warn("下载区块出错，更换节点下载", e);
				}
			}
			
			//如果都失败了，说明我本地的最新区块是分叉块，需要更换处理
			boolean fail = true;
			for (Boolean result : results) {
				if(result) {
					fail = false;
					break;
				}
			}
			if(fail) {
				//下载失败，判断最新块是否是分叉块
				if(localBestBlockIsFork) {
					log.error("同步区块出错：本地最新块可能是分叉块");
					//撤销本地最新块重试
					blockStoreProvider.revokedNewestBlock();
				} else {
					//不是分叉块，那么很有可能是本地数据损坏了，造成不能同步
					blockStoreProvider.resetData();
				}
			}
			Thread t = new Thread() {
				@Override
				public void run() {
					startSynchronous();
				}
			};
			t.setName("block download service");
			t.start();
			
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
	 * 同步是否完成
	 * @return boolean
	 */
	public boolean isDownloading() {
		return synchronousStatus == 1;
	}

	/**
	 * 停止同步服务
	 */
	public void stop() {
		synchronousStatus = 2;
	}
	
	/**
	 * 设置为下载中
	 */
	public void downloading() {
		synchronousStatus = 1;
	}
	
	/**
	 * 重置下载服务
	 */
	public void reset() {
		synchronousStatus = 0;
	}
	
	/**
	 * 处理下载的区块
	 * @param block
	 */
	public void processData(Block block) {
		if(downingMessage != null) {
			if(localBestBlockIsFork && block.getPreHash().equals(downingMessage.getStartHash())) {
				localBestBlockIsFork = false;
			}
			if(block.getPreHash().equals(downingMessage.getStartHash()) || (lastDownHash != null && lastDownHash.equals(block.getPreHash()))) {
				MessageProcessResult processResult = blockMessageProcess.process(block, null);
				if(!processResult.isSuccess()) {
					downloadFuture.set(false);
					return;
				}
				
				lastDownHash = block.getHash();
				
				if(downingMessage.getStopHash().equals(Sha256Hash.ZERO_HASH)) {
					if((block.getHeight() - startHeight >= GetBlocksMessageProcess.MAX_COUNT) || block.getHeight() == stopHeight) {
						downloadFuture.set(true);
					}
				} else if(downingMessage.getStopHash().equals(block.getHash())) {
					downloadFuture.set(true);
				}
			}
		}
	}
	
	/**
	 * 当收到对等节点返回数据没有找到的消息时，调用该方法触发结果
	 * 如果为拉取区块没有找到，则设置下载失败
	 */
	public void dataNotFoundCheck(Sha256Hash hash) {
		if(downloadFuture != null && downingMessage != null && downingMessage.getStartHash().equals(hash)) {
			downloadFuture.set(false);
		}
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
			if(peer.getPeerVersionMessage() == null || !peer.isHandshake()) {
				continue;
			}
			//根据hash判断，则获取初始连接时的版本信息里面的高度和hash
			if(equalAsHash) {
				long height = peer.getPeerVersionMessage().getBestHeight();
				Sha256Hash hash = peer.getPeerVersionMessage().getBestBlockHash();
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
		return list.get(0).getHeight();
	}
	
	/**
	 * 添加区块同步完成监听器
	 * @param blockDownendListener
	 */
	public void addblockDownendListener(BlockDownendListener blockDownendListener) {
		blockDownendListeners.add(blockDownendListener);
	}
	
	/**
	 * 删除区块同步完成监听器
	 * @param blockDownendListener
	 */
	public void removeblockDownendListener(BlockDownendListener blockDownendListener) {
		blockDownendListeners.remove(blockDownendListener);
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
