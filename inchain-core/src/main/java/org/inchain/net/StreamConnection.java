package org.inchain.net;

import java.nio.ByteBuffer;

public interface StreamConnection {

	void connectionClosed();

    void connectionOpened();

    int receiveBytes(ByteBuffer buff) throws Exception;

    void setWriteTarget(MessageWriteTarget writeTarget);

    int getMaxMessageSize();
}
