package org.inchain.msgprocess;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.core.Peer;
import org.inchain.filter.InventoryFilter;
import org.inchain.listener.BlockDownendListener;
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
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	@Autowired
	private InventoryFilter filter;
	
	//区块下载锁
	private static Lock blockLocker = new ReentrantLock();
	
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive inv message: {}", message);
		}
		
		InventoryMessage invMessage = (InventoryMessage) message;
		
		List<InventoryItem> invList = invMessage.getInvs();
		if(invList == null) {
			return null;
		}
		
		for (InventoryItem inventoryItem : invList) {
			if(inventoryItem.getType() == InventoryItem.Type.NewBlock) {
				//新区块诞生
				executorService.submit(new Runnable() {
					@Override
					public void run() {
						newBlockInventory(inventoryItem, peer);
					}
				});
			} else {
				//如果已经接收过，就跳过
				if(filter.contains(inventoryItem.getHash().getBytes())) {
					continue;
				} else {
					filter.insert(inventoryItem.getHash().getBytes());
					doProcessInventoryItem(inventoryItem);
				}
			}
		}
		
		return null;
	}

	/*
	 * 新区块诞生，下载
	 */
	private void newBlockInventory(InventoryItem inventoryItem, Peer peer) {
		blockLocker.lock();
		
		try {
			if(filter.contains(inventoryItem.getHash().getBytes())) {
				return;
			}
			peer.sendMessage(new GetDatasMessage(peer.getNetwork(), inventoryItem));
			//监听完成
			BlockDownendListener blockDownendListener = new BlockDownendListener() {
				@Override
				public void downend() {
					filter.insert(inventoryItem.getHash().getBytes());
					synchronized (this) {
						if(log.isDebugEnabled()) {
							log.debug("新区快 {} 同步成功", inventoryItem.getHash());
						}
						notify();
					}
				}
			};
			peer.setBlockDownendListener(blockDownendListener);
			
//			try {
//				//等待下载完成，2秒超时，如果超时，则会选择其它节点下载
//				synchronized (blockDownendListener) {
//					blockDownendListener.wait(2000);
//				}
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		} finally {
			blockLocker.unlock();
			peer.setBlockDownendListener(null);
		}
	}

	private void doProcessInventoryItem(InventoryItem inventoryItem) {
		// TODO Auto-generated method stub
		
	}

}
