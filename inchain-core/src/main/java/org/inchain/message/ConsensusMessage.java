package org.inchain.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.inchain.account.Account;
import org.inchain.core.Peer;
import org.inchain.core.TimeService;
import org.inchain.core.VarInt;
import org.inchain.core.exception.AccountEncryptedException;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
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
	//时间
	private long time;
	//随机数
	private long nonce;
	//签名
	private byte[][] signs;
	
	public ConsensusMessage(NetworkParams network) {
        super(network);
    }
    
	public ConsensusMessage(NetworkParams network, byte[] payload) throws ProtocolException {
        this(network, payload, 0);
    }
    
	public ConsensusMessage(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
        this(network, payload, offset, network.getProtocolVersionNum(ProtocolVersion.CURRENT));
    }

    public ConsensusMessage(NetworkParams network, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        super(network, payload, offset);
        this.protocolVersion = protocolVersion;
    }

	public ConsensusMessage(NetworkParams network, byte[] hash160, long height, byte[] content) {
        super(network);
        this.protocolVersion = PROTOCOL_VERSION;
        this.hash160 = hash160;
        this.height = height;
        this.content = content;
        this.time = TimeService.currentTimeMillis();
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

		byte[] sign1 = readBytes((int) readVarInt());
		byte[] sign2 = null;
		if(hasMoreBytes()) {
			sign2 = readBytes((int) readVarInt());
		}
		if(sign2 == null) {
			signs = new byte[][] {sign1};
		} else {
			signs = new byte[][] {sign1, sign2};
		}
		
		length = cursor - offset;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		
		stream.write(getBodyBytes());
		
		if(signs != null) {
			for (int i = 0; i < signs.length; i++) {
				byte[] sign = signs[i];
				stream.write(new VarInt(sign.length).encode());
				stream.write(sign);
			}
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

	/**
	 * 签名共识消息
	 * @param account
	 */
	public void sign(Account account) {
		Sha256Hash hash = Sha256Hash.twiceOf(getBodyBytes());
		//是否加密
		if(account.isEncrypted()) {
			throw new AccountEncryptedException();
		}
		if(account.isCertAccount()) {
			//认证账户
			if(account.getAccountTransaction() == null) {
				throw new VerificationException("签名失败，认证账户没有对应的信息交易");
			}
			
			ECKey[] keys = account.getTrEckeys();
			
			if(keys == null) {
				throw new VerificationException("账户没有解密？");
			}
			
			ECDSASignature ecSign = keys[0].sign(hash);
			byte[] sign1 = ecSign.encodeToDER();
			
			ecSign = keys[1].sign(hash);
			byte[] sign2 = ecSign.encodeToDER();
			
			signs = new byte[][] {sign1, sign2};
		} else {
			//普通账户
			ECKey key = account.getEcKey();
			
			ECDSASignature ecSign = key.sign(hash);
			byte[] sign = ecSign.encodeToDER();
			
			signs = new byte[][] {sign};
		}
	}
	
	/**
	 * 验证共识消息签名
	 * @param pubkeys
	 */
	public void verfify(byte[][] pubkeys) {
		if(signs == null || signs.length != pubkeys.length) {
			throw new VerificationException("签名错误，hash160:" + Hex.encode(getHash160()));
		}
		for (int i = 0; i < signs.length; i++) {
			ECKey key = ECKey.fromPublicOnly(pubkeys[i]);
			if(!key.verify(Sha256Hash.twiceOf(getBodyBytes()).getBytes(), signs[i])) {
				throw new VerificationException("错误的共识消息签名信息，hash160:" + Hex.encode(getHash160()));
			}
		}
	}

	public Sha256Hash getId() {
		if(id == null) {
			id = Sha256Hash.twiceOf(baseSerialize());
		}
		return id;
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
	
	public Peer getPeer() {
		return peer;
	}

	public void setPeer(Peer peer) {
		this.peer = peer;
	}

	public byte[][] getSigns() {
		return signs;
	}

	public void setSigns(byte[][] signs) {
		this.signs = signs;
	}

	@Override
	public String toString() {
		return "ConsensusMessage [peer=" + peer + ", id=" + id + ", hash160=" + Hex.encode(hash160) + ", content="
				+ Hex.encode(content) + ", height=" + height + ", nonce=" + nonce + ", signs="
				+ Arrays.toString(signs) + "]";
	}
}
