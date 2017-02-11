package org.inchain.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.filter.BloomFilter;
import org.inchain.listener.TransactionListener;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.script.Script;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.CertAccountTransaction;
import org.inchain.transaction.CreditTransaction;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.utils.Hex;
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
	private final static byte[] bestBlockKey = Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")).getBytes();
	//账户过滤器，用于判断交易是否与我有关
	private BloomFilter accountFilter = new BloomFilter(100000, 0.0001, RandomUtil.randomLong());;
	//区块状态提供器
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	//共识缓存器
	@Autowired
	private ConsensusPool consensusPool;

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
	public void saveBlock(BlockStore blockStore) throws IOException {
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
			//保存块头
			db.put(hash.getBytes(), blockStore.serializeHeaderToBytes());
			//保存交易
			for (int i = 0; i < block.getTxCount(); i++) {
				TransactionStore txs = new TransactionStore(network, block.getTxs().get(i), block.getHeight(), null);
		        
				Transaction tx = txs.getTransaction();
				
				db.put(tx.getHash().getBytes(), txs.baseSerialize());
				
				//如果是共识注册交易，则保存至区块状态表
				if(tx instanceof RegConsensusTransaction) {
					RegConsensusTransaction regTransaction = (RegConsensusTransaction)tx;
					
					byte[] uinfos = chainstateStoreProvider.getBytes(regTransaction.getHash160());
					
					if(uinfos == null) {
						throw new VerificationException("没有信用数据，不允许注册共识");
					}
					//4信用，4余额，33公钥，1共识状态
					byte[] values = new byte[42];
					System.arraycopy(uinfos, 0, values, 0, uinfos.length);
					values[41] = 1;
					//公钥
					byte[] pubkey = regTransaction.getScriptSig().getChunks().get(0).data;
					System.arraycopy(pubkey, 0, values, 8, pubkey.length);
					
					chainstateStoreProvider.put(regTransaction.getHash160(), values);
					//添加到共识缓存器里
					consensusPool.add(regTransaction.getHash160(), pubkey);
				} else if(tx instanceof CreditTransaction) {
					//只有创世块支持该类型交易
					if(bestBlockHeader == null && Arrays.equals(bestBlockKey, block.getPreHash().getBytes()) && block.getHeight() == 0l) {
						CreditTransaction creditTransaction = (CreditTransaction)tx;
						
						byte[] uinfos = chainstateStoreProvider.getBytes(creditTransaction.getHash160());
						if(uinfos == null) {
							//不存在时，直接写入信用
							byte[] value = new byte[4];
							Utils.uint32ToByteArrayBE(creditTransaction.getCredit(), value, 0);
							chainstateStoreProvider.put(creditTransaction.getHash160(), value);
						} else {
							//存在时，增加信用
							if(uinfos.length < 4) {
								throw new VerificationException("错误的信用数据");
							}
							long credit = Utils.readUint32BE(uinfos, 0);
							credit += creditTransaction.getCredit();
							Utils.uint32ToByteArrayBE(credit, uinfos, 0);

							chainstateStoreProvider.put(creditTransaction.getHash160(), uinfos);
						}
					} else {
						throw new VerificationException("出现不支持的交易，保存失败");
					}
				} else if(tx.getType() == TransactionDefinition.TYPE_PAY || 
						tx.getType() == TransactionDefinition.TYPE_COINBASE) {
					//普通交易
					//coinbase交易没有输入
					if(tx.getType() == TransactionDefinition.TYPE_PAY) {
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
						
						chainstateStoreProvider.put(key, new byte[]{1});
					}
				} else if(tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER || 
						tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_UPDATE) {
					//帐户注册和修改密码
					CertAccountRegisterTransaction rtx = (CertAccountRegisterTransaction) tx;
					if(tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_UPDATE) {
						//删除之前的信息
						byte[] oldTxid = chainstateStoreProvider.getBytes(rtx.getHash160());
						chainstateStoreProvider.delete(oldTxid);
					}
					chainstateStoreProvider.put(rtx.getHash().getBytes(), rtx.baseSerialize());
					chainstateStoreProvider.put(rtx.getHash160(), rtx.getHash().getBytes());
				}
				//交易是否与我有关
				checkIsMineAndUpdate(txs);
			}
			
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
		} finally {
			blockLock.unlock();
		}
	}

	/**
	 * 检查交易是否与我有关，并且更新状态
	 * @param txs
	 */
	public void checkIsMineAndUpdate(TransactionStore txs) {
		Transaction transaction = txs.getTransaction();
		
		//是否是跟自己有关的交易
		if(transaction.getType() == TransactionDefinition.TYPE_PAY || 
				transaction.getType() == TransactionDefinition.TYPE_COINBASE) {
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
					output = (TransactionOutput) getTransaction(fromId.getBytes()).getTransaction().getOutput(index);
					
					Script script = output.getScript();
					if(script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
						//
						updateMineTx(txs);
						return;
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
					if(transaction.getType() == TransactionDefinition.TYPE_COINBASE) {
						if(tOutput.getValue() > 0) {
							updateMineTx(txs);
							return;
						}
					} else {
						updateMineTx(txs);
						return;
					}
				}
			}
		} else if(transaction instanceof CertAccountTransaction) {
			//账户注册交易
			CertAccountTransaction certTx = (CertAccountTransaction) transaction;
			if(accountFilter.contains(certTx.getHash160())) {
				updateMineTx(txs);
			}
		}
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
					if(tx.getType() == TransactionDefinition.TYPE_COINBASE ||
							tx.getType() == TransactionDefinition.TYPE_PAY) {
						//获取转入交易转入的多少钱
						List<Output> outputs = tx.getOutputs();
						
						if(outputs == null) {
							continue;
						}
						//交易状态
						byte[] status = new byte[outputs.size()];
						//交易是否跟我有关
						boolean isMineTx = false;
						
						for (int i = 0; i < outputs.size(); i++) {
							Output output = outputs.get(i);
							Script script = output.getScript();
							
							for (byte[] hash160 : hash160s) {
								if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
									status[i] = TransactionStore.STATUS_UNUSE;
									isMineTx = true;
									break;
								}
							}
						}
						List<Input> inputs = tx.getInputs();
						if(inputs != null) {
							for (Input input : inputs) {
								TransactionInput txInput = (TransactionInput) input;
								if(txInput.getFrom() == null) {
									continue;
								}
								Sha256Hash fromTxHash = txInput.getFrom().getParent().getHash();
								
								for (TransactionStore transactionStore : mineTxs) {
									if(transactionStore.getTransaction().getHash().equals(fromTxHash)) {
										transactionStore.getStatus()[txInput.getFrom().getIndex()] = TransactionStore.STATUS_USED;
										isMineTx = true;
										break;
									}
								}
							}
						}
						
						if(isMineTx) {
							mineTxs.add(new TransactionStore(network, tx, block.getHeight(), status));
						}
					} else if(tx.getType() == TransactionDefinition.TYPE_REG_CONSENSUS) {
						//参与共识交易
						
					} else if(tx instanceof CertAccountTransaction) {
						//认证账户类交易
						CertAccountTransaction certTx = (CertAccountTransaction) tx;
						for (byte[] hash160 : hash160s) {
							if(Arrays.equals(certTx.getHash160(), hash160)) {
								mineTxs.add(new TransactionStore(network, tx, block.getHeight(), new byte[]{}));
								break;
							}
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
}
