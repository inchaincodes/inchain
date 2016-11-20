/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2015 Ross Nicoll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.transaction.Transaction;
import org.inchain.utils.Utils;

public abstract class MessageSerializer {
	
	protected static final int COMMAND_LEN = 12;

	/**
	 * 接收到消息流，解码为消息
	 * @param in
	 * @return
	 * @throws ProtocolException
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
    public abstract Message deserialize(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException;

    /**
     * 接收到消息流，解析消息头
     * @param in
     * @return
     * @throws ProtocolException
     * @throws IOException
     * @throws UnsupportedOperationException
     */
    public abstract MessagePacketHeader deserializeHeader(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException;

    /**
     * 通过消息头，解析消息主体
     * @param header
     * @param in
     * @return
     * @throws ProtocolException
     * @throws BufferUnderflowException
     * @throws UnsupportedOperationException
     */
    public abstract Message deserializePayload(MessagePacketHeader header, ByteBuffer in) throws ProtocolException, BufferUnderflowException, UnsupportedOperationException;

    public abstract void seekPastMagicBytes(ByteBuffer in) throws BufferUnderflowException;

    public abstract void serialize(String name, byte[] message, OutputStream out) throws IOException, UnsupportedOperationException;

    public abstract void serialize(Message message, OutputStream out) throws IOException, UnsupportedOperationException;
    /**
     * Whether the serializer will produce cached mode Messages
     */
    public abstract boolean isParseRetainMode();
    
    public static class MessagePacketHeader {
        /** The largest number of bytes that a header can represent */
        public static final int HEADER_LENGTH = COMMAND_LEN + 4 + 4;

        public final byte[] header;
        public final String command;
        public final int size;
        public final byte[] checksum;

        public MessagePacketHeader(ByteBuffer in) throws ProtocolException, BufferUnderflowException {
            header = new byte[HEADER_LENGTH];
            in.get(header, 0, header.length);

            int cursor = 0;

            // The command is a NULL terminated string, unless the command fills all twelve bytes
            // in which case the termination is implicit.
            for (; header[cursor] != 0 && cursor < COMMAND_LEN; cursor++) ;
            byte[] commandBytes = new byte[cursor];
            System.arraycopy(header, 0, commandBytes, 0, cursor);
            command = Utils.toString(commandBytes, "US-ASCII");
            cursor = COMMAND_LEN;

            size = (int) Utils.readUint32(header, cursor);
            cursor += 4;

            if (size > Message.MAX_SIZE || size < 0)
                throw new ProtocolException("Message size too large: " + size);

            // Old clients don't send the checksum.
            checksum = new byte[4];
            // Note that the size read above includes the checksum bytes.
            System.arraycopy(header, cursor, checksum, 0, 4);
            cursor += 4;
        }
    }
    
    /**
     * 解析交易
     * @param payloadBytes
     * @return
     * @throws ProtocolException
     * @throws UnsupportedOperationException
     */
    public abstract Transaction makeTransaction(byte[] payloadBytes) throws ProtocolException, UnsupportedOperationException;
    
    /**
     * 解析交易
     * @param payloadBytes
     * @param hash
     * @return
     * @throws ProtocolException
     * @throws UnsupportedOperationException
     */
    public abstract Transaction makeTransaction(byte[] payloadBytes, 
    		Sha256Hash hash) throws ProtocolException, UnsupportedOperationException;
    
    /**
     * 解析交易
     * @param payloadBytes
     * @param offset
     * @return
     * @throws ProtocolException
     */
    public abstract Transaction makeTransaction(byte[] payloadBytes, int offset) throws ProtocolException;
    
}
