package org.inchain.msgprocess;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.core.Coin;
import org.inchain.core.Peer;
import org.inchain.core.Definition;
import org.inchain.core.exception.VerificationException;
import org.inchain.kits.PeerKit;
import org.inchain.message.Block;
import org.inchain.message.Message;
import org.inchain.message.RejectMessage;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Transaction;
import org.inchain.utils.ConsensusRewardCalculationUtil;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.validator.ValidatorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 下载区块的消息
 * 接收到新的区块之后，验证该区块是否合法，如果合法则进行收录并转播出去
 * 验证该区块是否合法的流程为：
 * 1、该区块基本的验证（包括区块的时间、大小、交易的合法性，梅克尔树根是否正确）。
 * 2、该区块的广播人是否是合法的委托人。
 * 3、该区块是否衔接最新区块，不允许分叉区块。
 * @author ln
 *
 */
@Service
public class BlockMessageProcess implements MessageProcess {

	private static final Logger log = LoggerFactory.getLogger(BlockMessageProcess.class);

	private static Lock lock = new ReentrantLock();
	
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private NetworkParams network;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	@Autowired
	private TransactionValidator transactionValidator;
	
	/**
	 * 接收到区块消息，进行区块合法性验证，如果验证通过，则收录，然后转发区块
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("down block : {}", message);
		}
		
		lock.lock();
		
		Block block = (Block) message;
		block.verifyScript();
		
		try {
			BlockHeaderStore header = blockStoreProvider.getHeader(block.getHash().getBytes());
			if(header != null) {
				//已经存在
				return replyRejectMessage(block);
			}
			
			//验证区块消息的合法性
			if(!verifyBlock(block)) {
				return replyRejectMessage(block);
			}
			
			//验证通过 ，存储区块数据
			try {
				BlockStore blockStore = new BlockStore(network, block);
				blockStoreProvider.saveBlock(blockStore);
			} catch (IOException e) {
				throw new VerificationException(e);
			}
			
			//区块变化监听器
			if(peerKit.getBlockChangedListener() != null) {
				peerKit.getBlockChangedListener().onChanged(block.getHeight(), -1l, block.getHash(), null);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return replyRejectMessage(block);
		} finally {
			lock.unlock();
		}
		return new MessageProcessResult(block.getHash(), true);
	}

	/*
	 * 验证区块的合法性，如果验证不通过，则抛出验证异常
	 */
	private boolean verifyBlock(Block block) {
		//获取区块的最新高度
		BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
		//必需衔接
		if(!block.getPreHash().equals(bestBlockHeader.getBlockHeader().getHash()) ||
				block.getHeight() != bestBlockHeader.getBlockHeader().getHeight() + 1) {
			log.warn("block info warn");
			return false;
		}
		//验证区块签名
		//TODO
		
		//验证梅克尔树根是否正确
		if(!block.buildMerkleHash().equals(block.getMerkleHash())) {
			throw new VerificationException("block merkle hash error");
		}
		
		//验证交易是否合法
		Coin coinbaseFee = Coin.ZERO; //coinbase 交易包含的金额，主要是手续费
		Coin fee = Coin.ZERO; 		  //手续费
		
		//每个区块只能包含一个coinbase交易，并且只能是第一个
		boolean coinbase = false;
		
		List<Transaction> txs = block.getTxs();
		for (Transaction tx : txs) {
			
			ValidatorResult<TransactionValidatorResult> rs = transactionValidator.valDo(tx, txs);
			
			if(!rs.getResult().isSuccess()) {
				throw new VerificationException(rs.getResult().getMessage());
			}
			
			//区块的第一个交易必然是coinbase交易，除第一个之外的任何交易都不应是coinbase交易，否则出错
			if(!coinbase) {
				if(tx.getType() != Definition.TYPE_COINBASE) {
					throw new VerificationException("the block first tx is not coinbase tx");
				}
				coinbaseFee = Coin.valueOf(tx.getOutput(0).getValue());
				coinbase = true;
				continue;
			} else if(tx.getType() == Definition.TYPE_COINBASE) {
				throw new VerificationException("the block too much coinbase tx");
			}
			if(rs.getResult().getFee() != null) {
				fee = fee.add(rs.getResult().getFee());
			}
		}
		//验证金额，coinbase交易的费用必须等于交易手续费
		//获取该高度的奖励
		Coin rewardCoin = ConsensusRewardCalculationUtil.calculat(block.getHeight());
		if(!coinbaseFee.equals(fee.add(rewardCoin))) {
			log.warn("the fee error");
			return false;
		}
		return true;
	}
	
	/**
	 * 回复拒绝消息
	 * @param block
	 * @return MessageProcessResult
	 */
	protected MessageProcessResult replyRejectMessage(Block block) {
		RejectMessage replyMessage = new RejectMessage(network, block.getHash());
		return new MessageProcessResult(block.getHash(), false, replyMessage);
	}
}
