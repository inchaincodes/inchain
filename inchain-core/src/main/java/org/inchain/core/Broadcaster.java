package org.inchain.core;

import org.inchain.message.Message;

/**
 * 消息广播器
 * @author ln
 *
 * @param <T>
 */
public interface Broadcaster<T extends Message> {
	
	/**
	 * 广播消息，需要等待接收响应
	 * @param message  			要广播的消息
	 * @return BroadcastResult 	广播结果
	 */
	BroadcastResult broadcast(T message);
	
	/**
	 * 广播消息，无需等待接收回应
	 * @param message  			要广播的消息
	 * @return int 				通过几个节点广播消息出去
	 */
	int broadcastMessage(T message);
	
	/**
	 * 广播消息
	 * @param message  			要广播的消息
	 * @param excludePeer  		要排除的节点
	 * @return int	 			通过几个节点广播消息出去
	 */
	int broadcastMessage(T message, Peer excludePeer);

	/**
	 * 广播消息
	 * @param message  			要广播的消息
	 * @param count  		    广播节点数量
	 * @return int	 			广播到超级节点
	 */
	int broadcastMessageToSuper(T message, int count);
}
