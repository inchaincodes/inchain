package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.inchain.core.PeerAddress;
import org.inchain.core.TimeService;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;


public class VersionMessage extends Message {

	 /**
     * Inchain 核心程序版本
     */
    public static final String INCHAIN_VERSION = "0.1";

    /**
     * 版本完整信息
     */
    public static final String LIBRARY_SUBVER = "/inchain core v" + INCHAIN_VERSION + "/";
    
    /**
     * 哪个网络服务
     */
    public int localServices;
    
    /**
     * 协议版本
     */
    public int clientVersion;
    
    /**
     * 对等体的时间
     */
    public long time;
   
    /**
     * 我的网络地址
     */
    public PeerAddress myAddr;
    
    /**
     * 对等体的网络时间
     */
    public PeerAddress theirAddr;
    
    /**
     * 版本信息
     */
    public String subVer;
    
    /**
     * 对等体的区块数量
     */
    public long bestHeight;
    
    /**
     * 对等体最新区块的hash
     */
    public Sha256Hash bestBlockHash;
    
    /**
     * 随机数
     */
    private long nonce;
    
	public VersionMessage(NetworkParams params) throws ProtocolException {
        super(params);
    }
	
	public VersionMessage(NetworkParams params, byte[] payload) throws ProtocolException {
        super(params, payload, 0);
    }
	
	public VersionMessage(NetworkParams params, long bestHeight, Sha256Hash bestBlockHash, PeerAddress remoteAddress) {
	    super(params);
        clientVersion = params.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT);
        localServices = params.getLocalServices();
        time = TimeService.currentTimeMillis();
        try {
            final byte[] localhost = { 127, 0, 0, 1 };
            myAddr = new PeerAddress(InetAddress.getByAddress(localhost), params.getPort(), 0);
            if(remoteAddress == null) {
            	theirAddr = new PeerAddress(InetAddress.getByAddress(localhost), params.getPort(), 0);
            } else {
            	theirAddr = remoteAddress;
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        subVer = LIBRARY_SUBVER;
        this.bestHeight = bestHeight;
        this.bestBlockHash = bestBlockHash;
	}
	
	@Override
	protected void parse() throws ProtocolException {
		localServices = readBytes(1)[0] & 0xFF;
		clientVersion = (int) readUint32();
        time = readInt64();
        
        myAddr = new PeerAddress(network, payload, cursor, 0);
        cursor += myAddr.getMessageSize();
        theirAddr = new PeerAddress(network, payload, cursor, 0);
        cursor += theirAddr.getMessageSize();
        
        subVer = readStr();
        bestHeight = readUint32();
        bestBlockHash = readHash();
        nonce = readInt64();
        length = cursor - offset;
	}
	
	@Override
    public void serializeToStream(OutputStream buf) throws IOException {
		buf.write(localServices);
        Utils.uint32ToByteStreamLE(clientVersion, buf);
        Utils.int64ToByteStreamLE(time, buf);
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
        // Now comes subVer.
        byte[] subVerBytes = subVer.getBytes("UTF-8");
        buf.write(new VarInt(subVerBytes.length).encode());
        buf.write(subVerBytes);
        // Size of known block chain.
        Utils.uint32ToByteStreamLE(bestHeight, buf);
        buf.write(bestBlockHash.getReversedBytes());
        Utils.int64ToByteStreamLE(nonce, buf);
    }
	
	@Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("local services: ").append(localServices).append("\n");
        stringBuilder.append("client version: ").append(clientVersion).append("\n");
        stringBuilder.append("time:           ").append(time).append("\n");
        stringBuilder.append("my addr:        ").append(myAddr).append("\n");
        stringBuilder.append("their addr:     ").append(theirAddr).append("\n");
        stringBuilder.append("sub version:    ").append(subVer).append("\n");
        stringBuilder.append("best block height:    ").append(bestHeight).append("\n");
        stringBuilder.append("best block hash:    ").append(bestBlockHash).append("\n");
        stringBuilder.append("nonce:    ").append(nonce).append("\n");
        return stringBuilder.toString();
    }
}
