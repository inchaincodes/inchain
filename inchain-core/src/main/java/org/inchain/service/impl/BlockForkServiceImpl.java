package org.inchain.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusAccount;
import org.inchain.consensus.ConsensusInfos;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.MeetingItem;
import org.inchain.core.DataSynchronizeHandler;
import org.inchain.core.TimeService;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.PeerKit;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.service.BlockForkService;
import org.inchain.store.BlockForkStore;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.validator.BlockValidator;
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

	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private BlockValidator blockValidator;
	@Autowired
	private DataSynchronizeHandler dataSynchronizeHandler;
	
	private boolean running;
	//本地最新高度hash
	private Sha256Hash localBestHash;
	//本地最新高度hash更新时间
	private long localBestHashLastTime;
	//是否重置
	private boolean hasReset;
	
	//违规列表锁
	private Lock penalizeLock = new ReentrantLock();
	//待处理分叉块列表
	private List<BlockForkStore> blockForks = new CopyOnWriteArrayList<BlockForkStore>();
	//待处理违规块列表
	private List<List<BlockHeader>> penalizeList = new CopyOnWriteArrayList<List<BlockHeader>>();

	public void startSyn() {
		Thread t = new Thread() {
			@Override
			public void run() {
				BlockForkServiceImpl.this.start();
			}
		};
		t.setName("block fork service");
		t.start();
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
		
		//进行初步预筛查
		if(!check(block)) {
			return;
		}
		
		BlockForkStore blockStore = new BlockForkStore(network, block, 0);
		chainstateStoreProvider.put(block.getHash().getBytes(), blockStore.baseSerialize());
		blockForks.add(blockStore);
	}

	/*
	 * 对分叉块进行初步筛查
	 * 如果重复的块，则不继续处理
	 * 这里需要监控两种对系统会造成影响的情况
	 * 1、同一时段，同一节点广播2个以上的块，这些块都会验证通过，不同的节点会接受不同的块，从而造成网络短期分叉。
	 * 2、同一个节点短期内发送大量恶意或者时段能验证通过的块，对网络其它节点造成负荷
	 * 如果是遭受大量垃圾块的恶意攻击，直接丢弃，同时交给miningService做出处罚
	 */
	private boolean check(Block block) {
		for (BlockForkStore blockForkStore : blockForks) {
			//过滤重复的
			if(blockForkStore.getBlock().getHash().equals(block.getHash())) {
				return false;
			}
			//找出同一轮中同一个人的多个块，并交给miningService做出相应的处罚 TODO
			
		}
		return true;
	}

	/*
	 * 监控分叉块
	 */
	private void monitor() {
		while(running) {
			try {
				Thread.sleep(1000l);
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
		
		//如果本地的最新高度1分钟都没有变化，那么则认为本地已经与外界不同步
		//出现这样的情况，说明分叉也解决不了的，所需可能需要同步最新的网络数据
		//最简单的处理方法就是断开所有连接，然后重新初始化网络
		
		BlockHeader bestBlockHeader = network.getBestBlockHeader();
		
		if(!bestBlockHeader.getHash().equals(localBestHash)) {
			localBestHash = bestBlockHeader.getHash();
			localBestHashLastTime = TimeService.currentTimeMillis();
			hasReset = false;
		} else {
			//是否到达1分钟没有变化的条件
			if(TimeService.currentTimeMillis() - localBestHashLastTime > 60000l && !hasReset) {
				//达到条件，触发
				hasReset = true;
				
				dataSynchronizeHandler.reset();
				peerKit.resetPeers();
				
				log.info("触发了1分钟本地高度没改变的条件，重置网络");
				return;
			}
		}
		//另外一种情况，一段时间内接收到的新分叉区块打包人数比本地新增区块打包的人数多，说明处在一个分叉的小网络里面，已经脱离主网运行
		//分叉网络的高度一般落后于主链的，所以只接受新的
		//可以拟定一个条件，接收到的新分叉块，不能比最新时间快太多，否则就存在欺骗的嫌疑
		//这时候需要重置网络
		//TODO
		
		
		for (BlockForkStore blockForkStore : blockForks) {
			//最多分析60次，就丢弃掉
			if(blockForkStore.getProcessCount() > 60) {
				discardBlock(blockForkStore);
			} else {
				blockForkStore.addProcessCount();
				processForkBlock(blockForkStore, null);
			}
		}
	}

	/*
	 * 处理分叉
	 * @param blockForkChain 分叉块的链，也就是分叉块里的组合长度，和主链判断是否超过主链，超过则代表本地应该重新选择
	 */
	private boolean processForkBlock(BlockForkStore blockForkStore, List<BlockForkStore> blockForkChains) {
		
//		log.info("分叉块：{}", blockForkStore.getBlock());
		
		Block block = blockForkStore.getBlock();

		//本地最新块
		BlockHeader localBestBlock = network.getBestBlockHeader();
		
		//这里做一个处理，保证整个网络不被欺骗，一般来说非人为因素的分叉，无非是网络同步快慢的原因
		//有的节点同步很快，有的同步慢，造成衔接不一致，这样的分叉一般在1-3个块即可处理完成，达成一致
		//拜占庭节点的欺骗数据，则不允许进入处理
		//当接收到的分叉块，和本地最新的块，差距过大（目前暂定20个块，这里应该设置得更小 TODO ）时，直接丢弃掉
		if(Math.abs(localBestBlock.getHeight() - block.getHeight()) > 20) {
			discardBlock(blockForkStore);
			return false;
		}
		
		
		//应该丢弃的块
		try {
			if(!block.verify()) {
				discardBlock(blockForkStore);
				return false;
			}
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
		//如果分叉的块比当前时间还要快很多，则可能是恶意节点在诱导分叉，直接丢弃
		if(block.getTime() - TimeService.currentTimeSeconds() > 2 * Configure.BLOCK_GEN_TIME) {
			discardBlock(blockForkStore);
			return false;
		}
		
		//先找到该块的父块再说，如果找不到，则不处理
		Sha256Hash preHash = block.getPreHash();
		//上一个块
		Block preBlock = null;

		if(blockForkChains == null) {
			blockForkChains = new ArrayList<BlockForkStore>();
		}
		
		BlockStore preBlockStore = blockStoreProvider.getBlock(preHash.getBytes());
		if(preBlockStore == null) {
			BlockForkStore preBlockForkStore = findInForkBlocks(preHash);
			if(preBlockForkStore == null) {
				return false;
			}
			//找到了
			blockForkChains.add(blockForkStore);
			//递归处理
			return processForkBlock(preBlockForkStore, blockForkChains);
		} else {
			preBlock = preBlockStore.getBlock();
			
			blockForkChains.add(blockForkStore);
		}
		
		//如果上一个块没有找到，则该块有可能是不合法的块，也有可能是还没有接收
		if(preBlock == null) {
			return false;
		}
		//递归处理之后，如果成功，那么上一个块则会是主链里的块，这样就能验证当前块的所有信息
		
		//块时段和共识人验证通过，但是父块不对应，有可能本地最新块是分叉块
		//分析时段信息
		List<ConsensusAccount> consensusList = consensusMeeting.analysisConsensusSnapshots(block.getPeriodStartTime());
		MeetingItem meeting = new MeetingItem(consensusMeeting, block.getPeriodStartTime(), consensusList);
		meeting.startConsensus();
		
		//已经连上整条链，验证不通过则抛弃
		//验证共识人是否合法
		int timePeriod = meeting.getPeriod(block.getHash160());
		if(block.getTimePeriod() != timePeriod) {
			//新区块打包人验证错误
			discardBlock(blockForkStore);
			return false;
		}
		
		//验证时间戳
		ConsensusInfos currentInfos = meeting.getCurrentConsensusInfos(timePeriod);
		if(currentInfos.getBeginTime() > block.getTime() || currentInfos.getEndTime() < block.getTime()) {
			//新区块打包人验证错误
			discardBlock(blockForkStore);
			return false;
		}

		//验证交易合法性，保证没有双花
		//这里有多种情况需要处理
		if(preBlock.getHash().equals(network.getBestBlockHeader().getHash())) {
			//1、上一个块是最新块，这种情况最好处理，直接把当前块当做最新块来验证即可
			try {
				boolean veriSuccess = blockValidator.verifyBlock(block).isSuccess();
				if(!veriSuccess) {
					log.info("分叉块 高度：{}， hash： {}, 打包人： {} 验证失败，丢弃", block.getHeight(), block.getHash(), new Address(network, block.getHash160()).getBase58());
					//验证失败
					discardBlock(blockForkStore);
					return false;
				}
			} catch (Exception e) {
				//验证错误
				log.info("分叉块 高度：{}， hash： {}, 打包人： {} 验证错误：{}，丢弃", block.getHeight(), block.getHash(), new Address(network, block.getHash160()).getBase58(), e.getMessage());
				discardBlock(blockForkStore);
				return false;
			}
		} else {
			//2、上一个块不是最新块，也就是从之前的分叉了，这时候的交易验证就要复杂一些，因为不能受后面块的影响
			//TODO 暂时先做丢弃处理
			if(localBestBlock.getHeight() - block.getHeight() > 3) {
				log.info("分叉块不是最新的块，丢弃处理， 高度：{}， hash： {}, 打包人： {}", block.getHeight(), block.getHash(), new Address(network, block.getHash160()).getBase58());
				discardBlock(blockForkStore);
				return false;
			}
		}
		
		//判断该链是否最优，也就是超过当前的主链没有，如果超过，则可重置主链为当前链
		//再次查询本地最新高度
		localBestBlock = network.getBestBlockHeader();
		if(preBlock.getHeight() + (blockForkChains == null ? 0 : blockForkChains.size()) > localBestBlock.getHeight() && blockForkChains.size() > 1) {
			//回滚主链上的块，知道最新块为preBlock
			while(true) {
				BlockStore bestBlock = blockStoreProvider.getBestBlock();
				if(bestBlock.getBlock().getHash().equals(preBlock.getHash())) {
					break;
				}
				//先存储到分叉状态链上
				BlockForkStore rockBlockStore = new BlockForkStore(network, bestBlock.getBlock(), 8);
				chainstateStoreProvider.put(rockBlockStore.getBlock().getHash().getBytes(), rockBlockStore.baseSerialize());
				//把回滚的块加入内存，以衔接更长的链
				blockForks.add(rockBlockStore);
				
				Block revokedBlock = blockStoreProvider.revokedNewestBlock();
				//处理并发情况，这里进行回滚的同时，又写入了一个最新的块
				if(revokedBlock.getHash().equals(bestBlock.getBlock().getHash())) {
					//回滚的块不是刚刚查询到的最新的块
					rockBlockStore = new BlockForkStore(network, revokedBlock, 8);
					chainstateStoreProvider.put(rockBlockStore.getBlock().getHash().getBytes(), rockBlockStore.baseSerialize());
					blockForks.add(rockBlockStore);
				}
			}
			
			//加入新链
			for (int i = blockForkChains.size() - 1; i >= 0; i--) {
				BlockForkStore blockFork = blockForkChains.get(i);
				processSuccessForkBlock(blockFork);
				
				try {
					log.info("添加分叉块 {} {} {}", network.getBestBlockHeight(), blockFork.getBlock().getHeight(), blockFork.getBlock().getHash());
					blockStoreProvider.saveBlock(new BlockStore(network, blockFork.getBlock()));
				} catch (VerificationException | IOException e) {
					log.error("添加分叉链到主链失败", e);
					//回滚事务操作？
					return false;
				}
			}
			
			//重新设置共识会议
			consensusMeeting.resetCurrentMeetingItem();
			
			//重新通知最新区块信息
			if(peerKit.getBlockChangedListener() != null) {
				Block newBestBlock = blockForkChains.get(0).getBlock();
				peerKit.getBlockChangedListener().onChanged(newBestBlock.getHeight(), newBestBlock.getHeight(), newBestBlock.getHash(), newBestBlock.getHash());
			}
			
			log.info("已成功添加一条分叉链到主链：{}", blockForkStore.getBlock());
			return true;
		}
		return false;
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
		blockForks.remove(blockForkStore);
	}

	/*
	 * 成功处理了分叉块
	 */
	private void processSuccessForkBlock(BlockForkStore blockForkStore) {
		blockForkStore.setStatus(3);
		chainstateStoreProvider.put(blockForkStore.getBlock().getHash().getBytes(), blockForkStore.baseSerialize());
		blockForks.remove(blockForkStore);
	}
	
	/**
	 * 获取某个账号在某轮共识中的时段
	 * 如果没有找到则返回-1
	 * @param hash160
	 * @param periodStartTime
	 * @return int
	 */
	public int getConsensusPeriod(byte[] hash160, long periodStartTime) {
		List<ConsensusAccount> consensusList = consensusMeeting.analysisConsensusSnapshots(periodStartTime);
		//获取位置
		for (int i = 0; i < consensusList.size(); i++) {
			ConsensusAccount consensusAccount = consensusList.get(i);
			if(Arrays.equals(hash160, consensusAccount.getHash160())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 添加到待处罚列表
	 * @param block
	 */
	@Override
	public void addBlockInPenalizeList(Block block, int type) {
		
		log.info("========={}", block);
		
		penalizeLock.lock();
		try {
			//验证不通过则代表可能是伪造的，不处理
			block.verify();
			block.verifyScript();
			
			if(type == 1) {
				//一轮中重复出块
				//重复的块
				BlockHeader repeatBlock = null;
				//最新块
				BlockHeader bestBlockHeader = blockStoreProvider.getBestBlockHeader().getBlockHeader();
				while(true) {
					//和传入的必须同一轮
					if(bestBlockHeader.getPeriodStartTime() != block.getPeriodStartTime()) {
						break;
					}
					if(Arrays.equals(block.getHash160(), bestBlockHeader.getHash160()) && !bestBlockHeader.getHash().equals(block.getHash())) {
						repeatBlock = bestBlockHeader;
						break;
					}
					bestBlockHeader = blockStoreProvider.getHeader(bestBlockHeader.getPreHash().getBytes()).getBlockHeader();
				}
				
				if(repeatBlock != null) {
					//加入待处理列表
					List<BlockHeader> list = new ArrayList<BlockHeader>();
					list.add(bestBlockHeader);
					list.add(block.getBlockHeader());
					penalizeList.add(list);
					
					log.info("=========加入成功");
				}
			}
		} finally {
			penalizeLock.unlock();
		}
	}
	
	/**
	 * 获取并移除待处罚列表
	 * @return List<BlockHeader>
	 */
	public List<BlockHeader> getAndRemovePenalize() {
		penalizeLock.lock();
		try {
			if(penalizeList.size() > 0) {
				return penalizeList.remove(0);
			} else {
				return null;
			}
		} finally {
			penalizeLock.unlock();
		}
	}
	
	/**
	 * 获取一个块
	 * @param hash
	 * @return Block
	 */
	public Block getBlock(Sha256Hash hash) {
		for (BlockForkStore blockForkStore : blockForks) {
			if(blockForkStore.getBlock().getHash().equals(hash)) {
				return blockForkStore.getBlock();
			}
		}
		
		byte[] blockStroeBytes = chainstateStoreProvider.getBytes(hash.getBytes());
		if(blockStroeBytes == null) {
			return null;
		}
		BlockForkStore blockForkStore = new BlockForkStore(network, blockStroeBytes);
		return blockForkStore.getBlock();
	}
}
