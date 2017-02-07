package org.inchain.msgprocess;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.core.BroadcastContext;
import org.inchain.core.BroadcastResult;
import org.inchain.core.GetDataResult;
import org.inchain.core.Peer;
import org.inchain.filter.InventoryFilter;
import org.inchain.kits.PeerKit;
import org.inchain.message.Block;
import org.inchain.message.GetDatasMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 向量清单消息处理器
 * @author ln
 *
 */
@Service
public class InventoryMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(InventoryMessageProcess.class);
	
	//由于新区块的同步，需要线程阻塞，等待同步完成通知，避免重复下载，所以加入一个固定的线程池，异步执行，以免阻塞影响其它消息的接收执行
	private static final ExecutorService blockExecutorService = Executors.newSingleThreadExecutor();
	private static final ExecutorService txExecutorService = Executors.newSingleThreadExecutor();

	//区块下载锁
	private static Lock blockLocker = new ReentrantLock();
	//交易下载锁
	private static Lock txLocker = new ReentrantLock();

	@Autowired
	private InventoryFilter filter;
	@Autowired
	private PeerKit peerKit;
	
	
	@Override
	public MessageProcessResult process(final Message message, final Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive inv message: {}", message);
		}
		
		InventoryMessage invMessage = (InventoryMessage) message;
		
		List<InventoryItem> invList = invMessage.getInvs();
		if(invList == null) {
			return null;
		}
		
		for (final InventoryItem inventoryItem : invList) {
			processInventoryItem(peer, inventoryItem);
		}
		
		return null;
	}

	//处理明细
	private void processInventoryItem(final Peer peer, final InventoryItem inventoryItem) {
		//如果已经接收并处理过了，就跳过
		if(filter.contains(inventoryItem.getHash().getBytes())) {
			return;
		} else if(inventoryItem.getType() == InventoryItem.Type.NewBlock) {
			//新区块诞生
			blockExecutorService.submit(new Runnable() {
				@Override
				public void run() {
					newBlockInventory(inventoryItem, peer);
				}
			});
		} else if(inventoryItem.getType() == InventoryItem.Type.Transaction) {
			//交易的inv
			txExecutorService.submit(new Runnable() {
				@Override
				public void run() {
					txInventory(inventoryItem, peer);
				}
			});
		} else {
			//默认处理
			filter.insert(inventoryItem.getHash().getBytes());
			doProcessInventoryItem(inventoryItem);
		}
	}

	/*
	 * 交易的inv
	 */
	protected void txInventory(InventoryItem inventoryItem, Peer peer) {
		txLocker.lock();
		try {
			if(filter.contains(inventoryItem.getHash().getBytes())) {
				return;
			}
			//当本地有广播结果等待时，代表该交易由我广播出去的，则不下载
			boolean needDown = false;
			
			BroadcastResult broadcastResult = BroadcastContext.get().get(inventoryItem.getHash());
			if(broadcastResult == null) {
				needDown = true;
			} else {
				broadcastResult.addReply(peer);
			}
			if(needDown) {
				//下载交易
				Future<GetDataResult> resultFuture = peer.sendGetDataMessage(new GetDatasMessage(peer.getNetwork(), inventoryItem));
				
				//获取下载结果，60s超时
				GetDataResult result = resultFuture.get(10, TimeUnit.SECONDS);
				
				if(result.isSuccess()) {
					if(log.isDebugEnabled()) {
						log.debug("交易{}获取成功", inventoryItem.getHash());
					}
					filter.insert(inventoryItem.getHash().getBytes());
				}
			}
		} catch (Exception e) {
			log.error("交易inv消息处理失败 {}", inventoryItem, e);
		} finally {
			txLocker.unlock();
		}
	}

	/*
	 * 新区块诞生，下载
	 */
	private void newBlockInventory(final InventoryItem inventoryItem, Peer peer) {
		blockLocker.lock();
		try {
			if(filter.contains(inventoryItem.getHash().getBytes())) {
				return;
			}
			Future<GetDataResult> resultFuture = peer.sendGetDataMessage(new GetDatasMessage(peer.getNetwork(), inventoryItem));
			
			//获取下载结果，有超时时间
			GetDataResult result = resultFuture.get(30, TimeUnit.SECONDS);
			
			if(result.isSuccess()) {
				Block block = (Block) result.getData();
				
				filter.insert(inventoryItem.getHash().getBytes());
				if(log.isDebugEnabled()) {
					log.debug("新区快 高度:{} hash:{} 同步成功", block.getHeight(), inventoryItem.getHash());
				}
				//区块变化监听器
				if(peerKit.getBlockChangedListener() != null) {
					peerKit.getBlockChangedListener().onChanged(-1l, block.getHeight(), null, inventoryItem.getHash());
				}
			}
			
		} catch (Exception e) {
			log.error("新区快inv消息处理失败 {}", inventoryItem, e);
		} finally {
			blockLocker.unlock();
		}
	}

	private void doProcessInventoryItem(InventoryItem inventoryItem) {
		// TODO Auto-generated method stub
		
	}

}
