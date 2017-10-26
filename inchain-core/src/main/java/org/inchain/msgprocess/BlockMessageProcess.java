package org.inchain.msgprocess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.core.Peer;
import org.inchain.core.Result;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.message.Message;
import org.inchain.message.RejectMessage;
import org.inchain.network.NetworkParams;
import org.inchain.service.BlockForkService;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.validator.BlockValidator;
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
	protected BlockForkService blockForkService;
	@Autowired
	protected BlockStoreProvider blockStoreProvider;
	@Autowired
	private BlockValidator blockValidator;
	
	//错误的hash列表
	private List<Sha256Hash> errorHashs = new ArrayList<Sha256Hash>();
	
	/**
	 * 接收到区块消息，进行区块合法性验证，如果验证通过，则收录，然后转发区块
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("down block : {}", message);
		}
		Block block = (Block) message;
		
		lock.lock();
		
		try {
			//区块是否已经存在
			BlockHeaderStore header = blockStoreProvider.getHeader(block.getHash().getBytes());
			if(header != null) {
				//已经存在，但是判断是否是最新的区块，如果不是最新的区块，则说明数据有问题，则自动修复
				BlockHeader bestBlockHeader = blockStoreProvider.getBestBlockHeader().getBlockHeader();
				if(header.getBlockHeader().getHeight() > bestBlockHeader.getHeight()) {
					//重复的块比本地的最新区块还大，那么修复
					BlockStore errorBlock = blockStoreProvider.getBlock(header.getBlockHeader().getHash().getBytes());
					blockStoreProvider.revokedBlock(errorBlock.getBlock());
				} else {
					//已经存在，返回失败
					return replyRejectMessage(block);
				}
			}
			
			//验证区块消息的合法性
			Result verifyReuslt = new Result(true);
			if(block.getHeight()<457377 || block.getHeight()>457477)
				verifyReuslt= blockValidator.verifyBlock(block);
			
			if(!verifyReuslt.isSuccess()) {
				
				blockForkService.addBlockFork(block);
				
				MessageProcessResult result = replyRejectMessage(block);
				result.setErrorCode(verifyReuslt.getErrorCode());
				
				if(result.getErrorCode() == BlockValidator.ERROR_CODE_HEIGHT_ERROR) {
					//掉块容错处理
					//当运行过程中，如果由于网络或者其它原因，导致中间断掉1个或者多个块没有收到，那么这里进行监控并处理
					//监控方法是连续达到的6个块以上，处理办法为重置网络，同步到最新
					int errorHashsSize = errorHashs.size();
					boolean equals = false;
					if(errorHashsSize > 0) {
						equals = block.getPreHash().equals(errorHashs.get(errorHashsSize - 1));
					}
					if(!equals || errorHashsSize > 6) {
						errorHashs.clear();
					} else if(errorHashsSize == 6) {
						//连续N个块出错，重置下载
						BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
						if(block.getHeight() - bestBlockHeader.getBlockHeader().getHeight() < 10) {
							//重置
							peerKit.resetPeers();
							errorHashs.clear();
						}
					}
					errorHashs.add(block.getHash());
				}
				
				return result;
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
			blockForkService.addBlockFork(block);
			log.error(e.getMessage(), e);
			return replyRejectMessage(block);
		} finally {
			lock.unlock();
		}
		return new MessageProcessResult(block.getHash(), true);
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
