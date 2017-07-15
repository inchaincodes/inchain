package org.inchain.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.ByteHash;
import org.inchain.core.Coin;
import org.inchain.core.DataSynchronizeHandler;
import org.inchain.core.Definition;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.filter.BloomFilter;
import org.inchain.listener.TransactionListener;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.network.PeerDiscovery;
import org.inchain.script.Script;
import org.inchain.service.CreditCollectionService;
import org.inchain.service.SystemStatusService;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.*;
import org.inchain.utils.RandomUtil;
import org.inchain.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.inchain.core.ViolationEvidence;

/**
 * 区块提供服务
 * @author ln
 *
 */
@Repository
public class BlockStoreProvider extends BaseStoreProvider {

	//区块锁，保证每次新增区块时，不会有并发问题，每次读取最新区块时始终会返回本地最新的一个块
	//当新增时也要检查要保存的块是否和最新的块能衔接上
	private final static Lock blockLock = new ReentrantLock();
	//最新区块标识
	private final static byte[] bestBlockKey = Sha256Hash.ZERO_HASH.getBytes();
	//账户过滤器，用于判断交易是否与我有关
	private BloomFilter accountFilter = new BloomFilter(100000, 0.0001, RandomUtil.randomLong());;
	//区块状态提供器
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	//共识缓存器
	@Autowired
	private ConsensusPool consensusPool;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private CreditCollectionService creditCollectionService;
	@Autowired
	private SystemStatusService systemStatusService;

	//新交易监听器
	private TransactionListener transactionListener;
	
	//异步顺序执行，以免有处理时间较长的线程阻塞，影响性能
	private ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	//缓存
	private final int CACHER_SIZE = 3000;
	//区块头信息缓存
	private Map<ByteHash, byte[]> blockHeaderCacher = new HashMap<ByteHash, byte[]>();
	//区块高度和hash映射缓存
	private Map<ByteHash, byte[]> blockHeightHashCacher = new HashMap<ByteHash, byte[]>();
	//最新区块hash缓存
	private byte[] bestHashCacher = null;
	
	//单例
	BlockStoreProvider() {
		super(Configure.DATA_BLOCK, 50 * 1048576, 20 * 1048576);
	}

	@Override
	protected byte[] toByte(Store store) {
		return null;
	}

	@Override
	protected Store pase(byte[] content) {
		return null;
	}
	
	@PostConstruct
	public void init() {
		initCacher();
//		test();
	}
	
	private void initCacher() {
		//初始化一些缓存数据
		BlockHeaderStore bestBlockHeader = getBestBlockHeader();
		if(bestBlockHeader == null) {
			return;
		}
		Sha256Hash hash = bestBlockHeader.getBlockHeader().getHash();
		int count = CACHER_SIZE;
		while(count -- > 0) {
			if(Sha256Hash.ZERO_HASH.equals(hash)) {
				break;
			}
			BlockHeaderStore blockHeader = getHeader(hash.getBytes());
			if(blockHeader == null) {
				break;
			}
			blockHeaderCacher.put(new ByteHash(hash.getBytes()), blockHeader.getBlockHeader().baseSerialize());
			
			byte[] heightBytes = new byte[4]; 
			Utils.uint32ToByteArrayBE(blockHeader.getBlockHeader().getHeight(), heightBytes, 0);
			blockHeightHashCacher.put(new ByteHash(heightBytes), hash.getBytes());
			
			hash = blockHeader.getBlockHeader().getPreHash();
		}	
	}

	/**
	 * 保存区块完整的区块信息
	 * @param blockStore
	 * @throws IOException 
	 */
	public void saveBlock(BlockStore blockStore) throws IOException, VerificationException {
		blockLock.lock();
		try {
			//最新的区块
			BlockHeaderStore bestBlockHeader = getBestBlockHeader();
			//判断当前要保存的区块，是否是在最新区块之后
			//保存创始块则不限制
			Block block = blockStore.getBlock();
			Sha256Hash hash = block.getHash();
			
			Sha256Hash preHash = block.getPreHash();
			if (preHash == null) {
				throw new VerificationException("要保存的区块缺少上一区块的引用");
			} else if(bestBlockHeader == null && Arrays.equals(bestBlockKey, preHash.getBytes()) && block.getHeight() == 0l) {
				//创世块则通过
			} else if(bestBlockHeader != null && bestBlockHeader.getBlockHeader().getHash().equals(preHash) &&
					bestBlockHeader.getBlockHeader().getHeight() + 1 == block.getHeight()) {
				//要保存的块和最新块能连接上，通过
			} else {
				throw new VerificationException("错误的区块，保存失败");
			}
		
			if(blockStore.getNextHash() == null) {
				blockStore.setNextHash(Sha256Hash.ZERO_HASH);
			}
			//先保存交易，再保存区块，保证区块体不出错
			//保存交易
			for (int i = 0; i < block.getTxCount(); i++) {
				
				Transaction tx = block.getTxs().get(i);
				TransactionStore txs = new TransactionStore(network,  tx, block.getHeight(), null);
		        
				db.put(tx.getHash().getBytes(), txs.baseSerialize());
				
				saveChainstate(block, txs);
			}
			
			//保存块头
			byte[] blockHeaderBytes = blockStore.serializeHeaderToBytes();
			db.put(hash.getBytes(), blockHeaderBytes);
			//写入区块头信息缓存
			blockHeaderCacher.put(new ByteHash(hash.getBytes()), blockHeaderBytes);
			
			byte[] heightBytes = new byte[4]; 
			Utils.uint32ToByteArrayBE(block.getHeight(), heightBytes, 0);
			
			db.put(heightBytes, hash.getBytes());
			//写入区块高度与hash映射缓存
			blockHeightHashCacher.put(new ByteHash(heightBytes), hash.getBytes());
			//TODO 更新过期缓存
			
			//更新最新区块
			db.put(bestBlockKey, hash.getBytes());
			bestHashCacher = hash.getBytes();
			
			//更新上一区块的指针
			if(!Sha256Hash.ZERO_HASH.equals(block.getPreHash())) {
				BlockHeaderStore preBlockHeader = getHeader(block.getPreHash().getBytes());
				preBlockHeader.setNextHash(block.getHash());
				db.put(preBlockHeader.getBlockHeader().getHash().getBytes(), preBlockHeader.baseSerialize());
			}
		} catch (Exception e) {
			log.info("保存区块出错：", e);
			throw e;
		} finally {
			//检查区块数据的完整性
			BlockStore blockStoreTemp = getBlock(blockStore.getBlock().getHash().getBytes());
			if(blockStoreTemp.getBlock().getTxs().size() != (int)blockStore.getBlock().getTxCount()) {
				//交易存储不完整
				revokedNewestBlock();
//				//重新存储
//				saveBlock(blockStore);
				throw new VerificationException("区块保存不完整，实际交易数量为" + blockStore.getBlock().getTxCount() + " , 保存数量为" + blockStoreTemp.getBlock().getTxs());
			}
			
			if(blockHeaderCacher.size() > 2 * CACHER_SIZE) {
				blockHeaderCacher.clear();
				blockHeightHashCacher.clear();
				initCacher();
			}
			blockLock.unlock();
		}
	}

	private void saveChainstate(Block block, TransactionStore txs) throws IOException {
		Transaction tx = txs.getTransaction();
		//TODO 下面的代码请使用状态模式重构
		if(tx instanceof CreditTransaction) {
			//信用值的增减
			CreditTransaction creditTransaction = (CreditTransaction)tx;
			
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(creditTransaction.getOwnerHash160());
			if(accountInfo == null) {
				if(Arrays.equals(creditTransaction.getHash160(), creditTransaction.getOwnerHash160())) {
					//理论上只有普通账户才有可能没信息，注册账户没有注册信息的话，交易验证不通过
					accountInfo = chainstateStoreProvider.createNewAccountInfo(creditTransaction, AccountBody.empty(), new byte[][] {creditTransaction.getPubkey()});
					accountInfo.setSupervisor(creditTransaction.getOwnerHash160());
					accountInfo.setLevel(0);
				} else {
					//不存在时，直接写入信用
					accountInfo = new AccountStore(network);
					accountInfo.setHash160(creditTransaction.getOwnerHash160());
					accountInfo.setType(0); //不确定
					accountInfo.setCert(0);
					accountInfo.setAccountBody(AccountBody.empty());
					accountInfo.setBalance(Coin.ZERO.value);
					accountInfo.setCreateTime(tx.getTime());
					accountInfo.setLastModifyTime(tx.getTime());
					accountInfo.setInfoTxid(tx.getHash());
					accountInfo.setLevel(0);
					accountInfo.setSupervisor(creditTransaction.getOwnerHash160());
					accountInfo.setType(network.getSystemAccountVersion());
					
					//获取公钥，理论上有转账记录
					for (Transaction txTemp : block.getTxs()) {
						if(creditTransaction.getReason().equals(txTemp.getHash())) {
							try {
								accountInfo.setPubkeys(new byte[][] { txTemp.getInput(0).getScriptSig().getPubKey() });
							} catch (Exception e) {
								log.error("通过交易签名脚本获取公钥出错", e);
							}
							break;
						}
					}
					if(accountInfo.getPubkeys() == null) {
						accountInfo.setPubkeys(new byte[][] {});
					}
				}
			}
			//存在时，增加信用
			accountInfo.setCert(accountInfo.getCert() + creditTransaction.getCredit());
			chainstateStoreProvider.saveAccountInfo(accountInfo);
			creditCollectionService.addCredit(creditTransaction.getReasonType(), creditTransaction.getOwnerHash160(), block.getTime());
		} else if(tx.isPaymentTransaction()) {
			
			//转账交易
			//coinbase交易没有输入
			if(tx.getType() != Definition.TYPE_COINBASE) {
				List<TransactionInput> inputs = tx.getInputs();
				for (TransactionInput input : inputs) {
					if(input.getFroms() == null || input.getFroms().size() == 0) {
						continue;
					}
					for (TransactionOutput from : input.getFroms()) {
						//对上一交易的引用以及索引值
						Sha256Hash fromId = from.getParent().getHash();
						int index = from.getIndex();
						
						byte[] key = new byte[fromId.getBytes().length + 1];
						
						System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
						key[key.length - 1] = (byte) index;
						
						chainstateStoreProvider.put(key, new byte[]{ TransactionStore.STATUS_USED});
					}
				}
			}
			//添加输出
			List<TransactionOutput> outputs = tx.getOutputs();
			for (TransactionOutput output : outputs) {
				Sha256Hash id = tx.getHash();
				int index = output.getIndex();
				
				byte[] key = new byte[id.getBytes().length + 1];
				
				System.arraycopy(id.getBytes(), 0, key, 0, key.length - 1);
				key[key.length - 1] = (byte) index;
				
				chainstateStoreProvider.put(key, new byte[]{ TransactionStore.STATUS_UNUSE});
			}
			//特殊业务交易处理
			if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				//生产防伪码
				//把防伪码写进状态表
				AntifakeCodeMakeTransaction atx = (AntifakeCodeMakeTransaction) tx;

				chainstateStoreProvider.put(atx.getAntifakeCode(), tx.getHash().getBytes());
				
				List<Sha256Hash> sources = atx.getSources();
				//如果有来源引用，则写入验证信息
				for (Sha256Hash mtxHash : sources) {
					AntifakeCodeMakeTransaction mtx = (AntifakeCodeMakeTransaction) getTransaction(mtxHash.getBytes()).getTransaction();
					chainstateStoreProvider.verifyAntifakeCode(mtx.getAntifakeCode(), atx.getHash());
				}
			}  else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
				//防伪码验证
				AntifakeCodeVerifyTransaction acvtx = (AntifakeCodeVerifyTransaction) tx;
				
				chainstateStoreProvider.verifyAntifakeCode(acvtx.getAntifakeCode(), acvtx.getHash());
			}else if(tx.getType() == Definition.TYPE_REG_CONSENSUS) {
				
				//如果是共识注册交易，则保存至区块状态表
				RegConsensusTransaction regTransaction = (RegConsensusTransaction)tx;
				chainstateStoreProvider.addConsensus(regTransaction);

			} else if(tx.getType() == Definition.TYPE_REM_CONSENSUS || tx.getType() == Definition.TYPE_VIOLATION) {
				//退出共识
				chainstateStoreProvider.removeConsensus(tx);
				
//				byte[] hash160 = null;
//				if(tx instanceof RemConsensusTransaction) {
//					//主动退出共识
//					RemConsensusTransaction remTransaction = (RemConsensusTransaction)tx;
//					hash160 = remTransaction.getHash160();
//				} else {
//					//违规被提出共识
//					ViolationTransaction vtx = (ViolationTransaction)tx;
//					hash160 = vtx.getViolationEvidence().getAudienceHash160();
//				}

//				//乖节点遵守系统规则，被T则停止共识，否则就会被排除链外
//				Account consensusAccount = consensusMeeting.getAccount();
//				if(consensusAccount != null && Arrays.equals(consensusAccount.getAddress().getHash160(), hash160)) {
//					//下一轮停止共识
//					consensusMeeting.resetCurrentMeetingItem();
//				}
			} else if(tx.getType() == Definition.TYPE_ASSETS_REGISTER) {
				//资产登记
				AssetsRegisterTransaction assetsRegisterTx = (AssetsRegisterTransaction) tx;
				chainstateStoreProvider.assetsRegister(assetsRegisterTx);
			}
		} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER || 
				tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
			//帐户注册和修改账户信息
			CertAccountRegisterTransaction rtx = (CertAccountRegisterTransaction) tx;
			
			//添加账户信息，如果不存在的话
			Sha256Hash oldHash = null;
			
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(rtx.getHash160());
			byte[][] pubkeys = new byte[][] {rtx.getMgPubkeys()[0], rtx.getMgPubkeys()[1], rtx.getTrPubkeys()[0]};
			if(accountInfo == null) {
				accountInfo = chainstateStoreProvider.createNewAccountInfo(rtx, rtx.getBody(), pubkeys);
				accountInfo.setSupervisor(rtx.getSuperhash160());
				accountInfo.setLevel(rtx.getLevel());
			} else {
				oldHash = accountInfo.getInfoTxid();
				accountInfo.setAccountBody(rtx.getBody());
				accountInfo.setLevel(rtx.getLevel());
				accountInfo.setSupervisor(rtx.getSuperhash160());
				accountInfo.setStatus((byte)0);
				accountInfo.setLastModifyTime(rtx.getTime());
				accountInfo.setInfoTxid(rtx.getHash());
				accountInfo.setPubkeys(pubkeys);
			}
			chainstateStoreProvider.saveAccountInfo(accountInfo);
			
			if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
				//删除之前的
				if(oldHash != null) {
					chainstateStoreProvider.delete(oldHash.getBytes());
				}
				//修改之前的信息
				byte[] certAccountHash160s = chainstateStoreProvider.getBytes(Configure.CERT_ACCOUNT_KEYS);
				if(certAccountHash160s == null) {
					certAccountHash160s = new byte[0];
				}
				
				for (int j = 0; j < certAccountHash160s.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
					byte[] addressHash160 = Arrays.copyOfRange(certAccountHash160s, j, j + Address.LENGTH);
					if(Arrays.equals(addressHash160, rtx.getHash160())) {
						byte[] newBusinessHash160s = new byte[certAccountHash160s.length];

						System.arraycopy(certAccountHash160s, 0, newBusinessHash160s, 0, j);
						System.arraycopy(addressHash160, 0, newBusinessHash160s, 0, Address.LENGTH);
						System.arraycopy(rtx.getHash().getBytes(), 0, newBusinessHash160s, 0, Sha256Hash.LENGTH);
						int nextIndex = j + (Address.LENGTH + Sha256Hash.LENGTH);
						if(nextIndex < certAccountHash160s.length) {
							System.arraycopy(certAccountHash160s, nextIndex, newBusinessHash160s, j, certAccountHash160s.length - nextIndex);
						}
						chainstateStoreProvider.put(Configure.CERT_ACCOUNT_KEYS, newBusinessHash160s);
						break;
					}
				}
			} else {
				//账户注册，加入到认证账户列表中
				byte[] certAccountHash160s = chainstateStoreProvider.getBytes(Configure.CERT_ACCOUNT_KEYS);
				if(certAccountHash160s == null) {
					certAccountHash160s = new byte[0];
				}
				byte[] newBusinessHash160s = new byte[certAccountHash160s.length + Address.LENGTH];
				System.arraycopy(certAccountHash160s, 0, newBusinessHash160s, 0, certAccountHash160s.length);
				System.arraycopy(rtx.getHash160(), 0, newBusinessHash160s, certAccountHash160s.length, Address.LENGTH);
				chainstateStoreProvider.put(Configure.CERT_ACCOUNT_KEYS, newBusinessHash160s);
			}
			chainstateStoreProvider.put(rtx.getHash().getBytes(), rtx.baseSerialize());
			
		} else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {
			//普通防伪验证
			GeneralAntifakeTransaction generalAntifakeTransaction = (GeneralAntifakeTransaction) tx;
			
			byte[] antifakeHashBytes = generalAntifakeTransaction.getAntifakeHash().getBytes();
			
			chainstateStoreProvider.put(antifakeHashBytes, tx.getHash().getBytes());
		} else if(tx.getType() == Definition.TYPE_RELEVANCE_SUBACCOUNT) {
			//认证账户关联子账户
			RelevanceSubAccountTransaction relevancSubAccountTx = (RelevanceSubAccountTransaction) tx;
			chainstateStoreProvider.addSubAccount(relevancSubAccountTx);
		} else if(tx.getType() == Definition.TYPE_REMOVE_SUBACCOUNT) {
			//删除子账户的关联
			RemoveSubAccountTransaction removeSubAccountTx = (RemoveSubAccountTransaction) tx;
			chainstateStoreProvider.removeSubAccount(removeSubAccountTx);
		} else if(tx.getType() == Definition.TYPE_REG_ALIAS) {
			//注册别名
			RegAliasTransaction rtx = (RegAliasTransaction) tx;
			chainstateStoreProvider.setAccountAlias(rtx.getHash160(), rtx.getAlias());
		} else if(tx.getType() == Definition.TYPE_UPDATE_ALIAS) {
			//修改别名，消耗信用点
			UpdateAliasTransaction utx = (UpdateAliasTransaction) tx;
			chainstateStoreProvider.updateAccountAlias(utx.getHash160(), utx.getAlias());
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CIRCULATION) {
			//防伪码流转信息
			CirculationTransaction ctx = (CirculationTransaction) tx;
			//放入状态数据库里
			chainstateStoreProvider.addCirculation(ctx.getAntifakeCode(), ctx.getHash160(), ctx.getHash());
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_TRANSFER) {
			AntifakeTransferTransaction attx = (AntifakeTransferTransaction) tx;
			chainstateStoreProvider.antifakeTransfer(attx.getAntifakeCode(), attx.getHash160(), attx.getReceiveHashs(), attx.getHash());
		} else if(tx.getType() == Definition.TYPE_ASSETS_ISSUED) {
			//资产发行
			AssetsIssuedTransaction assetsIssuedTx = (AssetsIssuedTransaction) tx;
			chainstateStoreProvider.assetsIssued(assetsIssuedTx);
		} else if(tx.getType() == Definition.TYPE_ASSETS_TRANSFER) {
			//资产交易
			AssetsTransferTransaction assetsTransferTx = (AssetsTransferTransaction) tx;
			chainstateStoreProvider.assetsTransfer(assetsTransferTx);
		}else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REVOKE){
			CertAccountRevokeTransaction rtx = (CertAccountRevokeTransaction)tx;
			chainstateStoreProvider.addRevokeCertAccount(rtx);
		}else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_BIND){
			AntifakeCodeBindTransaction bindtx = (AntifakeCodeBindTransaction)tx;
			byte [] makebind = new byte[2* Sha256Hash.LENGTH];
			byte [] makebyte = chainstateStoreProvider.getBytes(bindtx.getAntifakeCode());
			System.arraycopy(makebyte,0,makebind,0,Sha256Hash.LENGTH);
			System.arraycopy(tx.getHash().getBytes(),0,makebind,Sha256Hash.LENGTH,Sha256Hash.LENGTH);

			chainstateStoreProvider.put(bindtx.getAntifakeCode(), makebind);
		}
		//交易是否与我有关
		checkIsMineAndUpdate(txs);
	}

	/**
	 * 撤销本地最新块，放入分叉块中，目前先放到状态存储中
	 * 注意块的撤销，只能重最新块依次处理
	 * 需要有出错回滚事务 TODO
	 */
	public Block revokedNewestBlock() {
		blockLock.lock();
		try {
			Block bestBlock = getBestBlock().getBlock();
			if(bestBlock.getHash().equals(network.getGengsisBlock().getBlock().getHash())) {
				//创世块，禁止
				return null;
			}
			revokedBlock(bestBlock);
			//更新最新区块
			byte[] bestBlockHashBytes = bestBlock.getPreHash().getBytes();
			db.put(bestBlockKey, bestBlockHashBytes);
			bestHashCacher = bestBlockHashBytes;
			
			return bestBlock;
		} finally {
			blockLock.unlock();
		}
	}

	/**
	 * 回滚区块
	 * @param block
	 */
	public void revokedBlock(Block block) {
		
		Sha256Hash bestBlockHash = block.getHash();
		
		chainstateStoreProvider.put(bestBlockHash.getBytes(), block.baseSerialize());
		
		//回滚块信息
		db.delete(bestBlockHash.getBytes());
		
		byte[] heightBytes = new byte[4]; 
		Utils.uint32ToByteArrayBE(block.getHeight(), heightBytes, 0);
		
		db.delete(heightBytes);
		
		//删除区块头信息缓存
		blockHeaderCacher.remove(new ByteHash(bestBlockHash.getBytes()));
		blockHeightHashCacher.remove(new ByteHash(heightBytes));
		
		//更新上一区块的指针
		if(!Sha256Hash.ZERO_HASH.equals(block.getPreHash())) {
			BlockHeaderStore preBlockHeader = getHeader(block.getPreHash().getBytes());
			preBlockHeader.setNextHash(Sha256Hash.ZERO_HASH);
			db.put(preBlockHeader.getBlockHeader().getHash().getBytes(), preBlockHeader.baseSerialize());
		}
		
		//回滚交易
		//反转交易，保证回滚时序正确
		//TODO
		for (int i = (int) (block.getTxs().size() - 1); i >= 0 ; i--) {
			TransactionStore txs = new TransactionStore(network, block.getTxs().get(i), block.getHeight(), null);
			
			revokedTransaction(txs);
		}
	}
	
	/**
	 * 重置交易
	 * @param txs
	 */
	public void revokedTransaction(TransactionStore txs) {
		
		Transaction tx = txs.getTransaction();
		
		db.delete(tx.getHash().getBytes());
		
		//把这些交易再次放回内存中
		//除信用交易外
		if(!(tx.getType() == Definition.TYPE_COINBASE ||
				tx.getType() == Definition.TYPE_CREDIT ||
				tx.getType() == Definition.TYPE_VIOLATION)) {
			MempoolContainer.getInstace().add(tx);
		}
		
		if(tx.isPaymentTransaction()) {

			//转账交易
			//coinbase交易没有输入
			if(tx.getType() != Definition.TYPE_COINBASE) {
				List<TransactionInput> inputs = tx.getInputs();
				for (TransactionInput input : inputs) {
					if(input.getFroms() == null || input.getFroms().size() == 0) {
						continue;
					}
					
					for (TransactionOutput from : input.getFroms()) {
						//对上一交易的引用以及索引值
						Sha256Hash fromId = from.getParent().getHash();
						int index = from.getIndex();
						
						byte[] key = new byte[fromId.getBytes().length + 1];
						
						System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
						key[key.length - 1] = (byte) index;
						
						chainstateStoreProvider.put(key, new byte[]{ TransactionStore.STATUS_UNUSE});
					}
				}
			}
			//添加输出
			List<TransactionOutput> outputs = tx.getOutputs();
			for (TransactionOutput output : outputs) {
				Sha256Hash id = tx.getHash();
				int index = output.getIndex();
				
				byte[] key = new byte[id.getBytes().length + 1];
				
				System.arraycopy(id.getBytes(), 0, key, 0, key.length - 1);
				key[key.length - 1] = (byte) index;
				
				chainstateStoreProvider.delete(key);
			}
			//特殊业务交易处理
			if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				//生产防伪码
				//把防伪码写进状态表
				AntifakeCodeMakeTransaction atx = (AntifakeCodeMakeTransaction) tx;

				try {
					chainstateStoreProvider.delete(atx.getAntifakeCode());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
				//防伪码验证
				AntifakeCodeVerifyTransaction acvtx = (AntifakeCodeVerifyTransaction) tx;
				
				chainstateStoreProvider.revokedVerifyAntifakeCode(acvtx.getAntifakeCode());
			} else if(tx.getType() == Definition.TYPE_REG_CONSENSUS) {
				//注册共识，因为是回滚区块，需要从共识列表中删除
				//退出共识
				RegConsensusTransaction regTransaction = (RegConsensusTransaction)tx;
				
				byte[] hash160 = regTransaction.getHash160();
				//从共识账户列表中删除
				chainstateStoreProvider.deleteConsensusFromCollection(hash160);
				
				//乖节点遵守系统规则，被T则停止共识，否则就会被排除链外
				Account consensusAccount = consensusMeeting.getAccount();
				if(consensusAccount != null && Arrays.equals(consensusAccount.getAddress().getHash160(), hash160)) {
					//下一轮停止共识
					consensusMeeting.resetCurrentMeetingItem();
				}
			} else if(tx.getType() == Definition.TYPE_REM_CONSENSUS || tx.getType() == Definition.TYPE_VIOLATION) {
				//退出或者被踢出共识，这里需要再次加入
				chainstateStoreProvider.revokedConsensus(tx);
				
				consensusMeeting.resetCurrentMeetingItem();
			}
			else if(tx.getType() == Definition.TYPE_ASSETS_REGISTER) {
				//回滚资产注册以及资产注册后相关的
				chainstateStoreProvider.rollbackAssetsRegisterTx(tx);
			}
		} else if(tx instanceof CreditTransaction) {
			//信用值的增加
			CreditTransaction creditTransaction = (CreditTransaction)tx;
			
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(creditTransaction.getOwnerHash160());
			//存在时，增加信用
			accountInfo.setCert(accountInfo.getCert() - creditTransaction.getCredit());
			chainstateStoreProvider.saveAccountInfo(accountInfo);

			creditCollectionService.removeCredit(creditTransaction.getReasonType(), creditTransaction.getOwnerHash160());
		} else if(tx.getType() == Definition.TYPE_RELEVANCE_SUBACCOUNT) {
			//认证账户关联子账户
			RelevanceSubAccountTransaction relevancSubAccountTx = (RelevanceSubAccountTransaction) tx;
			chainstateStoreProvider.revokedAddSubAccount(relevancSubAccountTx);
		} else if(tx.getType() == Definition.TYPE_REMOVE_SUBACCOUNT) {
			//删除子账户的关联
			RemoveSubAccountTransaction removeSubAccountTx = (RemoveSubAccountTransaction) tx;
			chainstateStoreProvider.revokedRemoveSubAccount(removeSubAccountTx);
		} else if(tx.getType() == Definition.TYPE_REG_ALIAS) {
			//注册别名
			RegAliasTransaction rtx = (RegAliasTransaction) tx;
			chainstateStoreProvider.revokedSetAccountAlias(rtx.getHash160(), rtx.getAlias());
		} else if(tx.getType() == Definition.TYPE_UPDATE_ALIAS) {
			//修改别名，消耗信用点
			UpdateAliasTransaction utx = (UpdateAliasTransaction) tx;
			chainstateStoreProvider.revokedUpdateAccountAlias(utx.getHash160(), utx.getAlias());
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CIRCULATION) {
			//防伪码流转信息
			CirculationTransaction ctx = (CirculationTransaction) tx;
			//放入状态数据库里
			chainstateStoreProvider.revokedAddCirculation(ctx.getAntifakeCode(), ctx.getHash160(), ctx.getHash());
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_TRANSFER) {
			AntifakeTransferTransaction attx = (AntifakeTransferTransaction) tx;
			chainstateStoreProvider.revokedAntifakeTransfer(attx.getAntifakeCode(), attx.getHash160(), attx.getReceiveHashs(), attx.getHash());
		}else if(tx.getType() == Definition.TYPE_ASSETS_ISSUED) {
			chainstateStoreProvider.rollbackAssetsIssueTx(tx);
		}else if(tx.getType() == Definition.TYPE_ASSETS_TRANSFER) {
			chainstateStoreProvider.rollbackAssetsTransferTx(tx);
		}

		else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER ||
				tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
			
			//帐户注册和修改账户信息
			CertAccountRegisterTransaction rtx = (CertAccountRegisterTransaction) tx;
			
			if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
				//修改
				chainstateStoreProvider.delete(rtx.getHash().getBytes());
				//恢复之前的信息
				try {
					Script script = rtx.getScript();
					byte[] oldTxBytes = script.getChunks().get(1).data;
					TransactionStore txStore = getTransaction(oldTxBytes);
					
					if(txStore != null) {
						Transaction certTx = txStore.getTransaction();
						CertAccountRegisterTransaction newCertTx = (CertAccountRegisterTransaction) certTx;
						
						chainstateStoreProvider.put(newCertTx.getHash().getBytes(), newCertTx.baseSerialize());
						
						//修改之前的信息
						byte[] certAccountHash160s = chainstateStoreProvider.getBytes(Configure.CERT_ACCOUNT_KEYS);
						if(certAccountHash160s == null) {
							certAccountHash160s = new byte[0];
						}
						
						for (int j = 0; j < certAccountHash160s.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
							byte[] addressHash160 = Arrays.copyOfRange(certAccountHash160s, j, j + Address.LENGTH);
							if(Arrays.equals(addressHash160, newCertTx.getHash160())) {
								byte[] newBusinessHash160s = new byte[certAccountHash160s.length];

								System.arraycopy(certAccountHash160s, 0, newBusinessHash160s, 0, j);
								System.arraycopy(addressHash160, 0, newBusinessHash160s, 0, Address.LENGTH);
								System.arraycopy(newCertTx.getHash().getBytes(), 0, newBusinessHash160s, 0, Sha256Hash.LENGTH);
								
								int nextIndex = j + (Address.LENGTH + Sha256Hash.LENGTH);
								if(nextIndex < certAccountHash160s.length) {
									System.arraycopy(certAccountHash160s, nextIndex, newBusinessHash160s, j, certAccountHash160s.length - nextIndex);
								}
								chainstateStoreProvider.put(Configure.CERT_ACCOUNT_KEYS, newBusinessHash160s);
								break;
							}
						}
						
						AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(newCertTx.getHash160());
						byte[][] pubkeys = new byte[][] {newCertTx.getMgPubkeys()[0], newCertTx.getMgPubkeys()[1], newCertTx.getTrPubkeys()[0], newCertTx.getTrPubkeys()[1]};
						accountInfo.setAccountBody(newCertTx.getBody());
						accountInfo.setLastModifyTime(newCertTx.getTime());
						accountInfo.setInfoTxid(newCertTx.getHash());
						accountInfo.setPubkeys(pubkeys);
						chainstateStoreProvider.saveAccountInfo(accountInfo);
					}
					
				} catch (Exception e) {
					log.error("认证账户修改回滚出错，{}", e);
				}
			} else {
				//注册，简单，删除即可
				chainstateStoreProvider.delete(rtx.getHash().getBytes());
				chainstateStoreProvider.delete(rtx.getHash160());
				
				//从列表中删除
				byte[] certAccountHash160s = chainstateStoreProvider.getBytes(Configure.CERT_ACCOUNT_KEYS);
				if(certAccountHash160s != null) {
					for (int j = 0; j < certAccountHash160s.length; j += (Address.LENGTH + Sha256Hash.LENGTH)) {
						byte[] addressHash160 = Arrays.copyOfRange(certAccountHash160s, j, j + Address.LENGTH);
						if(Arrays.equals(addressHash160, rtx.getHash160())) {
							byte[] newBusinessHash160s = new byte[certAccountHash160s.length - (Address.LENGTH + Sha256Hash.LENGTH)];
							
							System.arraycopy(certAccountHash160s, 0, newBusinessHash160s, 0, j);
							
							int nextIndex = j + (Address.LENGTH + Sha256Hash.LENGTH);
							if(nextIndex < certAccountHash160s.length) {
								System.arraycopy(certAccountHash160s, nextIndex, newBusinessHash160s, j, certAccountHash160s.length - nextIndex);
							}
							
							chainstateStoreProvider.put(Configure.CERT_ACCOUNT_KEYS, newBusinessHash160s);
							break;
						}
					}
				}
			}
		} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REVOKE) {
			CertAccountRevokeTransaction retx = (CertAccountRevokeTransaction) tx;
			chainstateStoreProvider.deleteRevokeCertAccount(retx);
		}else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_BIND){
			AntifakeCodeBindTransaction bindtx = (AntifakeCodeBindTransaction)tx;
			byte [] makebyte = new byte[Sha256Hash.LENGTH];
			byte [] makebind = chainstateStoreProvider.getBytes(bindtx.getAntifakeCode());
			if(makebind != null && makebind.length == 2*Sha256Hash.LENGTH) {
				System.arraycopy(makebind, 0, makebyte, 0, Sha256Hash.LENGTH);
				chainstateStoreProvider.put(bindtx.getAntifakeCode(), makebyte);
			}
		}
		else {
			//TODO
		}
		
		//交易是否与我有关
		checkIsMineAndRevoked(txs);
	}

	/**
	 * 检查交易是否与我有关，并且回滚交易状态
	 * @param txs
	 */
	private void checkIsMineAndRevoked(TransactionStore txs) {
		Transaction transaction = txs.getTransaction();
		
		boolean isMine = checkTxIsMine(transaction);
		if(isMine) {
			if(transactionListener != null) {
				transactionListener.revokedTransaction(txs);
			}
		}
	}
	
	/**
	 * 检查交易是否与我有关，并且更新状态
	 * @param txs
	 */
	public void checkIsMineAndUpdate(final TransactionStore txs) {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				Transaction transaction = txs.getTransaction();
				
				boolean isMine = checkTxIsMine(transaction);
				if(isMine) {
					updateMineTx(txs);
				}
			}
		});
	}

	/**
	 * 检查交易是否跟我有关
	 * @param tx
	 * @return boolean
	 */
	public boolean checkTxIsMine(Transaction tx) {
		return checkTxIsMine(tx, null);
	}

	/**
	 * 检查交易是否跟我有关
	 * @param transaction
	 * @return boolean
	 */
	public boolean checkTxIsMine(Transaction transaction, byte[] hash160) {
		//是否是跟自己有关的交易
		if(transaction.isPaymentTransaction()) {
			//普通交易
			//输入
			List<TransactionInput> inputs = transaction.getInputs();
			if(inputs != null && inputs.size() > 0) {
				for (TransactionInput input : inputs) {
					if(input.getFroms() == null || input.getFroms().size() == 0) {
						continue;
					}
					
					for (TransactionOutput from : input.getFroms()) {
						//对上一交易的引用以及索引值
						Sha256Hash fromId = from.getParent().getHash();
						int index = from.getIndex();
						
						TransactionStore txStore = getTransaction(fromId.getBytes());
						if(txStore == null) {
							return false;
						}
						from = (TransactionOutput) txStore.getTransaction().getOutput(index);
						
						Script script = from.getScript();
						if(hash160 == null && script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data) || 
								hash160 != null && script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
							return true;
						}
					}
				}
			}
			//输出
			List<TransactionOutput> outputs = transaction.getOutputs();
			for (TransactionOutput output : outputs) {
				Script script = output.getScript();
				if(hash160 == null && script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data) || 
						hash160 != null && script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
					//如果是coinbase交易，那么交易费大于0的才显示出来
					if(transaction.getType() == Definition.TYPE_COINBASE) {
						if(output.getValue() > 0) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
			if(transaction.getType() == Definition.TYPE_VIOLATION) {
				//特殊的交易，违规处理，很大可能是别人发起的交易
				ViolationTransaction vtx = (ViolationTransaction) transaction;
				byte[]	hash160Temp = vtx.getViolationEvidence().getAudienceHash160();
				//有一种情况，委托共识处理人是被委托人，但实际上是处理的委托人
				byte[] outputHash160 = vtx.getOutput(0).getScript().getAccountHash160();
				if(!Arrays.equals(outputHash160, hash160Temp)) {
					if(vtx.getViolationEvidence().getViolationType() == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
						//超时未出块，被惩罚人是委托人
						hash160Temp = outputHash160;
					} else {
						//重复出块
						TransactionStore regTxStore = getTransaction(vtx.getOutput(0).getParent().getHash().getBytes());
						if(regTxStore == null) {
							return false;
						}
						RegConsensusTransaction regTx = (RegConsensusTransaction) regTxStore.getTransaction();
						hash160Temp = regTx.getHash160();
					}
				}
				if(hash160Temp != null && ((hash160 == null && accountFilter.contains(hash160Temp)) || (hash160 != null && Arrays.equals(hash160, hash160Temp)))) {
					return true;
				}
			}
		} else if(transaction.getType() == Definition.TYPE_CREDIT) {
			//特殊的交易，增加信用，很大可能是别人发起的交易
			CreditTransaction ctx = (CreditTransaction) transaction;
			byte[] hash160Temp = ctx.getOwnerHash160();
			if(hash160Temp != null && ((hash160 == null && accountFilter.contains(hash160Temp)) || (hash160 != null && Arrays.equals(hash160, hash160Temp)))) {
				return true;
			}
		} else if(transaction instanceof BaseCommonlyTransaction) {
			BaseCommonlyTransaction commonlytx = (BaseCommonlyTransaction) transaction;
			if((hash160 == null && accountFilter.contains(commonlytx.getHash160())) || (hash160 != null && Arrays.equals(hash160, commonlytx.getHash160()))) {
				return true;
			}
			//防伪码转让
			if(transaction.getType() == Definition.TYPE_ANTIFAKE_TRANSFER) {
				AntifakeTransferTransaction atx = (AntifakeTransferTransaction) transaction;
				if((hash160 == null && accountFilter.contains(atx.getReceiveHash160())) || (hash160 != null && Arrays.equals(hash160, atx.getReceiveHash160()))) {
					return true;
				}
			} else if(transaction.getType() == Definition.TYPE_ASSETS_ISSUED) {
				AssetsIssuedTransaction assetsIssuedTx = (AssetsIssuedTransaction) transaction;
				if((hash160 == null && accountFilter.contains(assetsIssuedTx.getReceiver())) || (hash160 != null && Arrays.equals(hash160, assetsIssuedTx.getReceiver()))) {
					return true;
				}
			} else if(transaction.getType() == Definition.TYPE_ASSETS_TRANSFER) {
				AssetsTransferTransaction assetsTransferTx = (AssetsTransferTransaction) transaction;
				if((hash160 == null && accountFilter.contains(assetsTransferTx.getReceiver())) || (hash160 != null && Arrays.equals(hash160, assetsTransferTx.getReceiver()))) {
					return true;
				}
			}
		}
		return false;
	}

	//更新与自己相关的交易
	public void updateMineTx(TransactionStore txs) {
		if(transactionListener != null) {
			transactionListener.newTransaction(txs);
		}
	}

	/**
	 * 获取区块头信息
	 * @param hash
	 * @return BlockHeaderStore
	 */
	public BlockHeaderStore getHeader(byte[] hash) {
		
		byte[] content = null;
		content = blockHeightHashCacher.get(new ByteHash(hash));
		
		if(content == null) {
			content = db.get(hash);
		}

		if(content == null) {
			return null;
		}
		BlockHeaderStore blockHeaderStore = new BlockHeaderStore(network, content);
		blockHeaderStore.getBlockHeader().setHash(Sha256Hash.wrap(hash));
		return blockHeaderStore;
	}
	
	/**
	 * 获取区块头信息
	 * @param height
	 * @return BlockHeaderStore
	 */
	public BlockHeaderStore getHeaderByHeight(long height) {
		byte[] heightBytes = new byte[4]; 
		Utils.uint32ToByteArrayBE(height, heightBytes, 0);
		
		byte[] hash = null;
		
		hash = blockHeightHashCacher.get(new ByteHash(heightBytes));
		
		if(hash == null) {
			hash = db.get(heightBytes);
		}
		if(hash == null) {
			return null;
		}
		return getHeader(hash);
	}

	/**
	 * 获取区块
	 * @param height
	 * @return BlockStore
	 */
	public BlockStore getBlockByHeight(long height) {
		return getBlockByHeader(getHeaderByHeight(height));
	}
	
	/**
	 * 获取完整的区块信息
	 * @param hash
	 * @return BlockStore
	 */
		
	public BlockStore getBlock(byte[] hash) {
		BlockHeaderStore header = getHeader(hash);
		if(header == null) {
			return null;
		}
		return getBlockByHeader(header);
	}
	
	/**
	 * 通过区块头获取区块的完整信息，主要是把交易详情查询出来
	 * @param header
	 * @return BlockStore
	 */
	public BlockStore getBlockByHeader(BlockHeaderStore header) {
		//交易列表
		List<Transaction> txs = new ArrayList<Transaction>();
		
		BlockHeader blockHeader = header.getBlockHeader();
		if(blockHeader.getTxHashs() != null) {
			for (Sha256Hash txHash : header.getBlockHeader().getTxHashs()) {
				TransactionStore tx = getTransaction(txHash.getBytes());
				if(tx == null) {
					log.error("Block height {} , tx {} not found", header.getBlockHeader().getHeight(), txHash);
					continue;
				}
				txs.add(tx.getTransaction());
			}
		}
		
		BlockStore blockStore = new BlockStore(network);
		
		Block block = new Block(network);
		block.setTxs(txs);
		block.setVersion(header.getBlockHeader().getVersion());
		block.setHash(header.getBlockHeader().getHash());
		block.setHeight(header.getBlockHeader().getHeight());
		block.setMerkleHash(header.getBlockHeader().getMerkleHash());
		block.setPreHash(header.getBlockHeader().getPreHash());
		block.setTime(header.getBlockHeader().getTime());
		block.setTxCount(header.getBlockHeader().getTxCount());
		block.setPeriodCount(header.getBlockHeader().getPeriodCount());
		block.setTimePeriod(header.getBlockHeader().getTimePeriod());
		block.setPeriodStartTime(header.getBlockHeader().getPeriodStartTime());
		block.setScriptBytes(header.getBlockHeader().getScriptBytes());
		
		blockStore.setBlock(block);
		blockStore.setNextHash(header.getNextHash());
		
		return blockStore;
	}
	
	/**
	 * 获取一笔交易
	 * @param hash
	 * @return TransactionStore
	 */
	public TransactionStore getTransaction(byte[] hash) {
		byte[] content = db.get(hash);
		if(content == null) {
			return null;
		}
		TransactionStore store = new TransactionStore(network, content);
		store.setKey(hash);
		
		return store;
	}
	
	/**
	 * 获取最新块的头信息
	 * @return BlockHeaderStore
	 */
	public BlockHeaderStore getBestBlockHeader() {
		blockLock.lock();
		
		byte[] bestBlockHash = null;
		try {
			if(bestHashCacher == null) {
				bestBlockHash = db.get(bestBlockKey);
				bestHashCacher = bestBlockHash;
			} else {
				bestBlockHash = bestHashCacher;
			}
		} catch (Exception e) {
			return null;
		} finally {
			blockLock.unlock();
		}
		if(bestBlockHash == null) {
			return null;
		} else {
			return getHeader(bestBlockHash);
		}
	}
	
	/**
	 * 获取最新区块的完整信息
	 * @return BlockStore
	 */
	public BlockStore getBestBlock() {
		//获取最新的区块
		BlockHeaderStore header = getBestBlockHeader();
		if(header == null) {
			return null;
		}
		return getBlockByHeader(header);
	}
	
	/**
	 * 初始化账户过滤器
	 * @param hash160s
	 */
	public void initAccountFilter(List<byte[]> hash160s) {
		accountFilter = new BloomFilter(100000, 0.0001, RandomUtil.randomLong());
		for (byte[] hash160 : hash160s) {
			accountFilter.insert(hash160);
		}
	}
	
	/**
	 * 添加账户过滤器记录
	 * @param hash160
	 */
	public void addAccountFilter(byte[] hash160) {
		accountFilter.insert(hash160);
	}
	
	/**
	 * 获取账户过滤器
	 * @return BloomFilter
	 */
	public BloomFilter getAccountFilter() {
		return accountFilter;
	}

	/**
	 * 重新加载相关的所有交易，意味着会遍历整个区块
	 * 该操作一遍只会在账号导入之后进行操作
	 * @param hash160s
	 * @return List<TransactionStore>  返回交易列表
	 */
	public List<TransactionStore> loadRelatedTransactions(List<byte[]> hash160s) {
		blockLock.lock();
		try {
			accountFilter.init();
			for (byte[] hash160 : hash160s) {
				accountFilter.insert(hash160);
			}
			
			//从创始快开始遍历所有区块
			BlockStore blockStore = network.getGengsisBlock();
			Sha256Hash nextHash = blockStore.getBlock().getHash();
			
			List<TransactionStore> mineTxs = new ArrayList<TransactionStore>();
			while(!nextHash.equals(Sha256Hash.ZERO_HASH)) {
				BlockStore nextBlockStore = getBlock(nextHash.getBytes());
				
				Block block = nextBlockStore.getBlock();
				
				List<Transaction> txs = block.getTxs();
				
				for (Transaction tx : txs) {
					
					//普通交易
					if(tx.isPaymentTransaction()) {
						//获取转入交易转入的多少钱
						List<TransactionOutput> outputs = tx.getOutputs();
						
						if(outputs == null) {
							continue;
						}
						//过滤掉coinbase里的0交易
						if(tx.getType() == Definition.TYPE_COINBASE && outputs.get(0).getValue() == 0l) {
							continue;
						}
						
						//交易状态
						byte[] status = new byte[outputs.size()];
						//交易是否跟我有关
						boolean isMineTx = false;
						
						for (int i = 0; i < outputs.size(); i++) {
							Output output = outputs.get(i);
							Script script = output.getScript();
							
							if(script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
								status[i] = TransactionStore.STATUS_UNUSE;
								isMineTx = true;
								break;
							}
						}
						List<TransactionInput> inputs = tx.getInputs();
						if(inputs != null && inputs.size() > 0) {
							for (TransactionInput input : inputs) {
								if(input.getFroms() == null || input.getFroms().size() == 0) {
									continue;
								}
								for (TransactionOutput from : input.getFroms()) {
									Sha256Hash fromTxHash = from.getParent().getHash();
									
									for (TransactionStore transactionStore : mineTxs) {
										Transaction mineTx = transactionStore.getTransaction();
										if(mineTx.getHash().equals(fromTxHash)) {
											//对上一交易的引用以及索引值
											TransactionOutput output = (TransactionOutput) mineTx.getOutput(from.getIndex());
											Script script = output.getScript();
											if(script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
												transactionStore.getStatus()[from.getIndex()] = TransactionStore.STATUS_USED;
												isMineTx = true;
												break;
											}
										}
									}
								}
							}
						}
						
						//除单纯的转账交易外，还有可能有业务逻辑附带代币交易的
						if(!isMineTx && tx.getType() != Definition.TYPE_PAY &&
								tx.getType() != Definition.TYPE_COINBASE) {
							isMineTx = checkTxIsMine(tx);
						}
						
						if(isMineTx) {
							mineTxs.add(new TransactionStore(network, tx, block.getHeight(), status));
						}
					} else {
						boolean isMine = checkTxIsMine(tx);
						if(isMine) {
							mineTxs.add(new TransactionStore(network, tx, block.getHeight(), new byte[]{}));
						}
					}
				}
				nextHash = nextBlockStore.getNextHash();
			}
			
			return mineTxs;
		} finally {
			blockLock.unlock();
		}
	}

	public void addTransactionListener(TransactionListener transactionListener) {
		this.transactionListener = transactionListener;
	}

	/**
	 * 根据时段开始时间，查找区块头信息
	 * @param periodStartTime
	 * @return BlockHeader
	 */
	public BlockHeader getBlockHeaderByperiodStartTime(long periodStartTime) {
		BlockHeaderStore bestBlockHeaderStore = getBestBlockHeader();
		
		while(bestBlockHeaderStore != null) {
			BlockHeader bestBlockHeader = bestBlockHeaderStore.getBlockHeader();
			if(bestBlockHeader.getPreHash().equals(Sha256Hash.ZERO_HASH) || bestBlockHeader.getPeriodStartTime() == periodStartTime) {
				return bestBlockHeader;
			} else {
				bestBlockHeaderStore = getHeader(bestBlockHeader.getPreHash().getBytes());
			}
		}
		return null;
	}
	
	private boolean reseting = false;
	/**
	 * 重建数据
	 */
	public void resetData() {
		if(reseting) {
			return;
		}
		reseting = true;
		
		//设置系统状态
		int oldStatus = systemStatusService.getStatus();
		systemStatusService.setStatus(SystemStatusService.DATA_RESET);
		
		DataSynchronizeHandler handler = SpringContextUtils.getBean(DataSynchronizeHandler.class);
		try {
			handler.downloading();
			
			//第一步，检查区块数据的完整性
			//若发现有数据不完整的块，则重新下载该块
			//第二步，重新扫描区块，重置状态和交易记录
			//分析区块完整性
			log.info("开始分析区块完整性");
			
			BlockStore blockStore = network.getGengsisBlock();
			Sha256Hash nextHash = blockStore.getBlock().getHash();
			Sha256Hash lastBlockHash = null;
			long bestBlockHeight = blockStore.getBlock().getHeight();
			
			List<Long> badBlock = new ArrayList<Long>();
			
			while(!nextHash.equals(Sha256Hash.ZERO_HASH)) {
				BlockStore nextBlockStore = getBlock(nextHash.getBytes());
				if(nextBlockStore == null) {
					break;
				}
				bestBlockHeight = nextBlockStore.getBlock().getHeight();
				
				List<Transaction> txs = nextBlockStore.getBlock().getTxs();
				
				long txCount = nextBlockStore.getBlock().getTxCount();
				
				if(txs.size() != txCount) {
					badBlock.add(nextBlockStore.getBlock().getHeight());
				}
				nextHash = nextBlockStore.getNextHash();
				lastBlockHash = nextBlockStore.getBlock().getHash();
			}

			BlockHeaderStore bestBlockHeaderStore = getBestBlockHeader();

			if(bestBlockHeaderStore == null) {
				db.put(bestBlockKey, lastBlockHash.getBytes());
				bestHashCacher = lastBlockHash.getBytes();
				bestBlockHeaderStore = getBestBlockHeader();
			}
			BlockHeader bestBlockHeader = getBestBlockHeader().getBlockHeader();
			
			log.info("分析区块完整性分析完成，有{}个损坏的区块，本地最新区块{}，实际最新区块{}", badBlock.size(), bestBlockHeader.getHeight(), bestBlockHeight);
			
			//最新高度，重置
			if(bestBlockHeader.getHeight() > bestBlockHeight) {
				BlockStore newBestBlockStore = getBlockByHeight(bestBlockHeight);
				log.info("重置最新区块为{} - {}", newBestBlockStore.getBlock().getHeight(), newBestBlockStore.getBlock().getHash());
				
				byte[] bestBlockHashBytes = newBestBlockStore.getBlock().getHash().getBytes();
				db.put(bestBlockKey, bestBlockHashBytes);
				bestHashCacher = bestBlockHashBytes;
			}
			
			for (Long height : badBlock) {
				log.info("损坏的区块高度为{}", height);
			}
			
			//重置
			if(badBlock.size() > 0) {
				
				long realHeight = badBlock.get(0).longValue();
				
				log.info("开始修复");
				while(true) {
					BlockHeaderStore bestBlock = getBestBlockHeader();
					if(bestBlock.getBlockHeader().getHeight() < realHeight) {
						break;
					}
					revokedNewestBlock();
				}
				log.info("数据回滚成功，开始重置共识队列");
			}

			log.info("开始重置区块状态···");
			
			//重置区块状态
			chainstateStoreProvider.clean();
			//交易记录
			TransactionStoreProvider transactionStoreProvider = SpringContextUtils.getBean(TransactionStoreProvider.class);
			transactionStoreProvider.clean();
			//共识队列
			consensusPool.clearAll();
			//信用记录
			creditCollectionService.clean();
			
			blockStore = network.getGengsisBlock();
			nextHash = blockStore.getBlock().getHash();
			
			while(!nextHash.equals(Sha256Hash.ZERO_HASH)) {
				BlockStore nextBlockStore = getBlock(nextHash.getBytes());
				if(nextBlockStore == null) {
					break;
				}
				Block block = nextBlockStore.getBlock();
				for (int i = 0; i < block.getTxCount(); i++) {
					Transaction tx = block.getTxs().get(i);
					try {
						saveChainstate(nextBlockStore.getBlock(), new TransactionStore(network,  tx, block.getHeight(), null));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				nextHash = nextBlockStore.getNextHash();
			}
			log.info("修复完成，当前最新区块高度为{}，等待数据重新同步", getBestBlockHeader().getBlockHeader().getHeight());
		} catch (Exception e) {
			log.error("修复数据出错", e);
		} finally {
			//重置节点信息
			PeerDiscovery peerDiscovery = SpringContextUtils.getBean(PeerDiscovery.class);
			peerDiscovery.reset();
			
			//重新同步数据
			handler.reset();
			//设置系统状态为之前的
			systemStatusService.setStatus(oldStatus);
			
			reseting = false;
		}
	}
	
	public void resetConsensusQueue() {
		log.info("=========重置共识队列=======");
		//从创始快开始遍历所有区块
		BlockStore blockStore = network.getGengsisBlock();
		Sha256Hash nextHash = blockStore.getBlock().getHash();
		
		//清除共识队列
		consensusPool.clearAll();
		
		while(!nextHash.equals(Sha256Hash.ZERO_HASH)) {
			BlockStore nextBlockStore = getBlock(nextHash.getBytes());
			if(nextBlockStore == null) {
				break;
			}
			List<Transaction> txs = nextBlockStore.getBlock().getTxs();
			
			for (Transaction tx : txs) {
				if(tx.getType() == Definition.TYPE_REG_CONSENSUS) {
					//注册
					RegConsensusTransaction reg = (RegConsensusTransaction) tx;
					chainstateStoreProvider.addConsensus(reg);
				} else if(tx.getType() == Definition.TYPE_REM_CONSENSUS ||
						tx.getType() == Definition.TYPE_VIOLATION) {
					byte[] hash160 = null;
					if(tx instanceof RemConsensusTransaction) {
						//主动退出共识
						RemConsensusTransaction remTransaction = (RemConsensusTransaction)tx;
						hash160 = remTransaction.getHash160();
					} else {
						//违规被提出共识
						ViolationTransaction vtx = (ViolationTransaction)tx;
						hash160 = vtx.getViolationEvidence().getAudienceHash160();
					}
					//从集合中删除共识节点
					chainstateStoreProvider.deleteConsensusFromCollection(hash160);
				}
			}
			nextHash = nextBlockStore.getNextHash();
		}
		log.info("=========重置共识队列完成=======");
	}


	public void test() {
		SpringContextUtils.setNetwork(network);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//从创始快开始遍历所有区块
		BlockStore blockStore = network.getGengsisBlock();
		Sha256Hash nextHash = blockStore.getBlock().getHash();
		
		Address address = Address.fromBase58(network, "toMahRViJBfKJ49QzYymKVb6JqNCLxTPN4");
		
		Set<String> sets = new HashSet<String>();
		
		Sha256Hash hash1 = Sha256Hash.wrap("eb51fabc9f888ee0e0787004f9026b1371fb205805e943a09c1b900191ecbc77");
		Sha256Hash hash2 = Sha256Hash.wrap("501931df0230f8e0a3f74d985e12d1258991295a51a673b613289997274056b2");
		Sha256Hash hash3 = Sha256Hash.wrap("d257ce8652d65fda6860178916b4ee3aae8763ba031c311c4d1f8c81633f9712");

		log.info("======================");
		
		while(!nextHash.equals(Sha256Hash.ZERO_HASH)) {
			BlockStore nextBlockStore = getBlock(nextHash.getBytes());
			
			List<Transaction> txs = nextBlockStore.getBlock().getTxs();
			Block block = nextBlockStore.getBlock();
			if(block.getHeight() == 3600) {
				log.info("block height 3600 tx count is {}, real tx count is {}", block.getTxCount(), block.getTxs().size());
			}
			
			for (Transaction tx : txs) {
				if(tx.getHash().equals(hash1) || tx.getHash().equals(hash2) || tx.getHash().equals(hash3)) {
					log.info("{} , {} ", tx.getHash(), Coin.valueOf(tx.getOutput(1).getValue()).toText());
				}
				
				List<TransactionInput> inputs = tx.getInputs();
				if(inputs == null) {
					continue;
				}
				for (TransactionInput transactionInput : inputs) {
					List<TransactionOutput> froms = transactionInput.getFroms();
					for (TransactionOutput output : froms) {
						if((output.getParent().getHash().equals(hash1) && output.getIndex() == 1) ||
								(output.getParent().getHash().equals(hash2) && output.getIndex() == 1) ||
								(output.getParent().getHash().equals(hash3) && output.getIndex() == 1)) {
							log.info("有引用{}", output.getParent().getHash());
						}
					}
				}
			}
			
			nextHash = nextBlockStore.getNextHash();
			
			if(nextHash.equals(Sha256Hash.ZERO_HASH)) {
				log.info("一共处理{}个区块", nextBlockStore.getBlock().getHeight());
			}
		}
		
		byte[] s1 = new byte[Sha256Hash.LENGTH + 1];
		System.arraycopy(hash1.getBytes(), 0, s1, 0, Sha256Hash.LENGTH);
		s1[Sha256Hash.LENGTH] = (byte)1;
		
		byte[] r1 = chainstateStoreProvider.getBytes(s1);
		log.info("{}", r1);
		
		byte[] s2 = new byte[Sha256Hash.LENGTH + 1];
		System.arraycopy(hash2.getBytes(), 0, s2, 0, Sha256Hash.LENGTH);
		s2[Sha256Hash.LENGTH] = (byte)1;
		
		byte[] r2 = chainstateStoreProvider.getBytes(s2);
		log.info("{}", r2);
		
		byte[] s3 = new byte[Sha256Hash.LENGTH + 1];
		System.arraycopy(hash3.getBytes(), 0, s3, 0, Sha256Hash.LENGTH);
		s3[Sha256Hash.LENGTH] = (byte)1;
		
		byte[] r3 = chainstateStoreProvider.getBytes(s3);
		log.info("{}", r3);
		
		
		log.info("======================");
		
	}
}
