package org.inchain.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.transaction.Transaction;
import org.inchain.utils.ConsensusRewardCalculationUtil;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 区块消息
 * @author ln
 *
 */
public class Block extends BlockHeader {

	private static final Logger log = LoggerFactory.getLogger(Block.class);
	
	//交易列表
	private List<Transaction> txs;
		
	public Block(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public Block(NetworkParams network) {
		super(network);
	}

	/**
	 * 序列化
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		txHashs = null;
		super.serializeToStream(stream);
		
		//交易数量
		for (int i = 0; i < txs.size(); i++) {
			Transaction tx = txs.get(i);
			stream.write(tx.baseSerialize());
		}
	}
	
	/**
	 * 反序列化
	 */
	@Override
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
		
		txs = new ArrayList<Transaction>();
		for (int i = 0; i < txCount; i++) {
			Transaction transaction = network.getDefaultSerializer().makeTransaction(payload, cursor);
			txs.add(transaction);
			cursor += transaction.getLength();
		}
		
		length = cursor;
	}
	
	/**
	 * 计算区块的梅克尔树根
	 * @return Sha256Hash
	 */
	public Sha256Hash buildMerkleHash() {
		
		List<byte[]> tree = new ArrayList<byte[]>();
        for (Transaction t : txs) {
            tree.add(t.getHash().getBytes());
        }
        int levelOffset = 0;
        for (int levelSize = txs.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = Utils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = Utils.reverseBytes(tree.get(levelOffset + right));
                tree.add(Utils.reverseBytes(Sha256Hash.hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            levelOffset += levelSize;
        }
        Sha256Hash merkleHash = Sha256Hash.wrap(tree.get(tree.size() - 1));
        if(this.merkleHash == null) {
        	this.merkleHash = merkleHash;
        } else {
        	try {
        		Utils.checkState(this.merkleHash.equals(merkleHash), "the merkle hash is error " + this.merkleHash +"  !=  " + merkleHash+"   blcok:" + toString());
        	} catch (Exception e) {
        		log.warn(e.getMessage(), e);
        		throw e;
			}
        }
		return merkleHash;
	}

	/**
	 * 计算区块hash
	 * @return Sha256Hash
	 */
	public Sha256Hash getHash() {
		Sha256Hash id = Sha256Hash.twiceOf(baseSerialize());
		if(hash != null) {
			Utils.checkState(hash.equals(id), "区块信息不正确");
		} else {
			hash = id;
		}
		return id;
	}
	
	/**
	 * 对区块做基本的验证
	 * 梅克尔树是否正确，coinbase交易是否正确
	 * @return boolean
	 */
	public boolean verify() {
		//验证梅克尔树根是否正确
		if(!buildMerkleHash().equals(getMerkleHash())) {
			throw new VerificationException("block merkle hash error");
		}
		
		//验证交易是否合法
		Coin coinbaseFee = Coin.ZERO; //coinbase 交易包含的金额，主要是手续费
		
		//每个区块只能包含一个coinbase交易，并且只能是第一个
		boolean coinbase = false;
		
		List<Transaction> txs = getTxs();
		for (Transaction tx : txs) {
			//区块的第一个交易必然是coinbase交易，除第一个之外的任何交易都不应是coinbase交易，否则出错
			if(!coinbase) {
				if(tx.getType() != Definition.TYPE_COINBASE) {
					throw new VerificationException("区块的第一个交易必须是coinbase交易");
				}
				coinbaseFee = Coin.valueOf(tx.getOutput(0).getValue());
				coinbase = true;
				continue;
			} else if(tx.getType() == Definition.TYPE_COINBASE) {
				throw new VerificationException("一个块只允许一个coinbase交易");
			}
		}
		//验证金额，coinbase交易的费用必须等于交易手续费
		//获取该高度的奖励
		Coin rewardCoin = ConsensusRewardCalculationUtil.calculatReward(getHeight());
		//不小于奖励，不大于总量
		if(coinbaseFee.isLessThan(rewardCoin) || coinbaseFee.isGreaterThan(Coin.MAX)) {
			log.warn("the fee error");
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Block [hash=");
		builder.append(hash);
		builder.append(", preHash=");
		builder.append(preHash);
		builder.append(", merkleHash=");
		builder.append(merkleHash);
		builder.append(", time=");
		builder.append(time);
		builder.append(", height=");
		builder.append(height);
		builder.append(", txCount=");
		builder.append(txCount);
		builder.append(", periodCount=");
		builder.append(periodCount);
		builder.append(", timePeriod=");
		builder.append(timePeriod);
		builder.append(", periodStartPoint=");
		builder.append(periodStartTime);
		builder.append("]");
		return builder.toString();
	}

	public boolean equals(BlockHeader other) {
		return hash.equals(other.hash) && preHash.equals(other.preHash) && merkleHash.equals(other.merkleHash) && 
				txCount == other.txCount && time == other.time && height == other.height;
	}

	public List<Transaction> getTxs() {
		return txs;
	}

	public void setTxs(List<Transaction> txs) {
		this.txs = txs;
	}
}
