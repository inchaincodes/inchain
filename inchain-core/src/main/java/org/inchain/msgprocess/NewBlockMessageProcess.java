package org.inchain.msgprocess;

import java.util.Date;

import org.inchain.account.Address;
import org.inchain.core.Peer;
import org.inchain.core.Result;
import org.inchain.core.TimeService;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryItem.Type;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.message.VersionMessage;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockHeaderStore;
import org.inchain.transaction.Transaction;
import org.inchain.utils.DateUtil;
import org.inchain.validator.BlockValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 新区块广播消息
 * 接收到新的区块之后，验证该区块是否合法，如果合法则进行收录并转播出去
 * 验证该区块是否合法的流程为：
 * 1、该区块基本的验证（包括区块的时间、大小、交易的合法性，梅克尔树根是否正确）。
 * 2、该区块的广播人是否是合法的委托人。
 * 3、该区块是否衔接最新区块，不允许分叉区块。
 * @author ln
 *
 */
@Service
public class NewBlockMessageProcess extends BlockMessageProcess {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private BlockValidator blockValidator;
	
	/**
	 * 接收到区块消息，进行区块合法性验证，如果验证通过，则收录，然后转发区块
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {

		Block block = (Block) message;
		
		if(log.isDebugEnabled()) {
			log.debug("new block : {}", block.getHash());
		}
		log.info("new block : 当前时间{}, 时间偏移{}, 出块时间{}, 哈希 {}, 高度 {}, 交易数量 {}, 打包人 {}, 开始时间 {}, 当前位置 {}",
				DateUtil.convertDate(new Date(TimeService.currentTimeMillis())), TimeService.getNetTimeOffset(), DateUtil.convertDate(new Date(block.getTime() * 1000)), 
				block.getHash(), block.getHeight(), block.getTxCount(), new Address(network, block.getHash160()).getBase58(), DateUtil.convertDate(new Date(block.getPeriodStartTime()*1000)), block.getTimePeriod());
		
		//验证新区块
		Result valResult = blockValidator.doVal(block);
		if(!valResult.isSuccess()) {
			log.warn("新区块{} 验证失败： {}", block.getHash(), valResult.getMessage());
			
			blockForkService.addBlockFork(block);
			
			return new MessageProcessResult(block.getHash(), false);
		}
		
		//最值该节点的最新高度
		network.setBestHeight(block.getHeight());
		
		//区块不能喝已有的重复
		BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeader(block.getHash().getBytes());
		if(blockHeaderStore != null) {
			return new MessageProcessResult(block.getHash(), false);
		}
		
		MessageProcessResult result = super.process(message, peer);
		
		if(!result.isSuccess()) {
			return result;
		}

		VersionMessage peerVersion = peer.getPeerVersionMessage();
		if(peerVersion != null) {
			peerVersion.setBestHeight(block.getHeight());
		}
		
		for (Transaction tx : block.getTxs()) {
			//移除內存中的交易
			MempoolContainer.getInstace().remove(tx.getHash());
		}
		
		Sha256Hash hash = block.getHash();
		
		//区块变化监听器
		if(peerKit.getBlockChangedListener() != null) {
			peerKit.getBlockChangedListener().onChanged(-1l, block.getHeight(), null, hash);
		}
		
		//转发新区块消息
		if(log.isDebugEnabled()) {
			log.debug("new block {} saved", hash);
		}

		//转发
		InventoryItem item = new InventoryItem(Type.NewBlock, hash);
		InventoryMessage invMessage = new InventoryMessage(peer.getNetwork(), item);
		peerKit.broadcastMessage(invMessage, peer);
		return result;
	}
}
