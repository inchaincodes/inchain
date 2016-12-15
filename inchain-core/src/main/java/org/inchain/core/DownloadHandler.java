package org.inchain.core;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.message.GetBlockMessage;
import org.inchain.network.NetworkParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 区块同步器
 * @author ln
 *
 */
@Service
public class DownloadHandler {
	
	//下载锁，避免多节点重复下载
	private Lock locker = new ReentrantLock();

	@Autowired
	private NetworkParams network;
	
	/**
	 * 新节点连接，如果高度比当前节点高，那么下载区块数据
	 * @param peer
	 */
	public void newPeer(final Peer peer) {
		long localHeight = network.getBestBlockHeight();
		
		if(peer.getPeerVersionMessage().bestHeight > localHeight) {
			//启动下载
			new Thread() {
				public void run() {
					startDownload(peer);
				};
			}.start();
		}
	}

	/*
	 * 下载区块
	 */
	private void startDownload(Peer peer) {
		locker.lock();
		try {
			//远程节点的高度
			long remoteBestHeight = peer.getPeerVersionMessage().bestHeight;
			long localBestHeight = network.getBestBlockHeight();
			
			while(remoteBestHeight > localBestHeight) {
				long diffHeight = remoteBestHeight - localBestHeight;
				long count = diffHeight >= 100 ? 100: diffHeight;
				
				peer.sendMessage(new GetBlockMessage(network, localBestHeight, count));
				
				localBestHeight += count;
				
			}
		} finally {
			locker.unlock();
		}
	}
}
