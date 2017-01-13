package org.inchain.net;

import java.net.InetAddress;

public interface StreamConnectionFactory {
    StreamConnection getNewConnection(InetAddress inetAddress, int port);
}
