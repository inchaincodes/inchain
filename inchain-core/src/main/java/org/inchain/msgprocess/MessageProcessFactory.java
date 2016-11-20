package org.inchain.msgprocess;

import org.inchain.message.Message;

/**
 * 消息处理
 * @author ln
 *
 */
public interface MessageProcessFactory {

	MessageProcess getFactory(Message message);
}
