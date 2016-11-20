package org.inchain.core;

import static org.inchain.utils.Utils.checkNotNull;
import static org.inchain.utils.Utils.checkState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.message.Message;
import org.inchain.message.MessageSerializer;
import org.inchain.message.MessageSerializer.MessagePacketHeader;
import org.inchain.net.AbstractTimeoutHandler;
import org.inchain.net.MessageWriteTarget;
import org.inchain.net.StreamConnection;
import org.inchain.network.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PeerSocketHandler extends AbstractTimeoutHandler implements StreamConnection {

	private static final Logger log = LoggerFactory.getLogger(PeerSocketHandler.class);
	
	private byte[] largeReadBuffer;
    private int largeReadBufferPos;
    
    
    
    private MessageSerializer.MessagePacketHeader header;
	private final MessageSerializer serializer;
    protected PeerAddress myAddress;
    protected PeerAddress peerAddress;
    
    private boolean closePending = false;
 	protected MessageWriteTarget writeTarget = null;
 	
 	private Lock lock = new ReentrantLock();

 	public PeerSocketHandler(NetworkParameters params, InetSocketAddress remoteIp) {
        checkNotNull(params);
        serializer = params.getDefaultSerializer();
        this.peerAddress = new PeerAddress(params, remoteIp);
    }

    public PeerSocketHandler(NetworkParameters params, PeerAddress peerAddress) {
        checkNotNull(params);
        serializer = params.getDefaultSerializer();
        this.peerAddress = checkNotNull(peerAddress);
    }
    
	@Override
    protected void timeoutOccurred() {
        log.info("{}: Timed out", peerAddress.getSocketAddress());
        close();
    }

    public void sendMessage(Message message) throws NotYetConnectedException {
        lock.lock();
        try {
            if (writeTarget == null)
                throw new NotYetConnectedException();
        } finally {
            lock.unlock();
        }
        if(message.getSerializer() == null) {
        	message.setSerializer(serializer);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            serializer.serialize(message, out);
            writeTarget.writeBytes(out.toByteArray());
        } catch (IOException e) {
        	close();
        	log.info(" - " + e.getMessage());
        }
    }

    /**
     * Called every time a message is received from the network
     */
    protected abstract void processMessage(Message m) throws Exception;

    @Override
    public int receiveBytes(ByteBuffer buff) {
        checkState(buff.position() == 0 && buff.capacity() >= MessagePacketHeader.HEADER_LENGTH);
        try {
            // Repeatedly try to deserialize messages until we hit a BufferUnderflowException
            boolean firstMessage = true;
            while (true) {
                // If we are in the middle of reading a message, try to fill that one first, before we expect another
                if (largeReadBuffer != null) {
                    // This can only happen in the first iteration
                    checkState(firstMessage);
                    // Read new bytes into the largeReadBuffer
                    int bytesToGet = Math.min(buff.remaining(), largeReadBuffer.length - largeReadBufferPos);
                    buff.get(largeReadBuffer, largeReadBufferPos, bytesToGet);
                    largeReadBufferPos += bytesToGet;
                    // Check the largeReadBuffer's status
                    if (largeReadBufferPos == largeReadBuffer.length) {
                        // ...processing a message if one is available
                        processMessage(serializer.deserializePayload(header, ByteBuffer.wrap(largeReadBuffer)));
                        largeReadBuffer = null;
                        header = null;
                        firstMessage = false;
                    } else // ...or just returning if we don't have enough bytes yet
                        return buff.position();
                }
                // Now try to deserialize any messages left in buff
                Message message;
                int preSerializePosition = buff.position();
                try {
                    message = serializer.deserialize(buff);
                } catch (BufferUnderflowException e) {
                    // If we went through the whole buffer without a full message, we need to use the largeReadBuffer
                    if (firstMessage && buff.limit() == buff.capacity()) {
                        // ...so reposition the buffer to 0 and read the next message header
                        buff.position(0);
                        try {
                            serializer.seekPastMagicBytes(buff);
                            header = serializer.deserializeHeader(buff);
                            // Initialize the largeReadBuffer with the next message's size and fill it with any bytes
                            // left in buff
                            largeReadBuffer = new byte[header.size];
                            largeReadBufferPos = buff.remaining();
                            buff.get(largeReadBuffer, 0, largeReadBufferPos);
                        } catch (BufferUnderflowException e1) {
                            // If we went through a whole buffer's worth of bytes without getting a header, give up
                            // In cases where the buff is just really small, we could create a second largeReadBuffer
                            // that we use to deserialize the magic+header, but that is rather complicated when the buff
                            // should probably be at least that big anyway (for efficiency)
                            throw new ProtocolException("No magic bytes+header after reading " + buff.capacity() + " bytes");
                        }
                    } else {
                        // Reposition the buffer to its original position, which saves us from skipping messages by
                        // seeking past part of the magic bytes before all of them are in the buffer
                        buff.position(preSerializePosition);
                    }
                    return buff.position();
                }
                // Process our freshly deserialized message
                processMessage(message);
                firstMessage = false;
            }
        } catch (Exception e) {
            exceptionCaught(e);
            return -1;
        }
    }
    
	@Override
    public void setWriteTarget(MessageWriteTarget writeTarget) {
		checkNotNull(writeTarget);
        lock.lock();
        boolean closeNow = false;
        try {
        	checkState(this.writeTarget == null);
            closeNow = closePending;
            this.writeTarget = writeTarget;
        } finally {
            lock.unlock();
        }
        if (closeNow)
            writeTarget.closeConnection();
    }
	
	
	/**
     * Closes the connection to the peer if one exists, or immediately closes the connection as soon as it opens
     */
    public void close() {
        lock.lock();
        try {
            if (writeTarget == null) {
                closePending = true;
                return;
            }
        } finally {
            lock.unlock();
        }
        writeTarget.closeConnection();
    }
    
    private void exceptionCaught(Exception e) {
        PeerAddress addr = getAddress();
        String s = addr == null ? "?" : addr.toString();
        if (e instanceof ConnectException || e instanceof IOException) {
            // Short message for network errors
            log.info(s + " - " + e.getMessage());
        } else {
            log.warn(s + " - ", e);
            Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
            if (handler != null)
                handler.uncaughtException(Thread.currentThread(), e);
        }

        close();
    }
    
    public PeerAddress getAddress() {
        return peerAddress;
    }
}

