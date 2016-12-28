package org.inchain.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Peer;
import org.inchain.core.TimeHelper;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.network.NetworkParams.ProtocolVersion;
import org.inchain.utils.Hex;
import org.inchain.utils.RandomUtil;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共识交互消息
 * @author ln
 *
 */
public class ConsensusMessage extends Message {

	private static final Logger log = LoggerFactory.getLogger(ConsensusMessage.class);
	
	private static final int PROTOCOL_VERSION = 1;
	
	private Peer peer;
	
	//消息ID
	private Sha256Hash id;
	//消息发送人
	private byte[] hash160;
	//消息内容
	private byte[] content;
	//高度
	private long height;
	//消息发送时间
	private long time;
	//随机数
	private long nonce;
	//签名
	private byte[] sign;
	
	public ConsensusMessage(NetworkParams network) {
        super();
    }
    
	public ConsensusMessage(NetworkParams network, byte[] payload) throws ProtocolException {
        this(network, payload, 0);
    }
    
	public ConsensusMessage(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
        this(network, payload, offset, network.getProtocolVersionNum(ProtocolVersion.CURRENT));
    }

    public ConsensusMessage(NetworkParams network, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        super(network, payload, offset, protocolVersion, network.getDefaultSerializer(), UNKNOWN_LENGTH);
    }

	public ConsensusMessage(NetworkParams network, byte[] hash160, long height, byte[] content) {
        super();
        this.protocolVersion = PROTOCOL_VERSION;
        this.hash160 = hash160;
        this.height = height;
        this.content = content;
        this.time = TimeHelper.currentTimeMillis();
        this.nonce = RandomUtil.randomLong();
    }
	
	@Override
	protected void parse() throws ProtocolException {
		//协议号
		protocolVersion = readBytes(1)[0];
		hash160 = readBytes((int) readVarInt());
		height = readUint32();
		time = readInt64();
		nonce = readInt64();
		content = readBytes((int) readVarInt());
		sign = readBytes((int) readVarInt());
		
		length = cursor - offset;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		
		stream.write(getBodyBytes());
		
		if(sign != null) {
			stream.write(new VarInt(sign.length).encode());
			stream.write(sign);
		}
	}
	
	/**
	 * 获取主体内容，签名之前的
	 * @return byte[]
	 */
	public byte[] getBodyBytes() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			stream.write(protocolVersion);
			stream.write(new VarInt(hash160.length).encode());
			stream.write(hash160);
			Utils.uint32ToByteStreamLE(height, stream);
			Utils.int64ToByteStreamLE(time, stream);
			Utils.int64ToByteStreamLE(nonce, stream);
			stream.write(new VarInt(content.length).encode());
			stream.write(content);
			return stream.toByteArray();
		} catch (Exception e) {
			log.error("共识消息序列化出错", e);
			return new byte[0];
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				log.error("关闭字节缓冲器出错", e);
			}
		}
	}

	public Sha256Hash getId() {
		return id;
	}

	public void setId(Sha256Hash id) {
		this.id = id;
	}

	public byte[] getHash160() {
		return hash160;
	}

	public void setHash160(byte[] hash160) {
		this.hash160 = hash160;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
	}

	public byte[] getSign() {
		return sign;
	}

	public void setSign(byte[] sign) {
		this.sign = sign;
	}
	
	public Peer getPeer() {
		return peer;
	}

	public void setPeer(Peer peer) {
		this.peer = peer;
	}

	@Override
	public String toString() {
		return "ConsensusMessage [hash160=" + Hex.encode(hash160) + ", content=" + Hex.encode(content)
				+ ", nonce=" + nonce + ", time=" + time + ", height=" + height + ", sign=" + Hex.encode(sign) + "]";
	}
}
