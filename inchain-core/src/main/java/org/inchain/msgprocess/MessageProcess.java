package org.inchain.msgprocess;

import org.inchain.core.Peer;
import org.inchain.message.Message;

public interface MessageProcess {

	MessageProcessResult process(Message message, Peer peer);
}
