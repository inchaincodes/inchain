package org.inchain.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.inchain.core.exception.ProtocolException;
import org.inchain.message.Message;
import org.inchain.network.NetworkParams;
import org.inchain.utils.Utils;

/**
 * inchain p2p网络的服务地址
 * @author ln
 *
 */
public class PeerAddress extends Message implements Serializable {

	private static final long serialVersionUID = 4558857847624946548L;

	public static final int MESSAGE_SIZE = 30;

	private static final long DEFAULT_SERVICE = 1l;

    private InetAddress addr;
    private String hostname;
    private int port;
    private long services;
    private long time;

    public PeerAddress(NetworkParams params, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        super(params, payload, offset, protocolVersion);
    }

    public PeerAddress(InetAddress addr, int port, int protocolVersion) {
        this.addr = Utils.checkNotNull(addr);
        this.port = port;
        this.protocolVersion = protocolVersion;
        this.services = DEFAULT_SERVICE;
        length = MESSAGE_SIZE;
    }

    public PeerAddress(InetAddress addr, int port) {
        this(addr, port, NetworkParams.ProtocolVersion.CURRENT.getVersion());
    }

    public PeerAddress(NetworkParams params, InetAddress addr, int port) {
        this(addr, port, params.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT));
    }

    public PeerAddress(NetworkParams params, InetAddress addr) {
        this(params, addr, params.getPort());
    }

    public PeerAddress(InetSocketAddress addr) {
        this(addr.getAddress(), addr.getPort(), NetworkParams.ProtocolVersion.CURRENT.getVersion());
    }

    public PeerAddress(NetworkParams params, InetSocketAddress addr) {
        this(params, addr.getAddress(), addr.getPort());
    }

    public PeerAddress(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.protocolVersion = NetworkParams.ProtocolVersion.CURRENT.getVersion();
        this.services = DEFAULT_SERVICE;
    }

    public PeerAddress(NetworkParams params, String hostname, int port) {
        super(params);
        this.hostname = hostname;
        this.port = port;
        this.protocolVersion = params.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT);
        this.services = DEFAULT_SERVICE;
    }

    public static PeerAddress localhost(NetworkParams params) {
        try {
			return new PeerAddress(params, InetAddress.getByName("127.0.0.1"), params.getPort());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
        return null;
    }

    @Override
    protected void parse() throws ProtocolException {
        // 消息格式:
        //   当前时间戳 8 bytes
        //   服务版本	4 bytes
        //   IP地址      16 bytes
        //   端口		2 bytes
        time = readInt64();
        services = readUint32();
        byte[] addrBytes = readBytes(16);
        try {
            addr = InetAddress.getByAddress(addrBytes);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        port = ((0xFF & payload[cursor++]) << 8) | (0xFF & payload[cursor++]);
        length = MESSAGE_SIZE;
    }

    @Override
	public void serializeToStream(OutputStream stream) throws IOException {
        //当前时间
    	Utils.int64ToByteStreamLE(TimeService.currentTimeMillis(), stream);
        //服务版本
        Utils.uint32ToByteStreamLE(services, stream);
        
        // 手动把ip4转成ip6
        byte[] ipBytes = addr.getAddress();
        if (ipBytes.length == 4) {
            byte[] v6addr = new byte[16];
            System.arraycopy(ipBytes, 0, v6addr, 12, 4);
            v6addr[10] = (byte) 0xFF;
            v6addr[11] = (byte) 0xFF;
            ipBytes = v6addr;
        }
        stream.write(ipBytes);
        // And write out the port. Unlike the rest of the protocol, address and port is in big endian byte order.
        stream.write((byte) (0xFF & port >> 8));
        stream.write((byte) (0xFF & port));
    }
    
    public String getHostname() {
        return hostname;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddr(), getPort());
    }

    public void setAddr(InetAddress addr) {
        this.addr = addr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getServices() {
		return services;
	}

	public void setServices(long services) {
		this.services = services;
	}

	public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        if (hostname != null) {
            return "[" + hostname + "]:" + port;
        }
        return "[" + addr.getHostAddress() + "]:" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerAddress other = (PeerAddress) o;
        return other.addr.equals(addr) && other.port == port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addr, port, time, services);
    }
    
    public InetSocketAddress toSocketAddress() {
        // Reconstruct the InetSocketAddress properly
        if (hostname != null) {
            return InetSocketAddress.createUnresolved(hostname, port);
        } else {
            return new InetSocketAddress(addr, port);
        }
    }
}
