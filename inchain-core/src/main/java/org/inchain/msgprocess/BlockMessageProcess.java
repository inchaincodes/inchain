package org.inchain.msgprocess;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Account.AccountType;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Peer;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.BlockMessage;
import org.inchain.message.Message;
import org.inchain.network.NetworkParameters;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.RegisterTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class BlockMessageProcess implements MessageProcess {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private BlockStoreProvider blockStoreProvider;
	private TransactionStoreProvider transactionStoreProvider;
	private ChainstateStoreProvider chainstateStoreProvider;
	
	public BlockMessageProcess(NetworkParameters network) {
		blockStoreProvider = BlockStoreProvider.getInstace(Configure.DATA_BLOCK, network);
		transactionStoreProvider = TransactionStoreProvider.getInstace(Configure.DATA_TRANSACTION, network);
		chainstateStoreProvider = ChainstateStoreProvider.getInstace(Configure.DATA_CHAINSTATE, network);
	}
	
	/**
	 * 接收到区块消息，进行区块合法性验证，如果验证通过，则收录，然后转发区块
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("new block : {}", message);
		}
		
		BlockMessage blockMessage = (BlockMessage) message;
		
		//验证区块消息的合法性
		verifyBlock(blockMessage);
		
		//验证通过 ，存储区块数据
		try {
			blockStoreProvider.saveBlock(blockMessage.getBlockStore());
			//更新区块状态区数据
			updateState(blockMessage.getBlockStore());
			
			//更新与自己相关的交易
			
		} catch (IOException e) {
			throw new VerificationException(e);
		}
		
		return null;
	}

	//更新区块状态区数据
	private void updateState(BlockStore blockStore) {
		List<TransactionStore> txs = blockStore.getTxs();
		for (TransactionStore txst : txs) {
			//从状态区移出本次的所有输入，并加入本次的所有输出
			Transaction tx = txst.getTransaction();
			if(tx.getType() == Transaction.TYPE_PAY || 
					tx.getType() == Transaction.TYPE_COINBASE) {
				//coinbase交易没有输入
				if(tx.getType() == Transaction.TYPE_PAY) {
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
			} else if(tx.getType() == Transaction.TYPE_REGISTER || tx.getType() == Transaction.TYPE_CHANGEPWD) {
				//帐户注册和修改密码
				RegisterTransaction rtx = (RegisterTransaction) tx;
				
				chainstateStoreProvider.put(rtx.getAccount().getAddress().getHash160(), rtx.getHash().getBytes());
			}
		}
	}

	/*
	 * 验证区块的合法性，如果验证不通过，则抛出验证异常
	 */
	private void verifyBlock(BlockMessage blockMessage) {
		//获取区块的最新高度
		BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
		//必需衔接
		BlockStore blockStore = blockMessage.getBlockStore();
		if(!blockStore.getPreHash().equals(bestBlockHeader.getHash()) ||
				blockStore.getHeight() != bestBlockHeader.getHeight() + 1) {
			throw new VerificationException("block info error");
		}
		//验证区块签名
		//TODO
		
		//验证梅克尔树根是否正确
		if(!blockStore.buildMerkleHash().equals(blockStore.getMerkleHash())) {
			throw new VerificationException("block merkle hash error");
		}
		
		//验证交易是否合法
		Coin coinbaseFee = Coin.ZERO; //coinbase 交易包含的金额，主要是手续费
		Coin fee = Coin.ZERO; 		  //手续费
		Coin inputFee = Coin.ZERO;
		Coin outputFee = Coin.ZERO;
		
		//每个区块只能包含一个coinbase交易，并且只能是第一个
		boolean coinbase = false;
		
		List<TransactionStore> txs = blockStore.getTxs();
		for (TransactionStore txst : txs) {
			
			Transaction tx = txst.getTransaction();

			//验证交易的合法性
			tx.verfify();
			tx.verfifyScript();
			
			//交易的txid不能和区块里面的交易重复
			TransactionStore verifyTX = blockStoreProvider.getTransaction(tx.getHash().getBytes());
			if(verifyTX != null) {
				throw new VerificationException("the block hash repeat tx id");
			}
			
			//区块的第一个交易必然是coinbase交易，除第一个之外的任何交易都不应是coinbase交易，否则出错
			if(!coinbase) {
				if(tx.getType() != Transaction.TYPE_COINBASE) {
					throw new VerificationException("the block first tx is not coinbase tx");
				}
				coinbaseFee = Coin.valueOf(((TransactionOutput)tx.getOutput(0)).getValue());
				coinbase = true;
				continue;
			} else if(tx.getType() == Transaction.TYPE_COINBASE) {
				throw new VerificationException("the block too much coinbase tx");
			}
			//如果是转帐交易
			if(tx.getType() == Transaction.TYPE_PAY) {
				//验证交易的输入来源，是否已花费的交易，同时验证金额
				Coin txInputFee = Coin.ZERO;
				Coin txOutputFee = Coin.ZERO;
				
				//验证本次交易的输入
				List<Input> inputs = tx.getInputs();
				for (Input input : inputs) {
					TransactionInput tInput = (TransactionInput) input;
					TransactionOutput output = tInput.getFrom();
					if(output == null) {
						throw new VerificationException("tx error input");
					}
					//对上一交易的引用以及索引值
					Sha256Hash fromId = output.getParent().getHash();
					int index = output.getIndex();
					
					byte[] key = new byte[fromId.getBytes().length + 1];
					
					System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
					key[key.length - 1] = (byte) index;
					
					//判断是否未花费
					byte[] state = chainstateStoreProvider.getBytes(key);
					if(!Arrays.equals(state, new byte[]{1})) {
						throw new VerificationException("tx input verfify fail");
					}
					//查询上次的交易
					TransactionStore preTransaction = blockStoreProvider.getTransaction(fromId.getBytes());
					TransactionOutput perOutput = (TransactionOutput) preTransaction.getTransaction().getOutput(index);
					
					txInputFee = txInputFee.add(Coin.valueOf(perOutput.getValue()));
				}
				//验证本次交易的输出
				List<Output> outputs = tx.getOutputs();
				for (Output output : outputs) {
					TransactionOutput tOutput = (TransactionOutput) output;
					Coin outputCoin = Coin.valueOf(tOutput.getValue());
					//输出金额不能为负数
					if(outputCoin.isLessThan(Coin.ZERO)) {
						throw new VerificationException("tx output fee less than zero");
					}
					txOutputFee = txOutputFee.add(outputCoin);
					//FIXME 是否验证必须输出到已有的帐户 ???
				}
				//输出金额不能大于输入金额
				if(txOutputFee.isGreaterThan(txInputFee)) {
					throw new VerificationException("tx output fee greater than input fee");
				}
				inputFee = inputFee.add(txInputFee);
				outputFee = outputFee.add(txOutputFee);
				fee = fee.add(txInputFee.subtract(txOutputFee));
				
			} else if(tx.getType() == Transaction.TYPE_REGISTER) {
				//帐户注册
				RegisterTransaction regTx = (RegisterTransaction) tx;
				Address address = regTx.getAccount().getAddress();
				//注册的hash160地址，不能与现有的地址重复，当然正常情况重复的机率为0，不排除有人恶意广播数据
				byte[] hash160 = address.getHash160();
				
				byte[] txid = chainstateStoreProvider.getBytes(hash160);
				if(txid != null) {
					throw new VerificationException("the register txid hash160 hash exists");
				}
				
				//如果是普通帐户，任何人都可以注册，认证帐户，就需要判断是否经过认证的
				if(regTx.getAccount().getAccountType() == AccountType.CERT) {
					//TODO
					throw new VerificationException("the register has not cert");
				}
			}
		}
		//验证金额，coinbase交易的费用必须等于交易手续费
		if(!fee.equals(inputFee.subtract(outputFee)) || !coinbaseFee.equals(fee)) {
			throw new VerificationException("the fee error");
		}
	}
}
