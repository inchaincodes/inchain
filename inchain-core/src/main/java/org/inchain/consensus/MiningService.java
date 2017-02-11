package org.inchain.consensus;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.core.Coin;
import org.inchain.core.TimeHelper;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.Block;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryItem.Type;
import org.inchain.message.InventoryMessage;
import org.inchain.network.NetworkParams;
import org.inchain.network.NetworkParams.ProtocolVersion;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.ConsensusSigner;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
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
	@Autowired ChainstateStoreProvider chainstateStoreProvider;

	//运行状态
	private boolean runing;
	
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
		List<Transaction> transactionList = new ArrayList<Transaction>();
		
		Coin fee = Coin.ZERO;
		
		while (true) {
			//每次获取内存里面的一个交易
			Transaction tx = mempool.get();
			
			while(tx != null) {
				//如果某笔交易验证失败，则不打包进区块
				boolean res = verifyTx(transactionList, tx);
				if(res) {
					//交易费
					//只有pay交易才有交易费
					if(tx.getType() == TransactionDefinition.TYPE_PAY) {
						fee = fee.add(getTransactionFee(tx));
					}
					transactionList.add(tx);
				} else {
					//验证失败
					debug("交易验证失败：" + tx.getHash());
				}
				//如果时间到了，那么退出打包，然后广区块
				if(TimeHelper.currentTimeMillis() - time >= Configure.BLOCK_GEN_TIME * 1000) {
					break;
				}
				tx = mempool.get();
			}
			//如果时间到了，那么退出打包，然后广区块
			if(TimeHelper.currentTimeMillis() - time >= Configure.BLOCK_GEN_TIME * 1000) {
				break;
			}

			try {
				Thread.sleep(100l);
			} catch (InterruptedException e) {
				log.error("mining wait err", e);
			}
		}
		
		//coinbase交易
		//coinbase交易获取手续费
		Transaction coinBaseTx = new Transaction(network);
		coinBaseTx.setVersion(TransactionDefinition.VERSION);
		coinBaseTx.setType(TransactionDefinition.TYPE_COINBASE);
		coinBaseTx.setLockTime(bestBlockHeader.getBlockHeader().getHeight() + 1 + Configure.MINING_MATURE_COUNT);	//冻结区块数
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		log.info("高度： "+bestBlockHeader.getBlockHeader().getHeight()+"=======================手续费： " + fee);
		
		coinBaseTx.addOutput(fee, accountKit.getAccountList().get(0).getAddress());
		coinBaseTx.verfifyScript();
		
		//加入coinbase交易到交易列表
		transactionList.add(0, coinBaseTx);
		
		//广播区块
		Block block = new Block(network);

		block.setHeight(bestBlockHeader.getBlockHeader().getHeight()+1);
		block.setPreHash(bestBlockHeader.getBlockHeader().getHash());
		block.setTime(TimeHelper.currentTimeMillis());
		block.setVersion(network.getProtocolVersionNum(ProtocolVersion.CURRENT));
		block.setTxCount(transactionList.size());
		block.setTxs(transactionList);
		block.setMerkleHash(block.buildMerkleHash());
		
		BlockStore blockStore = new BlockStore(network, block);
		

		try {
			blockStoreProvider.saveBlock(blockStore);
			if(log.isDebugEnabled()) {
				log.debug("broadcast new block {}", blockStore);
			}
			//广播
			InventoryItem item = new InventoryItem(Type.NewBlock, block.getHash());
			InventoryMessage invMessage = new InventoryMessage(network, item);
			peerKit.broadcastMessage(invMessage);
			
//			peerKit.broadcastBlock(new NewBlockMessage(network, block.baseSerialize()));
		} catch (IOException e) {
			log.error("共识产生的新块保存时报错", e);
		}
	}
	
	/*
	 * 获取交易的手续费
	 */
	private Coin getTransactionFee(Transaction tx) {
		Coin inputFee = Coin.ZERO;
		
		List<Input> inputs = tx.getInputs();
		for (Input input : inputs) {
			inputFee = inputFee.add(Coin.valueOf(input.getFrom().getValue()));
		}
		
		Coin outputFee = Coin.ZERO;
		List<Output> outputs = tx.getOutputs();
		for (Output output : outputs) {
			outputFee = outputFee.add(Coin.valueOf(output.getValue()));
		}
		return inputFee.subtract(outputFee);
	}

	/*
	 * 验证交易的合法性
	 * @param transactionList	//本次已打包的交易列表
	 * @param tx				//本次打包的交易
	 * @return boolean
	 */
	private boolean verifyTx(List<Transaction> transactionList, Transaction tx) {

		try {
			//验证交易的合法性
			tx.verfifyScript();
			
			//交易的txid不能和区块里面的交易重复
			TransactionStore verifyTX = blockStoreProvider.getTransaction(tx.getHash().getBytes());
			if(verifyTX != null) {
				throw new VerificationException("交易hash与区块里的重复");
			}
			//如果是转帐交易
			if(tx.getType() == TransactionDefinition.TYPE_PAY) {
				//验证交易的输入来源，是否已花费的交易，同时验证金额
				Coin txInputFee = Coin.ZERO;
				Coin txOutputFee = Coin.ZERO;
				
				//验证本次交易的输入
				List<Input> inputs = tx.getInputs();
				for (Input input : inputs) {
					TransactionInput tInput = (TransactionInput) input;
					TransactionOutput output = tInput.getFrom();
					if(output == null) {
						throw new VerificationException("转账交易空的输出");
					}
					//对上一交易的引用以及索引值
					Sha256Hash fromId = output.getParent().getHash();
					int index = output.getIndex();
					
					byte[] key = new byte[fromId.getBytes().length + 1];
					
					System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
					key[key.length - 1] = (byte) index;
					
					//查询上次的交易
					Transaction preTransaction = null;
					
					//判断是否未花费
					byte[] state = chainstateStoreProvider.getBytes(key);
					if(!Arrays.equals(state, new byte[]{1})) {
						//链上没有该笔交易，这时有两种情况，第一确实是伪造交易，第二花费了没有确认的交易
						//对于第一种情况，验证不通过
						//第二种情况，我们顺序打包交易，也就是引用的输出必须是本次已打包的，否则就扔回内存池
						
						//首先判断是否已打包
						boolean hasPackage = false;
						for (Transaction transaction : transactionList) {
							if(transaction.getHash().equals(fromId)) {
								preTransaction = transaction;
								hasPackage = true;
								break;
							}
						}
						//已打包就通过
						if(!hasPackage) {
							//没打包，判断内存里面是否存在
							preTransaction = MempoolContainerMap.getInstace().get(fromId);
							if(preTransaction == null) {
								throw new VerificationException("引用了不存在或不可用的交易");
							} else {
								//在内存池里面，那么就把本笔交易扔回内存池，等待下次打包
								MempoolContainerMap.getInstace().add(tx);
								throw new VerificationException("该交易打包顺序不对");
							}
						}
					} else {
						//查询上次的交易
						preTransaction = blockStoreProvider.getTransaction(fromId.getBytes()).getTransaction();
						if(preTransaction == null) {
							throw new VerificationException("引用了不存在的交易");
						}
					}
					TransactionOutput perOutput = (TransactionOutput) preTransaction.getOutput(index);
					txInputFee = txInputFee.add(Coin.valueOf(perOutput.getValue()));
					output.setValue(perOutput.getValue());
				}
				//验证本次交易的输出
				List<Output> outputs = tx.getOutputs();
				for (Output output : outputs) {
					TransactionOutput tOutput = (TransactionOutput) output;
					Coin outputCoin = Coin.valueOf(tOutput.getValue());
					//输出金额不能为负数
					if(outputCoin.isLessThan(Coin.ZERO)) {
						throw new VerificationException("输出金额不能为负数");
					}
					txOutputFee = txOutputFee.add(outputCoin);
					//FIXME 是否验证必须输出到已有的帐户 ???
				}
				//输出金额不能大于输入金额
				if(txOutputFee.isGreaterThan(txInputFee)) {
					throw new VerificationException("输出金额不能大于输入金额");
				}
			} else if(tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER) {
				//帐户注册
				CertAccountRegisterTransaction regTx = (CertAccountRegisterTransaction) tx;
				//注册的hash160地址，不能与现有的地址重复，当然正常情况重复的机率为0，不排除有人恶意广播数据
				byte[] hash160 = regTx.getHash160();
				
				byte[] txid = chainstateStoreProvider.getBytes(hash160);
				if(txid != null) {
					throw new VerificationException("注册的账户重复");
				}
				
				//验证账户注册，必须是超级账号签名的才能注册
				byte[] verTxid = regTx.getScript().getChunks().get(1).data;
				byte[] verTxBytes = chainstateStoreProvider.getBytes(verTxid);
				if(verTxBytes == null) {
					throw new VerificationException("签名错误");
				}
				CertAccountRegisterTransaction verTx = new CertAccountRegisterTransaction(network, verTxBytes);
				
				//认证帐户，就需要判断是否经过认证的
				if(!Arrays.equals(verTx.getHash160(), network.getCertAccountManagerHash160())) {
					throw new VerificationException("账户没有经过认证");
				}
			}
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public void start() {
		
		runing = true;
		
		Account account = null;
		try {
			while(true && runing) {
				//监控自己是否成功成为共识节点
				List<Account> accountList = accountKit.getAccountList();
				if(accountList != null && accountList.size() > 0) {
					account = accountList.get(0);
					if(consensusPool.contains(account.getAddress().getHash160())) {
						break;
					}
				}
				Thread.sleep(1000l);
			}
		} catch (InterruptedException e) {
			log.error("mining err", e);
		}
		
		consensusMeeting.setAccount(account);
		
		//连接到其它节点之后，开始进行共识，如果没有连接，那么等待连接
		while(true && runing) {
			//是否可进行广播
			if(peerKit.canBroadcast()) {
				//拉取一次共识状态，拉取后的信息会通过consensusMeeting.receiveMeetingMessage接收
				BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
				Utils.checkNotNull(bestBlockHeader);
				long height = bestBlockHeader.getBlockHeader().getHeight();
				
				//content格式第一位为type,1为拉取共识状态信息
				byte[] content = new byte[] { 1 };
				
				ConsensusMessage message = new ConsensusMessage(network, account.getAddress().getHash160(), height, content);
				//签名共识消息
				ConsensusSigner.sign(message, ECKey.fromPrivate(new BigInteger(account.getPriSeed())));
				consensusMeeting.sendMeetingMessage(message);
				break;
			} else {
				try {
					Thread.sleep(1000l);
				} catch (InterruptedException e) {
					log.error("wait connect err", e);
				}
			}
		}
		
		if(runing) {
			consensusMeeting.startSyn();
		}
	}
	
	@Override
	public void stop() {
		runing = false;
		//强制停止
//		consensusMeeting.stop();
	}
	
	@Override
	public int status() {
		return 0;
	}

	private void debug(String debugMsg) {
		if(log.isDebugEnabled()) {
			log.debug(debugMsg);
		}
	}
}
