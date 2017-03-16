package org.inchain.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.ViolationEvidence;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.filter.BloomFilter;
import org.inchain.listener.TransactionListener;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.script.Script;
import org.inchain.service.CreditCollectionService;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.BaseCommonlyTransaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.CreditTransaction;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.transaction.business.RegConsensusTransaction;
import org.inchain.transaction.business.RemConsensusTransaction;
import org.inchain.transaction.business.ViolationTransaction;
import org.inchain.utils.RandomUtil;
import org.inchain.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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

	//新交易监听器
	private TransactionListener transactionListener;
	
	//单例
	BlockStoreProvider() {
		super(Configure.DATA_BLOCK);
	}

	@Override
	protected byte[] toByte(Store store) {
		return null;
	}

	@Override
	protected Store pase(byte[] content) {
		return null;
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
				TransactionStore txs = new TransactionStore(network, block.getTxs().get(i), block.getHeight(), null);
		        
				Transaction tx = txs.getTransaction();
				
				db.put(tx.getHash().getBytes(), txs.baseSerialize());
				
				//如果是共识注册交易，则保存至区块状态表
				//TODO 下面的代码请使用状态模式重构
				if(tx instanceof RegConsensusTransaction) {
					RegConsensusTransaction regTransaction = (RegConsensusTransaction)tx;
					
					//注册共识，加入到共识账户列表中
					byte[] consensusAccountHash160s = chainstateStoreProvider.getBytes(Configure.CONSENSUS_ACCOUNT_KEYS);
					if(consensusAccountHash160s == null) {
						consensusAccountHash160s = new byte[0];
					}
					byte[] hash160 = regTransaction.getHash160();
					byte[] newConsensusHash160s = new byte[consensusAccountHash160s.length + Address.LENGTH];
					System.arraycopy(consensusAccountHash160s, 0, newConsensusHash160s, 0, consensusAccountHash160s.length);
					System.arraycopy(hash160, 0, newConsensusHash160s, consensusAccountHash160s.length, Address.LENGTH);
					chainstateStoreProvider.put(Configure.CONSENSUS_ACCOUNT_KEYS, newConsensusHash160s);

					//添加账户信息，如果不存在的话
					AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
					if(accountInfo == null) {
						//理论上只有普通账户才有可能没信息，注册账户没有注册信息的话，交易验证不通过
						accountInfo = createNewAccountInfo(regTransaction, AccountBody.empty(), new byte[][] {regTransaction.getPubkey()});
						chainstateStoreProvider.put(hash160, accountInfo.baseSerialize());
					} else {
						//不确定的账户，现在可以确定下来了
						updateAccountInfo(accountInfo, regTransaction);
					}
					//公钥
					byte[][] pubkeys = accountInfo.getPubkeys();
					//添加到共识缓存器里
					consensusPool.add(regTransaction.getHash160(), pubkeys);
					//下一轮开始打包
					consensusMeeting.startConsensusOnNextRound(block);

				} else if(tx instanceof RemConsensusTransaction || tx instanceof ViolationTransaction) {
					//退出共识
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
					
					//从共识账户列表中删除
					byte[] consensusAccountHash160s = chainstateStoreProvider.getBytes(Configure.CONSENSUS_ACCOUNT_KEYS);
					
					byte[] newConsensusHash160s = new byte[consensusAccountHash160s.length - Address.LENGTH];
					
					//找出位置在哪里
					//判断在列表里面才更新，否则就被清空了
					boolean hashExist = false;
					for (int j = 0; j < consensusAccountHash160s.length; j += Address.LENGTH) {
						byte[] addressHash160 = Arrays.copyOfRange(consensusAccountHash160s, j, j + Address.LENGTH);
						if(Arrays.equals(addressHash160, hash160)) {
							hashExist = true;
							System.arraycopy(consensusAccountHash160s, 0, newConsensusHash160s, 0, j);
							System.arraycopy(consensusAccountHash160s, j + Address.LENGTH, newConsensusHash160s, j, consensusAccountHash160s.length - j - Address.LENGTH);
						}
					}
					if(hashExist) {
						chainstateStoreProvider.put(Configure.CONSENSUS_ACCOUNT_KEYS, newConsensusHash160s);
					}
					//从共识缓存器里中移除
					consensusPool.delete(hash160);

					//乖节点遵守系统规则，被T则停止共识，否则就会被排除链外
					Account consensusAccount = consensusMeeting.getAccount();
					if(consensusAccount != null && Arrays.equals(consensusAccount.getAddress().getHash160(), hash160)) {
						//下一轮停止共识
						consensusMeeting.stopConsensusOnNextRound(block);
					}
					
					//退出的账户
					if(tx instanceof ViolationTransaction) {
						//违规被提出共识，增加规则证据到状态里，以便查证
						ViolationTransaction vtx = (ViolationTransaction)tx;
						ViolationEvidence violationEvidence = vtx.getViolationEvidence();
						Sha256Hash evidenceHash = violationEvidence.getEvidenceHash();
						chainstateStoreProvider.put(evidenceHash.getBytes(), tx.getHash().getBytes());
						
						//减去相应的信用值
						AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
						long certChange = 0;
						if(violationEvidence.getViolationType() == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
							certChange = Configure.CERT_CHANGE_TIME_OUT;
						}
						
						accountInfo.setCert(accountInfo.getCert() + certChange);
						
						chainstateStoreProvider.saveAccountInfo(accountInfo);
					}
				} else if(tx instanceof CreditTransaction) {
					//信用值的增减
					CreditTransaction creditTransaction = (CreditTransaction)tx;
					
					AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(creditTransaction.getOwnerHash160());
					if(accountInfo == null) {
						if(Arrays.equals(creditTransaction.getHash160(), creditTransaction.getOwnerHash160())) {
							//理论上只有普通账户才有可能没信息，注册账户没有注册信息的话，交易验证不通过
							accountInfo = createNewAccountInfo(creditTransaction, AccountBody.empty(), new byte[][] {creditTransaction.getPubkey()});
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
							accountInfo.setCert(creditTransaction.getCredit());
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
						List<Input> inputs = tx.getInputs();
						for (Input input : inputs) {
							TransactionInput tInput = (TransactionInput) input;
							TransactionOutput output = tInput.getFrom();
							if(output == null) {
								throw new VerificationException("error input");
							}
							//对上一交易的引用以及索引值
							Sha256Hash fromId = output.getParent().getHash();
							int index = output.getIndex();
							
							byte[] key = new byte[fromId.getBytes().length + 1];
							
							System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
							key[key.length - 1] = (byte) index;
							
							chainstateStoreProvider.delete(key);
						}
					}
					//添加输出
					List<Output> outputs = tx.getOutputs();
					for (Output output : outputs) {
						TransactionOutput tOutput = (TransactionOutput) output;
						
						Sha256Hash id = tx.getHash();
						int index = tOutput.getIndex();
						
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

						chainstateStoreProvider.put(atx.getAntifakeHash().getBytes(), tx.getHash().getBytes());
					} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
						//TODO 防伪码验证
						
					}
				} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER || 
						tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
					//帐户注册和修改账户信息
					CertAccountRegisterTransaction rtx = (CertAccountRegisterTransaction) tx;
					if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
						//删除之前的信息
						byte[] oldTxid = chainstateStoreProvider.getBytes(rtx.getHash160());
						chainstateStoreProvider.delete(oldTxid);
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
					
					//添加账户信息，如果不存在的话
					AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(rtx.getHash160());
					byte[][] pubkeys = new byte[][] {rtx.getMgPubkeys()[0], rtx.getMgPubkeys()[1], rtx.getTrPubkeys()[0], rtx.getTrPubkeys()[1]};
					if(accountInfo == null) {
						accountInfo = createNewAccountInfo(rtx, rtx.getBody(), pubkeys);
					} else {
						accountInfo.setAccountBody(rtx.getBody());
						accountInfo.setLastModifyTime(rtx.getTime());
						accountInfo.setInfoTxid(rtx.getHash());
						accountInfo.setPubkeys(pubkeys);
					}
					chainstateStoreProvider.saveAccountInfo(accountInfo);
				} else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {
					//普通防伪验证
					GeneralAntifakeTransaction generalAntifakeTransaction = (GeneralAntifakeTransaction) tx;
					
					byte[] antifakeHashBytes = generalAntifakeTransaction.getAntifakeHash().getBytes();
					
					chainstateStoreProvider.put(antifakeHashBytes, tx.getHash().getBytes());
				}
				//交易是否与我有关
				checkIsMineAndUpdate(txs);
			}
			
			//保存块头
			db.put(hash.getBytes(), blockStore.serializeHeaderToBytes());
			
			byte[] heightBytes = new byte[4]; 
			Utils.uint32ToByteArrayBE(block.getHeight(), heightBytes, 0);
			
			db.put(heightBytes, hash.getBytes());
			
			//更新最新区块
			db.put(bestBlockKey, hash.getBytes());
			
			//更新上一区块的指针
			if(!Sha256Hash.ZERO_HASH.equals(block.getPreHash())) {
				BlockHeaderStore preBlockHeader = getHeader(block.getPreHash().getBytes());
				preBlockHeader.setNextHash(block.getHash());
				db.put(preBlockHeader.getBlockHeader().getHash().getBytes(), preBlockHeader.baseSerialize());
			}
		} catch (IOException | VerificationException e) {
			log.info("保存区块出错：", e);
			throw e;
		} finally {
			blockLock.unlock();
		}
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
			
			Sha256Hash bestBlockHash = bestBlock.getHash();
			
			chainstateStoreProvider.put(bestBlock.getHash().getBytes(), bestBlock.baseSerialize());
			
			//回滚块信息
			db.delete(bestBlockHash.getBytes());
			
			byte[] heightBytes = new byte[4]; 
			Utils.uint32ToByteArrayBE(bestBlock.getHeight(), heightBytes, 0);
			
			db.delete(heightBytes);
			
			//更新最新区块
			db.put(bestBlockKey, bestBlock.getPreHash().getBytes());
			
			//更新上一区块的指针
			if(!Sha256Hash.ZERO_HASH.equals(bestBlock.getPreHash())) {
				BlockHeaderStore preBlockHeader = getHeader(bestBlock.getPreHash().getBytes());
				preBlockHeader.setNextHash(Sha256Hash.ZERO_HASH);
				db.put(preBlockHeader.getBlockHeader().getHash().getBytes(), preBlockHeader.baseSerialize());
			}
			
			//回滚交易
			//TODO
			for (int i = 0; i < bestBlock.getTxCount(); i++) {
				TransactionStore txs = new TransactionStore(network, bestBlock.getTxs().get(i), bestBlock.getHeight(), null);
		        
				Transaction tx = txs.getTransaction();
				
				db.delete(tx.getHash().getBytes());
				
				//把这些交易再次放回内存中
				MempoolContainer.getInstace().add(tx);
				
				if(tx.isPaymentTransaction()) {
					
					//转账交易
					//coinbase交易没有输入
					if(tx.getType() != Definition.TYPE_COINBASE) {
						List<Input> inputs = tx.getInputs();
						for (Input input : inputs) {
							TransactionInput tInput = (TransactionInput) input;
							TransactionOutput output = tInput.getFrom();
							if(output == null) {
								throw new VerificationException("error input");
							}
							//对上一交易的引用以及索引值
							Sha256Hash fromId = output.getParent().getHash();
							int index = output.getIndex();
							
							byte[] key = new byte[fromId.getBytes().length + 1];
							
							System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
							key[key.length - 1] = (byte) index;
							
							chainstateStoreProvider.put(key, new byte[]{ TransactionStore.STATUS_UNUSE});
						}
					}
					//添加输出
					List<Output> outputs = tx.getOutputs();
					for (Output output : outputs) {
						TransactionOutput tOutput = (TransactionOutput) output;
						
						Sha256Hash id = tx.getHash();
						int index = tOutput.getIndex();
						
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
							chainstateStoreProvider.delete(atx.getAntifakeHash().getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
						//TODO 防伪码验证
						
					}
				} else if(tx instanceof RegConsensusTransaction) {
					//注册共识，因为是回滚区块，需要从共识列表中删除
					//退出共识
					RegConsensusTransaction regTransaction = (RegConsensusTransaction)tx;
					byte[] hash160 = regTransaction.getHash160();
					
					//从共识账户列表中删除
					byte[] consensusAccountHash160s = chainstateStoreProvider.getBytes(Configure.CONSENSUS_ACCOUNT_KEYS);
					
					byte[] newConsensusHash160s = new byte[consensusAccountHash160s.length - Address.LENGTH];
					
					//找出位置在哪里
					//判断在列表里面才更新，否则就被清空了
					boolean hashExist = false;
					for (int j = 0; j < consensusAccountHash160s.length; j += Address.LENGTH) {
						byte[] addressHash160 = Arrays.copyOfRange(consensusAccountHash160s, j, j + Address.LENGTH);
						if(Arrays.equals(addressHash160, hash160)) {
							hashExist = true;
							System.arraycopy(consensusAccountHash160s, 0, newConsensusHash160s, 0, j);
							System.arraycopy(consensusAccountHash160s, j + Address.LENGTH, newConsensusHash160s, j, consensusAccountHash160s.length - j - Address.LENGTH);
						}
					}
					if(hashExist) {
						chainstateStoreProvider.put(Configure.CONSENSUS_ACCOUNT_KEYS, newConsensusHash160s);
					}
					//从共识缓存器里中移除
					consensusPool.delete(hash160);

					//乖节点遵守系统规则，被T则停止共识，否则就会被排除链外
					Account consensusAccount = consensusMeeting.getAccount();
					if(consensusAccount != null && Arrays.equals(consensusAccount.getAddress().getHash160(), hash160)) {
						//下一轮停止共识
						consensusMeeting.stopConsensusOnNextRound(bestBlock);
					}
				} else if(tx instanceof RemConsensusTransaction || tx instanceof ViolationTransaction) {
					//退出或者被踢出共识，这里需要再次加入
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
					
					//重新加入共识账户列表中
					byte[] consensusAccountHash160s = chainstateStoreProvider.getBytes(Configure.CONSENSUS_ACCOUNT_KEYS);
					if(consensusAccountHash160s == null) {
						consensusAccountHash160s = new byte[0];
					}
					byte[] newConsensusHash160s = new byte[consensusAccountHash160s.length + Address.LENGTH];
					System.arraycopy(consensusAccountHash160s, 0, newConsensusHash160s, 0, consensusAccountHash160s.length);
					System.arraycopy(hash160, 0, newConsensusHash160s, consensusAccountHash160s.length, Address.LENGTH);
					chainstateStoreProvider.put(Configure.CONSENSUS_ACCOUNT_KEYS, newConsensusHash160s);

					//公钥
					AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
					
					byte[][] pubkeys = accountInfo.getPubkeys();
					//添加到共识缓存器里
					consensusPool.add(hash160, pubkeys);
					//下一轮开始打包
//					consensusMeeting.startConsensusOnNextRound(bestBlock);
					
					//退出的账户
					if(tx instanceof ViolationTransaction) {
						//违规被提出共识，删除证据
						ViolationTransaction vtx = (ViolationTransaction)tx;
						ViolationEvidence violationEvidence = vtx.getViolationEvidence();
						Sha256Hash evidenceHash = violationEvidence.getEvidenceHash();
						chainstateStoreProvider.delete(evidenceHash.getBytes());
						
						//加上之前减去的信用值
						long certChange = 0;
						if(violationEvidence.getViolationType() == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
							certChange = Configure.CERT_CHANGE_TIME_OUT;
						}
						
						accountInfo.setCert(accountInfo.getCert() - certChange);
						
						chainstateStoreProvider.saveAccountInfo(accountInfo);
					}
				} else if(tx instanceof CreditTransaction) {
					//信用值的增加
					CreditTransaction creditTransaction = (CreditTransaction)tx;
					
					AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(creditTransaction.getOwnerHash160());
					//存在时，增加信用
					accountInfo.setCert(accountInfo.getCert() - creditTransaction.getCredit());
					chainstateStoreProvider.saveAccountInfo(accountInfo);
					
					creditCollectionService.removeCredit(creditTransaction.getReasonType(), creditTransaction.getOwnerHash160(), bestBlock.getTime());
				}
				
				//交易是否与我有关
				checkIsMineAndRevoked(txs);
			}
			return bestBlock;
		} finally {
			blockLock.unlock();
		}
	}

	/**
	 * 创建一个新的账户存储信息
	 * @param tx
	 * @param accountBody
	 * @param pubkeys
	 * @return AccountStore
	 */
	public AccountStore createNewAccountInfo(BaseCommonlyTransaction tx, AccountBody accountBody, byte[][] pubkeys) {
		AccountStore accountInfo;
		accountInfo = new AccountStore(network);
		accountInfo.setHash160(tx.getHash160());
		accountInfo.setType(tx.isSystemAccount() ? network.getSystemAccountVersion() : network.getCertAccountVersion());
		accountInfo.setCert(0);
		accountInfo.setAccountBody(accountBody);
		accountInfo.setBalance(Coin.ZERO.value);
		accountInfo.setCreateTime(tx.getTime());
		accountInfo.setLastModifyTime(tx.getTime());
		accountInfo.setInfoTxid(tx.getHash());
		accountInfo.setPubkeys(pubkeys);
		return accountInfo;
	}

	/**
	 * 不确定的账户，确定下来
	 * @param accountInfo
	 * @param tx
	 */
	public void updateAccountInfo(AccountStore accountInfo, BaseCommonlyTransaction tx) {
		if(accountInfo != null && accountInfo.getType() == 0) {
			accountInfo.setType(tx.isSystemAccount() ? network.getSystemAccountVersion() : network.getCertAccountVersion());
			
			if(tx.isCertAccount()) {
				accountInfo.setInfoTxid(tx.getHash());
				CertAccountRegisterTransaction rtx = (CertAccountRegisterTransaction) tx;
				byte[][] pubkeys = new byte[][] {rtx.getMgPubkeys()[0], rtx.getMgPubkeys()[1], rtx.getTrPubkeys()[0], rtx.getTrPubkeys()[1]};
				accountInfo.setPubkeys(pubkeys);
			} else {
				accountInfo.setPubkeys(new byte[][] { tx.getPubkey() });
			}
			chainstateStoreProvider.saveAccountInfo(accountInfo);
		}
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
	public void checkIsMineAndUpdate(TransactionStore txs) {
		Transaction transaction = txs.getTransaction();
		
		boolean isMine = checkTxIsMine(transaction);
		if(isMine) {
			updateMineTx(txs);
		}
	}

	/**
	 * 检查交易是否跟我有关
	 * @param transaction
	 * @return boolean
	 */
	public boolean checkTxIsMine(Transaction transaction) {
		//是否是跟自己有关的交易
		if(transaction.getType() == Definition.TYPE_PAY || 
				transaction.getType() == Definition.TYPE_COINBASE) {
			//普通交易
			//输入
			List<Input> inputs = transaction.getInputs();
			if(inputs != null && inputs.size() > 0) {
				for (Input input : inputs) {
					TransactionInput tInput = (TransactionInput) input;
					TransactionOutput output = tInput.getFrom();
					if(output == null) {
						continue;
					}
					//对上一交易的引用以及索引值
					Sha256Hash fromId = output.getParent().getHash();
					int index = output.getIndex();
					
					TransactionStore txStore = getTransaction(fromId.getBytes());
					if(txStore == null) {
						return false;
					}
					output = (TransactionOutput) txStore.getTransaction().getOutput(index);
					
					Script script = output.getScript();
					if(script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
						//
						return true;
					}
				}
			}
			//输出
			List<Output> outputs = transaction.getOutputs();
			for (Output output : outputs) {
				TransactionOutput tOutput = (TransactionOutput) output;
				
				Script script = tOutput.getScript();
				if(script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
					//如果是coinbase交易，那么交易费大于0的才显示出来
					if(transaction.getType() == Definition.TYPE_COINBASE) {
						if(tOutput.getValue() > 0) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
		} else if(transaction.getType() == Definition.TYPE_CREDIT ||
				transaction.getType() == Definition.TYPE_VIOLATION) {
			//特殊的交易，增加信用和违规处理，很大可能是别人发起的交易
			byte[] hash160 = null;
			if(transaction.getType() == Definition.TYPE_CREDIT) {
				CreditTransaction ctx = (CreditTransaction) transaction;
				hash160 = ctx.getOwnerHash160();
			} else if(transaction.getType() == Definition.TYPE_VIOLATION) {
				ViolationTransaction vtx = (ViolationTransaction) transaction;
				hash160 = vtx.getViolationEvidence().getAudienceHash160();
			}
			if(hash160 != null && accountFilter.contains(hash160)) {
				return true;
			}
		} else if(transaction instanceof BaseCommonlyTransaction) {
			BaseCommonlyTransaction commonlytx = (BaseCommonlyTransaction) transaction;
			if(accountFilter.contains(commonlytx.getHash160())) {
				return true;
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
		byte[] content = db.get(hash);
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
		
		byte[] hash = db.get(heightBytes);
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
			if(db == null) {
				return null;
			}
			bestBlockHash = db.get(bestBlockKey);
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
						List<Output> outputs = tx.getOutputs();
						
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
						List<Input> inputs = tx.getInputs();
						if(inputs != null) {
							for (Input input : inputs) {
								TransactionInput txInput = (TransactionInput) input;
								if(txInput.getFrom() == null) {
									continue;
								}
								TransactionOutput out = txInput.getFrom();
								Sha256Hash fromTxHash = txInput.getFrom().getParent().getHash();
								
								for (TransactionStore transactionStore : mineTxs) {
									Transaction mineTx = transactionStore.getTransaction();
									if(mineTx.getHash().equals(fromTxHash)) {
										//对上一交易的引用以及索引值
										TransactionOutput output = (TransactionOutput) mineTx.getOutput(out.getIndex());
										Script script = output.getScript();
										if(script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
											transactionStore.getStatus()[txInput.getFrom().getIndex()] = TransactionStore.STATUS_USED;
											isMineTx = true;
											break;
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

//	@PostConstruct
//	public void test() {
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		//从创始快开始遍历所有区块
//		BlockStore blockStore = network.getGengsisBlock();
//		Sha256Hash nextHash = blockStore.getBlock().getHash();
//		
//		while(!nextHash.equals(Sha256Hash.ZERO_HASH)) {
//			BlockStore nextBlockStore = getBlock(nextHash.getBytes());
//			
//			List<Transaction> txs = nextBlockStore.getBlock().getTxs();
//			if(txs.size() > 2) {
//				log.info("============== block tx count is {}, size is {} bytes", txs.size(), nextBlockStore.getBlock().baseSerialize().length);
//			}
//			if(nextBlockStore.getBlock().getHeight() == 356l) {
//				log.info("========= {}", nextBlockStore.getBlock());
//			}
//			nextHash = nextBlockStore.getNextHash();
//		}
//	}
}
