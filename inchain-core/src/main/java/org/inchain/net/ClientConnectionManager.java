/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.inchain.net;

import java.io.IOException;
import java.util.concurrent.Future;

import org.inchain.network.Seed;

/**
 * <p>A generic interface for an object which keeps track of a set of open client connections, creates new ones and
 * ensures they are serviced properly.</p>
 *
 * the appropriate connectionClosed() calls must be made.</p>
 */
public interface ClientConnectionManager extends Runnable {
    /**
     * Creates a new connection to the given address, with the given connection used to handle incoming data. Any errors
     * that occur during connection will be returned in the given future, including errors that can occur immediately.
     */
    Future<Seed> openConnection(Seed seed, StreamConnection connection);

    /** Gets the number of connected peers */
    int getConnectedClientCount();

    /** Closes n peer connections */
    void closeConnections(int n);

    void setNewInConnectionListener(NewInConnectionListener newInConnectionListener);
    
    void start();
    
    void stop() throws IOException;
}
