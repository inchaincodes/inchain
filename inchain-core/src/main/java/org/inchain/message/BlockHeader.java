package org.inchain.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.account.Account;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.AccountEncryptedException;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.utils.Utils;

/**
 * 区块头信息
 * @author ln
 *
 */
public class BlockHeader extends Message {

	//区块版本
	protected long version;
	//区块hash
	protected Sha256Hash hash;
	//上一区块hash
	protected Sha256Hash preHash;
	//梅克尔树根节点hash
	protected Sha256Hash merkleHash;
	//时间戳，单位（秒）
	protected long time;
	//区块高度
	protected long height;
	//交易数
	protected long txCount;
	//该时段共识人数
	protected int periodCount;
	//本轮开始的时间点，单位（秒）
	protected long periodStartTime;
	//时段，一轮共识中的第几个时间段，可验证对应的共识人
	protected int timePeriod;
	//签名脚本，包含共识打包人信息和签名，签名是对以上信息的签名
	protected byte[] scriptBytes;
	protected Script scriptSig;
	
	protected List<Sha256Hash> txHashs;
	
	public BlockHeader(NetworkParams network) {
		super(network);
	}
	
	public BlockHeader(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
		super(network, payload, offset);
    }
	
	public BlockHeader(NetworkParams network, byte[] payload) throws ProtocolException {
		super(network, payload, 0);
    }
	
	public void parse() throws ProtocolException {
		version = readUint32();
		preHash = Sha256Hash.wrap(readBytes(32));
		merkleHash = Sha256Hash.wrap(readBytes(32));
		time = readUint32();
		height = readUint32();
		periodCount = (int) readVarInt();
		timePeriod = (int) readVarInt();
		periodStartTime = readUint32();
		scriptBytes = readBytes((int) readVarInt());
		
		scriptSig = new Script(scriptBytes);
		
		txCount = readVarInt();
		
		txHashs = new ArrayList<Sha256Hash>();
		for (int i = 0; i < txCount; i++) {
			txHashs.add(Sha256Hash.wrap(readBytes(32)));
		}
		
		length = cursor - offset;
	}
	
	/**
	 * 序列化区块头
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		Utils.uint32ToByteStreamLE(version, stream);
		stream.write(preHash.getBytes());
		stream.write(merkleHash.getBytes());
		Utils.uint32ToByteStreamLE(time, stream);
		Utils.uint32ToByteStreamLE(height, stream);
		
		stream.write(new VarInt(periodCount).encode());
		stream.write(new VarInt(timePeriod).encode());
		Utils.uint32ToByteStreamLE(periodStartTime, stream);

		stream.write(new VarInt(scriptBytes.length).encode());
		stream.write(scriptBytes);
		
		//交易数量
		stream.write(new VarInt(txCount).encode());
		if(txHashs != null) {
			for (int i = 0; i < txCount; i++) {
				stream.write(txHashs.get(i).getBytes());
			}
		}
    }
	
	/**
	 * 获取区块头部sha256 hash，用于签名验证
	 * @return byte[]
	 * @throws IOException 
	 */
	public Sha256Hash getHeaderHash() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			Utils.uint32ToByteStreamLE(version, stream);
			stream.write(preHash.getBytes());
			stream.write(merkleHash.getBytes());
			Utils.int64ToByteStreamLE(time, stream);
			Utils.uint32ToByteStreamLE(height, stream);
			stream.write(new VarInt(periodCount).encode());
			stream.write(new VarInt(timePeriod).encode());
			Utils.uint32ToByteStreamLE(periodStartTime, stream);
			//交易数量
			stream.write(new VarInt(txCount).encode());
			return Sha256Hash.twiceOf(stream.toByteArray());
		} catch (IOException e) {
			throw e;
		} finally {
			stream.close();
		}
	}
	
	/**
	 * 获取区块头信息
	 * @return BlockHeader
	 */
	public BlockHeader getBlockHeader() {
		BlockHeader blockHeader  = new BlockHeader(network, baseSerialize());
		return blockHeader;
	}
	
	/**
	 * 运行区块签名脚本
	 */
	public void verifyScript() throws VerificationException {
		//运行验证脚本
		BlockHeader temp = new BlockHeader(network, baseSerialize());
		try {
			scriptSig.runVerify(temp.getHeaderHash());
		} catch (IOException e) {
			throw new VerificationException(e);
		}		
	}

	/**
	 *  签名区块头信息
	 * @param account
	 */
	public void sign(Account account) {
		scriptBytes = null;
		Sha256Hash headerHash = null;
		try {
			headerHash = getHeaderHash();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

		//是否加密
		if(!account.isCertAccount() && account.isEncrypted()) {
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
			
			ECDSASignature ecSign = keys[0].sign(headerHash);
			byte[] sign1 = ecSign.encodeToDER();

			scriptSig = ScriptBuilder.createCertAccountScript(Definition.TX_VERIFY_TR, account.getAccountTransaction().getHash(), account.getAddress().getHash160(), sign1, null);
		} else {
			//普通账户
			ECKey key = account.getEcKey();
			
			ECDSASignature ecSign = key.sign(headerHash);
			byte[] sign = ecSign.encodeToDER();
			
			scriptSig = ScriptBuilder.createSystemAccountScript(account.getAddress().getHash160(), key.getPubKey(true), sign);
		}
		scriptBytes = scriptSig.getProgram();
		
		length += scriptBytes.length;
	}
	
	@Override
	public String toString() {
		return "BlockHeader [hash=" + hash + ", preHash=" + preHash + ", merkleHash=" + merkleHash + ", txCount="
				+ txCount + ", time=" + time + ", height=" + height + "]";
	}

	public boolean equals(BlockHeader other) {
		return hash.equals(other.hash) && preHash.equals(other.preHash) && merkleHash.equals(other.merkleHash) && 
				txCount == other.txCount && time == other.time && height == other.height;
	}
	
	/**
	 * 获得区块的打包人
	 * @return byte[]
	 */
	public byte[] getHash160() {
		return scriptSig.getAccountHash160();
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Sha256Hash getHash() {
		return hash;
	}

	public void setHash(Sha256Hash hash) {
		this.hash = hash;
	}

	public Sha256Hash getPreHash() {
		return preHash;
	}

	public void setPreHash(Sha256Hash preHash) {
		this.preHash = preHash;
	}

	public Sha256Hash getMerkleHash() {
		return merkleHash;
	}

	public void setMerkleHash(Sha256Hash merkleHash) {
		this.merkleHash = merkleHash;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public long getTxCount() {
		return txCount;
	}

	public void setTxCount(long txCount) {
		this.txCount = txCount;
	}

	public List<Sha256Hash> getTxHashs() {
		return txHashs;
	}

	public void setTxHashs(List<Sha256Hash> txHashs) {
		this.txHashs = txHashs;
	}

	public int getTimePeriod() {
		return timePeriod;
	}

	public void setTimePeriod(int timePeriod) {
		this.timePeriod = timePeriod;
	}

	public byte[] getScriptBytes() {
		return scriptBytes;
	}

	public void setScriptBytes(byte[] scriptBytes) {
		this.scriptBytes = scriptBytes;
		scriptSig = new Script(scriptBytes);
	}

	public Script getScriptSig() {
		return scriptSig;
	}

	public void setScriptSig(Script scriptSig) {
		this.scriptSig = scriptSig;
	}
	public long getPeriodStartTime() {
		return periodStartTime;
	}
	public void setPeriodStartTime(long periodStartTime) {
		this.periodStartTime = periodStartTime;
	}
	public int getPeriodCount() {
		return periodCount;
	}
	public void setPeriodCount(int periodCount) {
		this.periodCount = periodCount;
	}
}
