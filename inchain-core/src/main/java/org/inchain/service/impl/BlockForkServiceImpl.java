package org.inchain.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.inchain.consensus.ConsensusAccount;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.core.Result;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.Block;
import org.inchain.network.NetworkParams;
import org.inchain.service.BlockForkService;
import org.inchain.store.BlockForkStore;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分叉块处理服务
 * @author ln
 *
 */
@Service
public class BlockForkServiceImpl implements BlockForkService {
	
	private final static Logger log = LoggerFactory.getLogger(BlockForkServiceImpl.class);
	
	private CopyOnWriteArrayList<BlockForkStore> blockForks = new CopyOnWriteArrayList<BlockForkStore>();

	@Autowired
	private NetworkParams network;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	
	private boolean running;

	@PostConstruct
	public void init() {
		Thread t = new Thread() {
			@Override
			public void run() {
				BlockForkServiceImpl.this.start();
			}
		};
		t.setName("block fork service");
		t.start();
	}
	
	@PreDestroy
	public void close() {
		stop();
	}
	
	@Override
	public void start() {
		running = true;
		monitor();
	}
	
	@Override
	public void stop() {
		running = false;
	}

	@Override
	public void addBlockFork(Block block) {
		//过滤重复的
		for (BlockForkStore blockForkStore : blockForks) {
			if(blockForkStore.getBlock().getHash().equals(block.getHash())) {
				return;
			}
		}
		BlockForkStore blockStore = new BlockForkStore(network, block, 0);
		chainstateStoreProvider.put(block.getHash().getBytes(), blockStore.baseSerialize());
		blockForks.add(blockStore);
	}

	/*
	 * 监控分叉块
	 */
	private void monitor() {
		while(running) {
			try {
				Thread.sleep(3000l);
				scanning();
			} catch (Exception e) {
				log.error("处理分叉块出错", e);
			}
		}
	}

	/**
	 * 我们假设所有消息都能触达，实际上一个分布式的p2p网络，消息不触达的概率很低
	 * 一旦有分叉块出现，意味着是不能立即添加到主链上的块，可能的原因有以下几种
	 * 应该丢弃的块有以下几种：
	 * 1、其父链之上有双花交易的块
	 * 2、签名不正确的块
	 * 3、重复区块
	 * 
	 * 应该处理的分叉块有以下情况：
	 * 1、块时段和共识人验证通过，但是父块不对应，有可能本地最新块是分叉块
	 * 2、块时段或者共识人不匹配
	 * 3、同一时段的多个块，根据新块选择最长的链，其它的做丢弃处理
	 */
	private void scanning() {
		for (BlockForkStore blockForkStore : blockForks) {
			processForkBlock(blockForkStore);
		}
	}

	private boolean processForkBlock(BlockForkStore blockForkStore) {
		
		log.info("分叉块：{}", blockForkStore.getBlock());
		
		Block block = blockForkStore.getBlock();
		//应该丢弃的块
		try {
			block.verifyScript();
		} catch (Exception e) {
			//签名不正确，丢弃块
			discardBlock(blockForkStore);
			return false;
		}
		//重复块
		BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeader(block.getHash().getBytes());
		if(blockHeaderStore != null) {
			discardBlock(blockForkStore);
			return false;
		}
		//双花块
		Block bestBlock = blockForkStore.getBlock();
		if(block.getPreHash().equals(bestBlock.getHash())) {
			//TODO
			
		}
		
		//先找到该块的父块再说，如果找不到，则不处理
		Sha256Hash preHash = block.getPreHash();
		//上一个块
		Block preBlock = null;
		
		BlockStore preBlockStore = blockStoreProvider.getBlock(preHash.getBytes());
		if(preBlockStore == null) {
			BlockForkStore preBlockForkStore = findInForkBlocks(preHash);
			//递归处理
			if(preBlockForkStore == null || !processForkBlock(preBlockForkStore)) {
				return false;
			} 
			preBlock = preBlockForkStore.getBlock();
		} else {
			preBlock = preBlockStore.getBlock();
		}
		
		//如果上一个块没有找到，则该块有可能是不合法的块，也有可能是还没有接收
		if(preBlock == null) {
			return false;
		}
		//递归处理之后，如果成功，那么上一个块则会是主链里的块，这样就能验证当前块的所有信息
		log.info("preBlock {}", preBlock);
		
		
		//块时段和共识人验证通过，但是父块不对应，有可能本地最新块是分叉块
		//分析时段信息
		BlockHeaderStore startBlockHeader = blockStoreProvider.getHeaderByHeight(block.getPeriodStartPoint());
		if(startBlockHeader == null) {
			return false;
		}
		long startTime = startBlockHeader.getBlockHeader().getTime();
		List<ConsensusAccount> accountList = consensusMeeting.analysisSnapshotsByStartPoint(block.getPeriodStartPoint());
		
		
//		if(!Arrays.equals(currentInfos.getHash160(), block.getHash160())) {
//			log.error("新区块打包人验证错误 : {} {}", block.getHeight(), block.getHash());
//			return new Result(false, "新区块打包人验证错误");
//		}
//		
//		//如果时间不同，则应该放入分叉里
//		if(currentInfos.getBeginTime() > block.getTime() || currentInfos.getEndTime() < block.getTime()) {
//			log.error("新区块时间戳验证出错 : 高度 {} , hash {} , 块时间 {}, 时段 {} - {}", block.getHeight(), block.getHash(),
//					DateUtil.convertDate(new Date(block.getTime())), DateUtil.convertDate(new Date(currentInfos.getBeginTime())), DateUtil.convertDate(new Date(currentInfos.getEndTime())));
//			return new Result(false, "新区块时间戳验证出错");
//		}
		
		return true;
		
	}

	/*
	 * 在分叉块列表中寻找对应的块，找不到则返回null
	 */
	private BlockForkStore findInForkBlocks(Sha256Hash hash) {
		for (BlockForkStore blockForkStore : blockForks) {
			if(blockForkStore.getBlock().getHash().equals(hash)) {
				return blockForkStore;
			}
		}
		return null;
	}

	/*
	 * 丢弃块
	 */
	private void discardBlock(BlockForkStore blockForkStore) {
		blockForkStore.setStatus(1);
		chainstateStoreProvider.put(blockForkStore.getBlock().getHash().getBytes(), blockForkStore.baseSerialize());
	}
	
	/**
	 * 获取某个账号在某轮共识中的时段
	 * 如果没有找到则返回-1
	 * @param hash160
	 * @param startPoint
	 * @return int
	 */
	public int getConsensusPeriod(byte[] hash160, long startPoint) {
		List<ConsensusAccount> consensusList = consensusMeeting.analysisSnapshotsByStartPoint(startPoint);
		//获取位置
		for (int i = 0; i < consensusList.size(); i++) {
			ConsensusAccount consensusAccount = consensusList.get(i);
			if(Arrays.equals(hash160, consensusAccount.getHash160())) {
				return i;
			}
		}
		return -1;
	}
}
