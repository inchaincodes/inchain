package org.inchain.core;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.inchain.kits.PeerKit;
import org.inchain.listener.EnoughAvailablePeersListener;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.message.NewBlockMessage;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息广播器实现
 * @author ln
 *
 * @param <T>
 */
@Component
public class BroadcasterComponent<T extends Message> implements Broadcaster<T> {
	
    private static final Logger log = LoggerFactory.getLogger(BroadcasterComponent.class);
    
    private static Random random = new Random();
    
    @Autowired
    private PeerKit peerKit;
    @Autowired
    private NetworkParams network;
    
	@Override
	public BroadcastResult broadcast(final T message) {
		//广播结果
		final BroadcastResult result = new BroadcastResult();
		
		int minConnections = peerKit.getBroadcasterMinConnectionCount();
		
		if(log.isDebugEnabled()) {
			log.debug("minConnections is {}, wait ···", minConnections);
		}
		
		peerKit.waitForPeers(minConnections, new EnoughAvailablePeersListener() {
			@Override
			public void callback(List<Peer> peers) {
				new EnoughAvailablePeers(message, result).run();
			}
		});
		
		return result;
	}
	
	private class EnoughAvailablePeers {
		
		private final BroadcastResult result;
		private final Message message;

		public EnoughAvailablePeers(Message message, BroadcastResult result) {
			this.message = message;
			this.result = result;
		}

		public void run() {
			List<Peer> peers = peerKit.findAvailablePeers();
			
			Message sendMessage = converMessage(message, result);
			
			//通过一半的对等体广播出去，如果收到一半以上的inv消息，则代表成功
			int numConnected = peers.size();
            int numToBroadcastTo = (int) Math.max(1, Math.round(Math.ceil(peers.size() / 2.0)));
            int numWaitingFor = (int) Math.max(1, Math.ceil((numConnected - numToBroadcastTo) / 2.0));
            Collections.shuffle(peers, random);
            peers = peers.subList(0, numToBroadcastTo);
			
            log.info("broadcast: We have {} peers, adding {} to the memory pool", numConnected, sendMessage);
            log.info("Sending to {} peers, will wait for {}", numToBroadcastTo, numWaitingFor);
            if(log.isDebugEnabled()) {
            	log.debug("send to {}", peers);
            }
            
            //如果需要监听响应
            if(result.needWait()) {
            	BroadcastContext.get().add(result.getHash(), result);
				result.setBroadcastPeers(peers);
				result.setNumWaitingFor(numWaitingFor);
			}
            
            //通过随机选择的对等体进行广播
			for (Peer peer : peers) {
				peer.sendMessage(sendMessage);
			}
			
			//不需要等待的消息类型，直接响应
			if(!result.needWait()) {
				result.getFuture().set(result);
			}
		}
	}

	/**
	 * 消息转换，NewBlockMessage，transaction消息转换为inv消息
	 * @param message
	 * @param result 
	 * @return Message
	 */
	public Message converMessage(Message message, BroadcastResult result) {
		Message sendMessage = null;
		if(message instanceof NewBlockMessage) {
			//TODO
		} else if(message instanceof Transaction) {
			//交易
			Transaction tx = (Transaction) message;
			result.setHash(tx.getHash());
			sendMessage = new InventoryMessage(network, new InventoryItem(InventoryItem.Type.Transaction, tx.getHash()));
		}
		
		if(sendMessage == null) {
			sendMessage = message;
		}
		return sendMessage;
	}

	@Override
	public int broadcastMessage(T message) {
		return broadcastMessage(message, null);
	}

	@Override
	public int broadcastMessage(T message, Peer excludePeer) {
		int successCount = 0;
		if(peerKit.canBroadcast()) {
			for (Peer peer : peerKit.findAvailablePeers()) {
				if(excludePeer == null || (excludePeer!= null && !peer.equals(excludePeer))) {
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
    
}
