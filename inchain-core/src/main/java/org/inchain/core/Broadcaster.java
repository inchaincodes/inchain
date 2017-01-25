package org.inchain.core;

import org.inchain.message.Message;
import org.inchain.transaction.Transaction;

public interface Broadcaster {
	/**
	 * 广播消息
	 * @param message  要广播的消息
	 * @return int 成功广播给几个节点
	 */
	int broadcastMessage(Message message);
	
	/**
	 * 广播消息
	 * @param message  要广播的消息
	 * @param excludePeer  要排除的节点
	 * @return int 成功广播给几个节点
	 */
	int broadcastMessage(Message message, Peer excludePeer);
	
    TransactionBroadcast broadcastTransaction(final Transaction tx);
}
