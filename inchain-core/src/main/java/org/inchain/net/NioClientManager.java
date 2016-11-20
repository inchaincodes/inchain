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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.core.Peer;
import org.inchain.core.PeerAddress;
import org.inchain.network.NetworkParameters;
import org.inchain.network.Seed;
import org.inchain.utils.ContextPropagatingThreadFactory;
import org.inchain.utils.Utils;
import org.slf4j.LoggerFactory;

/**
 * A class which manages a set of client connections. Uses Java NIO to select network events and processes them in a
 * single network processing thread.
 */
public class NioClientManager implements ClientConnectionManager {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(NioClientManager.class);

    private final NetworkParameters network;
    
    private final Selector selector;
    
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    
    //被动连接监听
    private NewInConnectionListener newInConnectionListener;
    
    private boolean isServer = false; //是否启动本地监听服务 ， SPV就不需要
    private ServerSocket serverSocket;
    
    public NioClientManager(NetworkParameters network, boolean isServer, int port) {
    	try {
    		this.network = Utils.checkNotNull(network);
    		this.isServer = isServer;
    		
            selector = SelectorProvider.provider().openSelector();
            if(this.isServer) {
	            // 打开服务器套接字通道  
	            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();  
	            // 服务器配置为非阻塞  
	            serverSocketChannel.configureBlocking(false);  
	            // 检索与此通道关联的服务器套接字  
	            serverSocket = serverSocketChannel.socket();  
	            // 进行服务的绑定  
	            serverSocket.bind(new InetSocketAddress(port));  
	            // 注册到selector，等待连接  
	            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);  
	            log.info("Server Start on port {}:", port);
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // Shouldn't ever happen
        }
	}

    
    class PendingConnect {
        SocketChannel sc;
        StreamConnection connection;
        Seed seed;
        Future<Seed> future = new CompletableFuture<Seed>();

        PendingConnect(SocketChannel sc, StreamConnection connection, Seed seed) { this.sc = sc; this.connection = connection; this.seed = seed; }
    }
    final Queue<PendingConnect> newConnectionChannels = new LinkedBlockingQueue<PendingConnect>();

    // Added to/removed from by the individual ConnectionHandler's, thus must by synchronized on its own.
    private final Set<ConnectionHandler> connectedHandlers = Collections.synchronizedSet(new HashSet<ConnectionHandler>());

    // Handle a SelectionKey which was selected
    private void handleKey(SelectionKey key) throws IOException {
        // We could have a !isValid() key here if the connection is already closed at this point
        if (key.isValid() && key.isConnectable()) { // ie a client connection which has finished the initial connect process
            // Create a ConnectionHandler and hook everything together
            PendingConnect data = (PendingConnect) key.attachment();
            StreamConnection connection = data.connection;
            SocketChannel sc = (SocketChannel) key.channel();
            ConnectionHandler handler = new ConnectionHandler(connection, key, connectedHandlers);
            try {
                if (sc.finishConnect()) {
                    log.info("Connected to {}", sc.socket().getRemoteSocketAddress());
                    key.interestOps((key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT).attach(handler);
                    connection.connectionOpened();
                    data.seed.setStaus(Seed.SEED_CONNECT_SUCCESS);
                } else {
                    log.warn("Failed to connect to {}", sc.socket().getRemoteSocketAddress());
                    handler.closeConnection(); // Failed to connect for some reason
                    data.seed.setStaus(Seed.SEED_CONNECT_FAIL);
                }
            } catch (Exception e) {
                // If e is a CancelledKeyException, there is a race to get to interestOps after finishConnect() which
                // may cause this. Otherwise it may be any arbitrary kind of connection failure.
                // Calling sc.socket().getRemoteSocketAddress() here throws an exception, so we can only log the error itself
                log.warn("Failed to connect with exception: {}: {}", e.getMessage(), e.fillInStackTrace());
                handler.closeConnection();
                data.seed.setStaus(Seed.SEED_CONNECT_FAIL);
            }
        } else {
        	ConnectionHandler handler = ((ConnectionHandler)key.attachment());
            if (handler == null) {
            	if(key.isValid() && key.isAcceptable()) {
	            	ServerSocketChannel sc = (ServerSocketChannel) key.channel();
	            	SocketChannel socketChannel = sc.accept();
	            	
	            	if(newInConnectionListener == null || !newInConnectionListener.allowConnection()) {
	            		log.info("refush connection on " + socketChannel.getRemoteAddress());
	            		socketChannel.close();
	            		return;
	            	}
	            	
                    // 配置为非阻塞  
            		socketChannel.configureBlocking(false);
            		SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ);
//            		key.cancel();
            		
            		Peer peer = new Peer(network, new PeerAddress((InetSocketAddress)socketChannel.getRemoteAddress())) {
            			@Override
            			public void connectionOpened() {}
                		@Override
                		public void connectionClosed() {
                			if(newInConnectionListener != null) 
                				newInConnectionListener.connectionClosed(this);
                		}
                	};
            		handler = new ConnectionHandler(peer, newKey, socketChannel, connectedHandlers);
            		newKey.attach(handler);
		      		peer.connectionOpened();
            		
            		if(newInConnectionListener != null) 
        				newInConnectionListener.connectionOpened(peer);
                    
                    return;
                }
            }
            
        	// Process bytes read
        	ConnectionHandler.handleKey(key);
        }
    }

	@Override
    public void start() {
    	executor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void stop() throws IOException {
    	executor.shutdownNow();
        serverSocket.close();
        log.info("stoped service");
    }

    public void run() {
        try {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            while (!executor.isShutdown()) {
                PendingConnect conn;
                while ((conn = newConnectionChannels.poll()) != null) {
                    try {
                        SelectionKey key = conn.sc.register(selector, SelectionKey.OP_CONNECT);
                        key.attach(conn);
                    } catch (ClosedChannelException e) {
                        log.warn("SocketChannel was closed before it could be registered");
                    }
                }

                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    handleKey(key);
                }
            }
        } catch (Exception e) {
            log.warn("Error trying to open/read from connection: ", e);
        } finally {
            // Go through and close everything, without letting IOExceptions get in our way
            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException e) {
                    log.warn("Error closing channel", e);
                }
                key.cancel();
                if (key.attachment() instanceof ConnectionHandler)
                    ConnectionHandler.handleKey(key); // Close connection if relevant
            }
            try {
                selector.close();
            } catch (IOException e) {
                log.warn("Error closing client manager selector", e);
            }
        }
    }

	@Override
    public Future<Seed> openConnection(Seed seed, StreamConnection connection) {
        if (executor.isShutdown())
            throw new IllegalStateException();
        // seed and address not null
        Utils.checkNotNull(seed);
        Utils.checkNotNull(seed.getAddress());
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(seed.getAddress());
            PendingConnect data = new PendingConnect(sc, connection, seed);
            newConnectionChannels.offer(data);
            selector.wakeup();
            return data.future;
        } catch (Throwable e) {
            return null;
        }
    }

    public void triggerShutdown() {
        selector.wakeup();
    }

    @Override
    public int getConnectedClientCount() {
        return connectedHandlers.size();
    }

    @Override
    public void closeConnections(int n) {
        while (n-- > 0) {
            ConnectionHandler handler;
            synchronized (connectedHandlers) {
                handler = connectedHandlers.iterator().next();
            }
            if (handler != null)
                handler.closeConnection(); // Removes handler from connectedHandlers before returning
        }
    }
    
    public void setNewInConnectionListener(NewInConnectionListener newInConnectionListener) {
		this.newInConnectionListener = newInConnectionListener;
	}

    protected Executor executor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                new ContextPropagatingThreadFactory("NioClientManager").newThread(command).start();
            }
        };
    }
}
