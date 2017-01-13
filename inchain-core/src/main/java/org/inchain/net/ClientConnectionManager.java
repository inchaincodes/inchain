package org.inchain.net;

import java.io.IOException;
import java.util.concurrent.Future;

import org.inchain.network.Seed;

public interface ClientConnectionManager {
    Future<Seed> openConnection(Seed seed, StreamConnection connection);

    int getConnectedClientCount();

    void closeConnections(int n);

    void setNewInConnectionListener(NewInConnectionListener newInConnectionListener);
    
    void start();
    
    void stop() throws IOException;
}
