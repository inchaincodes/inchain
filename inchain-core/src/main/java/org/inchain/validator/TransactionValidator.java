package org.inchain.validator;

import java.util.Arrays;
import java.util.List;

import org.inchain.core.Coin;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.network.NetworkParams;
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
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;

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
			result.setSuccess(false);
			result.setMessage("交易hash与区块里的重复");
			return validatorResult;
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
					preTransaction = MempoolContainerMap.getInstace().get(fromId);
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
							result.setSuccess(false);
							result.setMessage("引用了不可用的交易");
							return validatorResult;
						}
					}
				} else {
					//查询上次的交易
					preTransaction = blockStoreProvider.getTransaction(fromId.getBytes()).getTransaction();
					if(preTransaction == null) {
						result.setSuccess(false);
						result.setMessage("引用了不存在的交易");
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
					result.setSuccess(false);
					result.setMessage("输出金额不能为负数");
					return validatorResult;
				}
				txOutputFee = txOutputFee.add(outputCoin);
				//FIXME 是否验证必须输出到已有的帐户 ???
			}
			//输出金额不能大于输入金额
			if(txOutputFee.isGreaterThan(txInputFee)) {
				result.setSuccess(false);
				result.setMessage("输出金额不能大于输入金额");
				return validatorResult;
			} else {
				result.setFee(txInputFee.subtract(txOutputFee));
			}
		} else if(tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER) {
			//帐户注册
			CertAccountRegisterTransaction regTx = (CertAccountRegisterTransaction) tx;
			//注册的hash160地址，不能与现有的地址重复，当然正常情况重复的机率为0，不排除有人恶意广播数据
			byte[] hash160 = regTx.getHash160();
			
			byte[] txid = chainstateStoreProvider.getBytes(hash160);
			if(txid != null) {
				result.setSuccess(false);
				result.setMessage("the register txid hash160 hash exists");
				return validatorResult;
			}
			
			//如果是普通帐户，任何人都可以注册，认证帐户，就需要判断是否经过认证的
			if(network.getCertAccountVersion() == network.getCertAccountVersion()) {
				//TODO
				result.setSuccess(false);
				result.setMessage("the register has not cert");
				return validatorResult;
			}
		}

		result.setSuccess(true);
		result.setMessage("ok");
		return validatorResult;
	}
}
