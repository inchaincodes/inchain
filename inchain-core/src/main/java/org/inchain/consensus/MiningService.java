package org.inchain.consensus;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.core.Coin;
import org.inchain.core.TimeHelper;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.NewBlockMessage;
import org.inchain.network.NetworkParams;
import org.inchain.network.NetworkParams.ProtocolVersion;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.ConsensusSigner;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 本地打包交易，当被委托时自动执行
 * @author ln
 *
 */
@Service
public final class MiningService implements Mining {
	
	private final static Logger log = LoggerFactory.getLogger(MiningService.class);
	
	private static MempoolContainer mempool = MempoolContainerMap.getInstace();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private ConsensusPool consensusPool;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private AccountKit accountKit;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
	/**
	 * mining 
	 */
	public void mining() {
		
		//如果区块高度不是最新的，那么同步至最新的再开始
		//TODO
		
		BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
		
		Utils.checkNotNull(bestBlockHeader);
		
		//上一区块的时间戳
		long time = TimeHelper.currentTimeMillis();
		
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
				if(TimeHelper.currentTimeMillis() - time >= Configure.BLOCK_GEN_TIME * 1000) {
					break;
				}
			}
			//如果时间到了，那么退出打包，然后广播区块
			if(TimeHelper.currentTimeMillis() - time >= Configure.BLOCK_GEN_TIME * 1000) {
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
		coinBaseTx.setVersion(TransactionDefinition.VERSION);
		coinBaseTx.setType(TransactionDefinition.TYPE_COINBASE);
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
		blockStore.setTime(TimeHelper.currentTimeMillis());
		blockStore.setVersion(network.getProtocolVersionNum(ProtocolVersion.CURRENT));
		blockStore.setTxCount(transactionList.size());
		blockStore.setTxs(transactionList);
		blockStore.setMerkleHash(blockStore.buildMerkleHash());

		try {
			blockStoreProvider.saveBlock(blockStore);
		} catch (IOException e) {
			log.error("save new block err", e);
		}
		if(log.isDebugEnabled()) {
			log.debug("broadcast new block {}", blockStore);
		}
		//广播
		peerKit.broadcastBlock(new NewBlockMessage(network, blockStore));
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
		
		Account account = null;
		try {
			while(true) {
				//监控自己是否成功成为共识节点
				List<Account> accountList = accountKit.getAccountList();
				if(accountList != null && accountList.size() > 0) {
					account = accountList.get(0);
					if(consensusPool.contains(account.getAddress().getHash160())) {
						break;
					}
				}
				Thread.sleep(5000l);
			}
		} catch (InterruptedException e) {
			log.error("mining err", e);
		}
		
		consensusMeeting.setAccount(account);
		
		//连接到其它节点之后，开始进行共识，如果没有连接，那么等待连接
		while(true) {
			//是否可进行广播
			if(peerKit.canBroadcast()) {
				//拉取一次共识状态，拉取后的信息会通过consensusMeeting.receiveMeetingMessage接收
				BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
				Utils.checkNotNull(bestBlockHeader);
				long height = bestBlockHeader.getHeight();
				
				//content格式第一位为type,1为拉取共识状态信息
				byte[] content = new byte[] { 1 };
				
				ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), height, content);
				//签名共识消息
				ConsensusSigner.sign(message, ECKey.fromPrivate(new BigInteger(account.getPriSeed())));
				consensusMeeting.sendMeetingMessage(message);
				break;
			} else {
				try {
					Thread.sleep(3000l);
				} catch (InterruptedException e) {
					log.error("wait connect err", e);
				}
			}
		}
		
		consensusMeeting.startSyn();
	}
	
	@Override
	public void stop() {
		//强制停止
	}
	
	@Override
	public int status() {
		return 0;
	}
	
}
