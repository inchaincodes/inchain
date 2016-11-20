package org.inchain.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;

import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NetworkParameters.ProtocolVersion;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Message {
	
	private static final Logger log = LoggerFactory.getLogger(Message.class);

	public static final int MAX_SIZE = 0x02000000; // 32MB
	public static final int HEADER_SIZE = 512+4;
	
	public static final int UNKNOWN_LENGTH = Integer.MIN_VALUE;

	// Useful to ensure serialize/deserialize are consistent with each other.
    private static final boolean SELF_CHECK = false;
    
    protected int length = UNKNOWN_LENGTH;
	// The offset is how many bytes into the provided byte array this message payload starts at.
    protected int offset;
    // The cursor keeps track of where we are in the byte array as we parse it.
    // Note that it's relative to the start of the array NOT the start of the message payload.
    protected int cursor;
    
	protected MessageSerializer serializer;

	// The raw message payload bytes themselves.
    protected byte[] payload;
    protected int protocolVersion;

    protected NetworkParameters network;

    protected Message() {
    	
    }

    protected Message(NetworkParameters network) {
        this.network = network;
        serializer = network.getDefaultSerializer();
    }
    
    protected Message(NetworkParameters network, byte[] payload, int offset) throws ProtocolException {
        this(network, payload, offset, network.getProtocolVersionNum(ProtocolVersion.CURRENT));
    }

    protected Message(NetworkParameters network, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        this(network, payload, offset, protocolVersion, network.getDefaultSerializer(), UNKNOWN_LENGTH);
    }
    
    protected Message(NetworkParameters network, byte[] payload, int offset, int protocolVersion, MessageSerializer serializer, int length) throws ProtocolException {
        this.serializer = serializer;
        this.protocolVersion = protocolVersion;
        this.network = network;
        this.payload = payload;
        this.cursor = this.offset = offset;
        this.length = length;

        parse();

        if (this.length == UNKNOWN_LENGTH) {
            Utils.checkState(false, "Length field has not been set in constructor for %s after parse.",
                       getClass().getSimpleName());
        }
        
        if (SELF_CHECK) {
            selfCheck(payload, offset);
        }
        
        if (!serializer.isParseRetainMode())
            this.payload = null;
    }
    
    protected abstract void parse() throws ProtocolException;
    
    private void selfCheck(byte[] payload, int offset) {
        if (!(this instanceof VersionMessage)) {
            byte[] payloadBytes = new byte[cursor - offset];
            System.arraycopy(payload, offset, payloadBytes, 0, cursor - offset);
            byte[] reserialized = baseSerialize();
            if (!Arrays.equals(reserialized, payloadBytes))
                throw new RuntimeException("Serialization is wrong: \n" +
                		Hex.encode(reserialized) + " vs \n" +
                		Hex.encode(payloadBytes));
        }
    }
    
    /**
     * Returns a copy of the array returned by {@link Message#unsafeBitcoinSerialize()}, which is safe to mutate.
     * If you need extra performance and can guarantee you won't write to the array, you can use the unsafe version.
     *
     * @return a freshly allocated serialized byte array
     */
    public byte[] baseSerialize() {
        byte[] bytes = unsafeBitcoinSerialize();
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        return copy;
    }

    /**
     * Serialize this message to a byte array that conforms to the bitcoin wire protocol.
     * <br/>
     * This method may return the original byte array used to construct this message if the
     * following conditions are met:
     * <ol>
     * <li>1) The message was parsed from a byte array with parseRetain = true</li>
     * <li>2) The message has not been modified</li>
     * <li>3) The array had an offset of 0 and no surplus bytes</li>
     * </ol>
     *
     * If condition 3 is not met then an copy of the relevant portion of the array will be returned.
     * Otherwise a full serialize will occur. For this reason you should only use this API if you can guarantee you
     * will treat the resulting array as read only.
     *
     * @return a byte array owned by this object, do NOT mutate it.
     */
    public byte[] unsafeBitcoinSerialize() {
        // 1st attempt to use a cached array.
        if (payload != null) {
            if (offset == 0 && length == payload.length) {
                // Cached byte array is the entire message with no extras so we can return as is and avoid an array
                // copy.
                return payload;
            }

            byte[] buf = new byte[length];
            System.arraycopy(payload, offset, buf, 0, length);
            return buf;
        }

        // No cached array available so serialize parts by stream.
        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(length < 32 ? 32 : length + 32);
        try {
            baseSerializeToStream(stream);
        } catch (IOException e) {
            // Cannot happen, we are serializing to a memory stream.
        }

        if (serializer.isParseRetainMode()) {
            // A free set of steak knives!
            // If there happens to be a call to this method we gain an opportunity to recache
            // the byte array and in this case it contains no bytes from parent messages.
            // This give a dual benefit.  Releasing references to the larger byte array so that it
            // it is more likely to be GC'd.  And preventing double serializations.  E.g. calculating
            // merkle root calls this method.  It is will frequently happen prior to serializing the block
            // which means another call to bitcoinSerialize is coming.  If we didn't recache then internal
            // serialization would occur a 2nd time and every subsequent time the message is serialized.
            payload = stream.toByteArray();
            cursor = cursor - offset;
            offset = 0;
            length = payload.length;
            return payload;
        }
        // Record length. If this Message wasn't parsed from a byte stream it won't have length field
        // set (except for static length message types).  Setting it makes future streaming more efficient
        // because we can preallocate the ByteArrayOutputStream buffer and avoid resizing.
        byte[] buf = stream.toByteArray();
        length = buf.length;
        return buf;
    }
    
    public final void baseSerializeToStream(OutputStream stream) throws IOException {
        // 1st check for cached bytes.
        if (payload != null && length != UNKNOWN_LENGTH) {
            stream.write(payload, offset, length);
            return;
        }
        serializeToStream(stream);
    }

    /**
     * Serializes this message to the provided stream. If you just want the raw bytes use bitcoinSerialize().
     */
    protected void serializeToStream(OutputStream stream) throws IOException {
        log.error("Error: {} class has not implemented bitcoinSerializeToStream method.  Generating message with no payload", getClass());
    }
    
    /**
     * This returns a correct value by parsing the message.
     */
    public final int getMessageSize() {
        if (length == UNKNOWN_LENGTH)
            Utils.checkState(false, "Length field has not been set in %s.", getClass().getSimpleName());
        return length;
    }
    
    protected long readUint32() throws ProtocolException {
        try {
            long u = Utils.readUint32(payload, cursor);
            cursor += 4;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected long readInt64() throws ProtocolException {
        try {
            long u = Utils.readInt64(payload, cursor);
            cursor += 8;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected BigInteger readUint64() throws ProtocolException {
        // Java does not have an unsigned 64 bit type. So scrape it off the wire then flip.
        return new BigInteger(Utils.reverseBytes(readBytes(8)));
    }
    
    protected long readVarInt() throws ProtocolException {
        return readVarInt(0);
    }

    protected long readVarInt(int offset) throws ProtocolException {
        try {
            VarInt varint = new VarInt(payload, cursor + offset);
            cursor += offset + varint.getOriginalSizeInBytes();
            return varint.value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }
    
    protected byte[] readBytes(int length) throws ProtocolException {
        if (length > MAX_SIZE) {
            throw new ProtocolException("Claimed value length too large: " + length);
        }
        try {
            byte[] b = new byte[length];
            System.arraycopy(payload, cursor, b, 0, length);
            cursor += length;
            return b;
        } catch (IndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }
    
    protected boolean hasMoreBytes() {
        return cursor < payload.length;
    }
    
    protected String readStr() throws ProtocolException {
        long length = readVarInt();
        return length == 0 ? "" : Utils.toString(readBytes((int) length), "UTF-8"); // optimization for empty strings
    }

	public MessageSerializer getSerializer() {
		return serializer;
	}

	public void setSerializer(MessageSerializer serializer) {
		this.serializer = serializer;
	}
	
	public NetworkParameters getNetwork() {
		return network;
	}

	public int getLength() {
		return length;
	}
}
