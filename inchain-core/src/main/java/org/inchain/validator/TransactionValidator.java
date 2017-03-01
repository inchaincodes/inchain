package org.inchain.validator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.inchain.Configure;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.store.AccountStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.transaction.business.RegConsensusTransaction;
import org.inchain.transaction.business.RemConsensusTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 交易验证器
 * @author ln
 *
 */
@Component
public class TransactionValidator {
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private ConsensusPool consensusPool;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;

	/**
	 * 交易验证器，验证交易的输入输出是否合法
	 * @param tx	待验证的交易
	 * @return ValidatorResult<TransactionValidatorResult>
	 */
	public ValidatorResult<TransactionValidatorResult> valDo(Transaction tx) {
		return valDo(tx, null);
	}
	
	/**
	 * 交易验证器，验证交易的输入输出是否合法
	 * @param tx	待验证的交易
	 * @param txs	当输入引用找不到时，就在这个列表里面查找（当同一个区块包含多个交易链时需要用到）
	 * @return ValidatorResult<TransactionValidatorResult>
	 */
	public ValidatorResult<TransactionValidatorResult> valDo(Transaction tx, List<Transaction> txs) {
		
		final TransactionValidatorResult result = new TransactionValidatorResult();
		ValidatorResult<TransactionValidatorResult> validatorResult = new ValidatorResult<TransactionValidatorResult>() {
			@Override
			public TransactionValidatorResult getResult() {
				return result;
			}
		};
		
		//验证交易的合法性
		tx.verfifyScript();
		
		//交易的txid不能和区块里面的交易重复
		TransactionStore verifyTX = blockStoreProvider.getTransaction(tx.getHash().getBytes());
		if(verifyTX != null) {
			result.setResult(false, "交易hash与区块里的重复");
			return validatorResult;
		}
		//如果是转帐交易
		if(tx.isPaymentTransaction() && tx.getType() != Definition.TYPE_COINBASE) {
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
				
				//查询上次的交易
				Transaction preTransaction = null;
				
				//判断是否未花费
				byte[] state = chainstateStoreProvider.getBytes(key);
				if(!Arrays.equals(state, new byte[]{1})) {
					//查询内存池里是否有该交易
					preTransaction = MempoolContainer.getInstace().get(fromId);
					if(preTransaction == null) {
						//区块链和内存池里面都没有，那么是否在传入的列表里面
						boolean exist = false;
						if(txs != null && txs.size() > 0) {
							for (Transaction transaction : txs) {
								if(transaction.getHash().equals(fromId)) {
									exist = true;
									preTransaction = transaction;
									break;
								}
							}
						}
						if(!exist) {
							result.setResult(false, TransactionValidatorResult.ERROR_CODE_USED, "引用了不可用的交易");
							return validatorResult;
						}
					}
				} else {
					//查询上次的交易
					preTransaction = blockStoreProvider.getTransaction(fromId.getBytes()).getTransaction();
					if(preTransaction == null) {
						result.setResult(false, "引用了不存在的交易");
						return validatorResult;
					}
				}
				TransactionOutput preOutput = (TransactionOutput) preTransaction.getOutput(index);
				txInputFee = txInputFee.add(Coin.valueOf(preOutput.getValue()));
				output.setValue(preOutput.getValue());
			}
			//验证本次交易的输出
			List<Output> outputs = tx.getOutputs();
			for (Output output : outputs) {
				TransactionOutput tOutput = (TransactionOutput) output;
				Coin outputCoin = Coin.valueOf(tOutput.getValue());
				//输出金额不能为负数
				if(outputCoin.isLessThan(Coin.ZERO)) {
					result.setResult(false, "输出金额不能为负数");
					return validatorResult;
				}
				txOutputFee = txOutputFee.add(outputCoin);
				//TODO 是否验证必须输出到已有的帐户 ???
			}
			//输出金额不能大于输入金额
			if(txOutputFee.isGreaterThan(txInputFee)) {
				result.setResult(false, "输出金额不能大于输入金额");
				return validatorResult;
			} else {
				result.setFee(txInputFee.subtract(txOutputFee));
			}
			//业务交易且带代币交易
			if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				//如果是验证码生成交易，则验证产品是否存在
				AntifakeCodeMakeTransaction atx = (AntifakeCodeMakeTransaction) tx;
				TransactionStore txStore = blockStoreProvider.getTransaction(atx.getProductTx().getBytes());
				if(txStore == null || txStore.getTransaction() == null) {
					result.setResult(false, "产品不存在");
					return validatorResult;
				}
				ProductTransaction ptx = (ProductTransaction) txStore.getTransaction();
				if(!Arrays.equals(ptx.getHash160(), atx.getHash160())) {
					result.setResult(false, "不合法的产品引用");
					return validatorResult;
				}
				//防伪码不能重复
				try {
					byte[] txid = chainstateStoreProvider.getBytes(atx.getAntifakeHash().getBytes());
					if(txid != null) {
						result.setResult(false, "重复的防伪码");
						return validatorResult;
					}
				} catch (IOException e) {
					result.setResult(false, "验证防伪码是否重复时出错，错误信息：" + e.getMessage());
					return validatorResult;
				}
			} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
				//防伪码验证交易
				AntifakeCodeVerifyTransaction acvtx = (AntifakeCodeVerifyTransaction) tx;
				
				byte[] antifakeCodeVerifyMakeTxHash = chainstateStoreProvider.getBytes(acvtx.getAntifakeCode().getBytes());
				if(antifakeCodeVerifyMakeTxHash == null) {
					result.setResult(false, "防伪码不存在");
					return validatorResult;
				}
				
				TransactionStore txStore = blockStoreProvider.getTransaction(antifakeCodeVerifyMakeTxHash);
				if(txStore == null || txStore.getTransaction() == null) {
					result.setResult(false, "防伪码生成交易不存在");
					return validatorResult;
				}
				Transaction antifakeCodeVerifyMakeTx = txStore.getTransaction();
				if(antifakeCodeVerifyMakeTx.getType() != Definition.TYPE_ANTIFAKE_CODE_MAKE) {
					result.setResult(false, "错误的防伪码");
					return validatorResult;
				}
				//保证该防伪码没有被验证
				byte[] txStatus = antifakeCodeVerifyMakeTx.getHash().getBytes();
				byte[] txIndex = new byte[txStatus.length + 1];
				
				System.arraycopy(txStatus, 0, txIndex, 0, txStatus.length);
				txIndex[txIndex.length - 1] = 0;
				
				byte[] status = chainstateStoreProvider.getBytes(txIndex);
				if(status == null) {
					result.setResult(false, "防伪码已被验证");
					return validatorResult;
				}
			}
		} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER) {
			//帐户注册
			CertAccountRegisterTransaction regTx = (CertAccountRegisterTransaction) tx;
			//注册的hash160地址，不能与现有的地址重复，当然正常情况重复的机率为0，不排除有人恶意广播数据
			byte[] hash160 = regTx.getHash160();
			
			byte[] txid = chainstateStoreProvider.getBytes(hash160);
			if(txid != null) {
				result.setResult(false, "注册的账户重复");
				return validatorResult;
			}
			
			//验证账户注册，必须是超级账号签名的才能注册
			byte[] verTxid = regTx.getScript().getChunks().get(1).data;
			byte[] verTxBytes = chainstateStoreProvider.getBytes(verTxid);
			if(verTxBytes == null) {
				result.setResult(false, "签名错误");
				return validatorResult;
			}
			CertAccountRegisterTransaction verTx = new CertAccountRegisterTransaction(network, verTxBytes);
			
			//认证帐户，就需要判断是否经过认证的
			if(!Arrays.equals(verTx.getHash160(), network.getCertAccountManagerHash160())) {
				result.setResult(false, "账户没有经过认证");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_REG_CONSENSUS) {
			//申请成为共识节点
			RegConsensusTransaction regConsensusTx = (RegConsensusTransaction) tx;
			byte[] hash160 = regConsensusTx.getHash160();
			//获取申请人信息，包括信用和可用余额
			AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountStore == null && regConsensusTx.isCertAccount()) {
				//TODO 需要信用才能注册的，这里不应该做任何处理
				//加入内存池  临时的处理方案
				result.setResult(false, "账户不存在");
				return validatorResult;
			}
			
			//判断是否达到共识条件
			long credit = (accountStore == null ? 0 : accountStore.getCert());
			if(credit < Configure.CONSENSUS_CREDIT) {
				//信用不够
				result.setResult(false, "信用值过低");
				return validatorResult;
			}
			
			//判断是否已经是共识节点
			if(consensusPool.contains(hash160)) {
				//已经是共识节点了
				result.setResult(false, "已经是共识节点了,勿重复申请");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_REM_CONSENSUS) {
			//退出共识交易
			RemConsensusTransaction remConsensusTx = (RemConsensusTransaction) tx;
			byte[] hash160 = remConsensusTx.getHash160();
			//判断是否已经是共识节点
			if(!consensusPool.contains(hash160)) {
				//不是共识节点，该交易不合法
				result.setResult(false, "不是共识节点了，该交易不合法");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {
			//普通防伪码验证
			//仅两种情况需要验证
			//1 当商品是区块里存在的，那么验证商家是否合法
			GeneralAntifakeTransaction gatx = (GeneralAntifakeTransaction) tx;
			if(gatx.getProductTx() != null) {
				TransactionStore ts = blockStoreProvider.getTransaction(gatx.getProductTx().getBytes());
				if(ts == null) {
					result.setResult(false, "关联的商品不存在");
					return validatorResult;
				}
				ProductTransaction productTx = (ProductTransaction) ts.getTransaction();
				if(!Arrays.equals(productTx.getHash160(), gatx.getSignVerificationScript().getAccountHash160())) {
					result.setResult(false, "不合法的商品使用");
					return validatorResult;
				}
			}
			
			//2验证防伪码是否被验证过了
			try {
				byte[] txhash = chainstateStoreProvider.getBytes(gatx.getAntifakeHash().getBytes());
				if(txhash != null) {
					result.setResult(false, "重复的验证");
					return validatorResult;
				}
			} catch (IOException e) {
				result.setResult(false, "验证出错，错误信息："+e.getMessage());
				return validatorResult;
			}
		}

		result.setSuccess(true);
		result.setMessage("ok");
		return validatorResult;
	}
}
