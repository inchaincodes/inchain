package org.inchain.consensus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.inchain.Configure;
import org.inchain.core.Coin;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.BlockMessage;
import org.inchain.network.NetworkParameters;
import org.inchain.network.NetworkParameters.ProtocolVersion;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地打包交易，当被委托时自动执行
 * @author ln
 *
 */
public final class LocalMining implements Mining {
	
	private final static Logger log = LoggerFactory.getLogger(LocalMining.class);
	
	//运行状态，0没运行，1准备就绪，2打包中，3等待停止，4停止，5异常结束
	private int runState = 0;
	
	private NetworkParameters network;
	private PeerKit peerKit;
	private AccountKit accountKit;
	
	private MempoolContainer mempool = MempoolContainerMap.getInstace();
	
	private BlockStoreProvider blockStoreProvider;
	
	public LocalMining(NetworkParameters network, AccountKit accountKit, PeerKit peerKit) {
		this.network = network;
		this.peerKit = peerKit;
		this.accountKit = accountKit;
		
		blockStoreProvider = BlockStoreProvider.getInstace(Configure.DATA_BLOCK, network);
	}
	
	/**
	 * mining 
	 */
	private void mining() {
		runState = 2;
		
		//如果区块高度不是最新的，那么同步至最新的再开始
		//TODO
		
		BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
		
		Utils.checkNotNull(bestBlockHeader);
		
		//上一区块的时间戳
		long time = bestBlockHeader.getTime();
		
		//被打包的交易列表
		List<TransactionStore> transactionList = new ArrayList<TransactionStore>();
		//预留一个coinbase交易
		transactionList.add(new TransactionStore(network, new Transaction(network)));
		
		Coin fee = Coin.ZERO;
		
		while(true) {
			//每次最多处理1000个交易
			Transaction[] txs = mempool.getNewest(1000);
			
			for (Transaction tx : txs) {
				//如果某笔交易验证失败，则不打包进区块
				if(verifyTx(tx)) {
					transactionList.add(new TransactionStore(network, tx));
				}
				//如果时间到了，那么退出打包，然后广区块
				if(Utils.currentTimeSeconds() - time >= Configure.BLOCK_GEN_TIME) {
					break;
				}
			}
			//如果时间到了，那么退出打包，然后广区块
			if(Utils.currentTimeSeconds() - time >= Configure.BLOCK_GEN_TIME) {
				break;
			}
			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				log.error("mining wait err", e);
			}
		}
		//coinbase交易获取手续费
		Transaction coinBaseTx = transactionList.get(0).getTransaction();
		coinBaseTx.setVersion(Transaction.VERSION);
		coinBaseTx.setType(Transaction.TYPE_COINBASE);
		coinBaseTx.setLockTime(bestBlockHeader.getHeight() + 1 + Configure.MINING_MATURE_COUNT);	//冻结区块数
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		coinBaseTx.addOutput(fee, accountKit.getAccountList().get(0).getAddress());
		coinBaseTx.verfify();
		coinBaseTx.verfifyScript();
		
		//广播区块
		BlockStore blockStore = new BlockStore(network);
		blockStore.setHeight(bestBlockHeader.getHeight()+1);
		blockStore.setPreHash(bestBlockHeader.getHash());
		blockStore.setTime(Utils.currentTimeSeconds());
		blockStore.setVersion(network.getProtocolVersionNum(ProtocolVersion.CURRENT));
		blockStore.setTxCount(transactionList.size());
		blockStore.setTxs(transactionList);
		blockStore.setMerkleHash(blockStore.buildMerkleHash());

		try {
			blockStoreProvider.saveBlock(blockStore);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(log.isDebugEnabled()) {
			log.debug("broadcast new block {}", blockStore);
		}
		//广播
		peerKit.broadcastBlock(new BlockMessage(network, blockStore));
		
		runState = 1;
	}
	
	/**
	 * 验证交易的合法性
	 * @param tx
	 * @return
	 */
	private boolean verifyTx(Transaction tx) {

		Sha256Hash txid = tx.getHash();
		try {
			//运行交易脚本
			tx.verfify();
			tx.verfifyScript();
		} catch (Exception e) {
			log.warn("tx {} verify fail", txid, e);
			return false;
		}
		//一些其它合法性的验证
		try {
			//TODO
			
		} catch (Exception e) {
			log.warn("交易处理失败，txid {} , ", txid, e);
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		try {
			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			log.error("mining err", e);
		}
		
		//这里需要引入共识机制来确定该由谁来打包交易
		//TODO
		
		//假设目前每轮都由自己打包
		runState = 1;
		
		while(true) {
			if(runState == 1) {
				mining();
			}
			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				log.error("mining err", e);
			}
		}
	}
	
	@Override
	public void stop() {
		//强制停止
		runState = 4;
	}
	
	@Override
	public int status() {
		return runState;
	}
	
}
