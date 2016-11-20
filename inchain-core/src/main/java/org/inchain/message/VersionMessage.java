package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.inchain.core.PeerAddress;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParameters;
import org.inchain.utils.Utils;


public class VersionMessage extends Message {

	/** The version of this library release, as a string. */
    public static final String BITCOINJ_VERSION = "0.1";
    /** The value that is prepended to the subVer field of this application. */
    public static final String LIBRARY_SUBVER = "/truechain:" + BITCOINJ_VERSION + "/";

    /** A services flag that denotes whether the peer has a copy of the block chain or not. */
    public static final int NODE_NETWORK = 1;
    /** A flag that denotes whether the peer supports the getutxos message or not. */
    public static final int NODE_GETUTXOS = 2;

    /**
     * The version number of the protocol spoken.
     */
    public int clientVersion;
    /**
     * Flags defining what optional services are supported.
     */
    public long localServices;
    /**
     * What the other side believes the current time to be, in seconds.
     */
    public long time;
    /**
     * What the other side believes the address of this program is. Not used.
     */
    public PeerAddress myAddr;
    /**
     * What the other side believes their own address is. Not used.
     */
    public PeerAddress theirAddr;
    /**
     * User-Agent as defined in <a href="https://github.com/bitcoin/bips/blob/master/bip-0014.mediawiki">BIP 14</a>.
     * Bitcoin Core sets it to something like "/Satoshi:0.9.1/".
     */
    public String subVer;
    /**
     * How many blocks are in the chain, according to the other side.
     */
    public long bestHeight;
    /**
     * Whether or not to relay tx invs before a filter is received.
     * See <a href="https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#extensions-to-existing-messages">BIP 37</a>.
     */
    public boolean relayTxesBeforeFilter;
    
	public VersionMessage(NetworkParameters params) throws ProtocolException {
        super(params);
    }
	
	public VersionMessage(NetworkParameters params, byte[] payload) throws ProtocolException {
        super(params, payload, 0);
    }
	
	public VersionMessage(NetworkParameters params, int newBestHeight, PeerAddress remoteAddress) {
	    super(params);
        clientVersion = params.getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT);
        localServices = NODE_NETWORK;
        time = System.currentTimeMillis() / 1000;
        // Note that the Bitcoin Core doesn't do anything with these, and finding out your own external IP address
        // is kind of tricky anyway, so we just put nonsense here for now.
        try {
            // We hard-code the IPv4 localhost address here rather than use InetAddress.getLocalHost() because some
            // mobile phones have broken localhost DNS entries, also, this is faster.
            final byte[] localhost = { 127, 0, 0, 1 };
            myAddr = new PeerAddress(InetAddress.getByAddress(localhost), params.getPort(), 0);
            if(remoteAddress == null) {
            	theirAddr = new PeerAddress(InetAddress.getByAddress(localhost), params.getPort(), 0);
            } else {
            	theirAddr = remoteAddress;
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);  // Cannot happen (illegal IP length).
        }
        subVer = LIBRARY_SUBVER;
        bestHeight = newBestHeight;
        relayTxesBeforeFilter = true;

        length = 85;
        if (protocolVersion > 31402)
            length += 8;
        length += VarInt.sizeOf(subVer.length()) + subVer.length();
	}
	
	@Override
	protected void parse() throws ProtocolException {
		clientVersion = (int) readUint32();
        localServices = readUint64().longValue();
        time = readUint64().longValue();
        myAddr = new PeerAddress(network, payload, cursor, 0);
        cursor += myAddr.getMessageSize();
        theirAddr = new PeerAddress(network, payload, cursor, 0);
        cursor += theirAddr.getMessageSize();
        // uint64 localHostNonce  (random data)
        // We don't care about the localhost nonce. It's used to detect connecting back to yourself in cases where
        // there are NATs and proxies in the way. However we don't listen for inbound connections so it's irrelevant.
        readUint64();
        try {
            // Initialize default values for flags which may not be sent by old nodes
            subVer = "";
            bestHeight = 0;
            relayTxesBeforeFilter = true;
            if (!hasMoreBytes())
                return;
            //   string subVer  (currently "")
            subVer = readStr();
            if (!hasMoreBytes())
                return;
            //   int bestHeight (size of known block chain).
            bestHeight = readUint32();
            if (!hasMoreBytes())
                return;
            relayTxesBeforeFilter = readBytes(1)[0] != 0;
        } finally {
            length = cursor - offset;
        }
	}
	
	@Override
    public void serializeToStream(OutputStream buf) throws IOException {
        Utils.uint32ToByteStreamLE(clientVersion, buf);
        Utils.uint32ToByteStreamLE(localServices, buf);
        Utils.uint32ToByteStreamLE(localServices >> 32, buf);
        Utils.uint32ToByteStreamLE(time, buf);
        Utils.uint32ToByteStreamLE(time >> 32, buf);
        try {
            // My address.
            myAddr.serializeToStream(buf);
            // Their address.
            theirAddr.serializeToStream(buf);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);  // Can't happen.
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
        // Next up is the "local host nonce", this is to detect the case of connecting
        // back to yourself. We don't care about this as we won't be accepting inbound 
        // connections.
        Utils.uint32ToByteStreamLE(0, buf);
        Utils.uint32ToByteStreamLE(0, buf);
        // Now comes subVer.
        byte[] subVerBytes = subVer.getBytes("UTF-8");
        buf.write(new VarInt(subVerBytes.length).encode());
        buf.write(subVerBytes);
        // Size of known block chain.
        Utils.uint32ToByteStreamLE(bestHeight, buf);
        buf.write(relayTxesBeforeFilter ? 1 : 0);
    }
	
	@Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("client version: ").append(clientVersion).append("\n");
        stringBuilder.append("local services: ").append(localServices).append("\n");
        stringBuilder.append("time:           ").append(time).append("\n");
        stringBuilder.append("my addr:        ").append(myAddr).append("\n");
        stringBuilder.append("their addr:     ").append(theirAddr).append("\n");
        stringBuilder.append("sub version:    ").append(subVer).append("\n");
        stringBuilder.append("best height:    ").append(bestHeight).append("\n");
        stringBuilder.append("delay tx relay: ").append(!relayTxesBeforeFilter).append("\n");
        return stringBuilder.toString();
    }
}
