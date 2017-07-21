package org.inchain.validator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusAccount;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.*;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.AccountStore;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.*;
import org.inchain.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 交易验证器
 * @author ln
 *
 */
@Component
public class TransactionValidator {

	private final static Logger log = LoggerFactory.getLogger(org.inchain.validator.TransactionValidator.class);

	@Autowired
	private NetworkParams network;
	@Autowired
	private ConsensusPool consensusPool;
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	@Autowired
	private AccountKit accountKit;
	@Autowired
	private DataSynchronizeHandler dataSynchronizeHandler;

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

		tx.verify();
		//验证交易的合法性
		if(tx instanceof BaseCommonlyTransaction) {
			((BaseCommonlyTransaction)tx).verifyScript();
		}

		//交易的txid不能和区块里面的交易重复
		TransactionStore verifyTX = blockStoreProvider.getTransaction(tx.getHash().getBytes());
		if(verifyTX != null) {

			result.setResult(false, TransactionValidatorResult.ERROR_CODE_EXIST, "交易hash与区块里的重复 " + tx.getHash());
			return validatorResult;
		}
		//如果是转帐交易
		//TODO 以下代码请使用状态模式重构
		if(tx.isPaymentTransaction() && tx.getType() != Definition.TYPE_COINBASE) {
			//验证交易的输入来源，是否已花费的交易，同时验证金额
			Coin txInputFee = Coin.ZERO;
			Coin txOutputFee = Coin.ZERO;

			//验证本次交易的输入
			List<TransactionInput> inputs = tx.getInputs();
			//交易引用的输入，赎回脚本必须一致
			byte[] scriptBytes = null;
			for (TransactionInput input : inputs) {
				List<TransactionOutput> outputs = input.getFroms();
				if(outputs == null || outputs.size() == 0) {
					throw new VerificationException("交易没有引用输入");
				}
				for (TransactionOutput output : outputs) {
					//对上一交易的引用以及索引值
					Transaction fromTx = output.getParent();
					if(fromTx == null) {
						throw new VerificationException("交易没有正确的输入引用");
					}
					Sha256Hash fromId = fromTx.getHash();
					int index = output.getIndex();

					//如果引用已经是完整的交易，则不查询
					if(fromTx.getOutputs() == null || fromTx.getOutputs().isEmpty()) {
						//需要设置引用的完整交易
						//查询内存池里是否有该交易
						Transaction preTransaction = MempoolContainer.getInstace().get(fromId);
						//内存池里面没有，那么是否在传入的列表里面
						if(preTransaction == null && txs != null && txs.size() > 0) {
							for (Transaction transaction : txs) {
								if(transaction.getHash().equals(fromId)) {
									preTransaction = transaction;
									break;
								}
							}
						}
						if(preTransaction == null) {
							//内存池和传入的列表都没有，那么去存储里面找
							TransactionStore preTransactionStore = blockStoreProvider.getTransaction(fromId.getBytes());
							if(preTransactionStore == null) {
								result.setResult(false, TransactionValidatorResult.ERROR_CODE_NOT_FOUND, "引用了不存在的交易");
								return validatorResult;
							}
							preTransaction = preTransactionStore.getTransaction();
						}
						output.setParent(preTransaction);
						output.setScript(preTransaction.getOutput(index).getScript());
						fromTx = preTransaction;
					}

					//验证引用的交易是否可用
					if(fromTx.getLockTime() < 0l ||
							(fromTx.getLockTime() > Definition.LOCKTIME_THRESHOLD && fromTx.getLockTime() > TimeService.currentTimeSeconds())
							|| (fromTx.getLockTime() < Definition.LOCKTIME_THRESHOLD && fromTx.getLockTime() > network.getBestHeight())) {
						throw new VerificationException("引用了不可用的交易");
					}
					//验证引用的交易输出是否可用
					long lockTime = output.getLockTime();
					if(lockTime < 0l || (lockTime > Definition.LOCKTIME_THRESHOLD && lockTime > TimeService.currentTimeSeconds())
							|| (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > network.getBestHeight())) {
						throw new VerificationException("引用了不可用的交易输出");
					}

					TransactionOutput preOutput = fromTx.getOutput(index);
					txInputFee = txInputFee.add(Coin.valueOf(preOutput.getValue()));
					output.setValue(preOutput.getValue());
					//验证交易赎回脚本必须一致
					if(scriptBytes == null) {
						scriptBytes = preOutput.getScriptBytes();
					} else if(!Arrays.equals(scriptBytes, preOutput.getScriptBytes())) {
						throw new VerificationException("错误的输入格式，不同的交易赎回脚本不能合并");
					}

					//验证交易不能双花
					byte[] statusKey = output.getKey();
					byte[] state = chainstateStoreProvider.getBytes(statusKey);
					if((state == null || Arrays.equals(state, new byte[]{ 1 })) && txs != null && !txs.isEmpty()) {
						//没有状态，则可能是在 txs 里，txs里面不能有2笔对此的引用，否则就造成了双花
//						int count = 0;
//						for (Transaction t : txs) {
//							if(t.getHash().equals(tx.getHash())) {
//								continue;
//							}
//							List<TransactionInput> inputsTemp = t.getInputs();
//							if(inputsTemp == null || inputsTemp.size() == 0) {
//								continue;
//							}
//							for (TransactionInput in : t.getInputs()) {
//								List<TransactionOutput> fromsTemp = in.getFroms();
//								if(fromsTemp == null || fromsTemp.size() == 0) {
//									break;
//								}
//								for (TransactionOutput fromTemp : fromsTemp) {
//									if(fromTemp.getParent() != null && fromTemp.getParent().getHash().equals(fromId) && fromTemp.getIndex() == index) {
//										count++;
//									}
//								}
//							}
//						}
//						if(count > 1) {
//							//双花了
//							result.setResult(false, TransactionValidatorResult.ERROR_CODE_USED, "同一块多个交易引用了同一个输入");
//							return validatorResult;
//						}
					} else if(Arrays.equals(state, new byte[]{2})) {
						//已经花费了
						result.setResult(false, TransactionValidatorResult.ERROR_CODE_USED, "引用了已花费的交易");
						return validatorResult;
					}
				}
				Script verifyScript = new Script(scriptBytes);
				if(verifyScript.isConsensusOutputScript()) {
					//共识保证金引用脚本，则验证
					//因为共识保证金，除了本人会操作，还会有其它共识人操作
					//并且不一定是转到自己的账户，所以必须对输入输出都做严格的规范
					if(!(tx.getType() == Definition.TYPE_REM_CONSENSUS || tx.getType() == Definition.TYPE_VIOLATION)) {
						throw new VerificationException("不合法的交易引用");
					}
					//输入必须只有一个
					if(inputs.size() != 1 || inputs.get(0).getFroms().size() != 1) {
						result.setResult(false, "该笔交易有保证金的引用，输入个数不对");
						return validatorResult;
					}
					//输出必须只有一个，切必须按照指定的类型输出到相应的账户
					if(tx.getOutputs().size() != 1) {
						result.setResult(false, "该笔交易有保证金的引用，输出个数不对");
						return validatorResult;
					}
					TransactionOutput ouput = tx.getOutputs().get(0);
					//验证保证金的数量
					if(ouput.getValue() != inputs.get(0).getFroms().get(0).getValue()) {
						result.setResult(false, "保证金的输入输出金额不匹配");
						return validatorResult;
					}
					Script outputScript = ouput.getScript();
					//必须输出到地址
					if(!outputScript.isSentToAddress()) {
						result.setResult(false, "保证金的输出不正确");
						return validatorResult;
					}
					//必须输出到指定的账户
					//自己的账户
					byte[] selfAccount = verifyScript.getChunks().get(0).data;
					//惩罚保证金接收账户
					byte[] punishmentAccount = verifyScript.getChunks().get(1).data;
					//输出账户
					byte[] ouputAccount = outputScript.getChunks().get(2).data;
					if(tx.getType() == Definition.TYPE_REM_CONSENSUS && !Arrays.equals(selfAccount, ouputAccount)) {
						result.setResult(false, "保证金的输出不合法,应该是保证金所属者");
						return validatorResult;
					} else if(tx.getType() == Definition.TYPE_VIOLATION) {
						//违规处理
						ViolationTransaction vt = (ViolationTransaction) tx;
						//证据
						ViolationEvidence violationEvidence = vt.getViolationEvidence();

						if(violationEvidence.getViolationType() == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK && !Arrays.equals(selfAccount, ouputAccount)) {
							result.setResult(false, "超时不出块,保证金的输出不合法,应该是保证金所属者");
							return validatorResult;
						} else if(violationEvidence.getViolationType() == ViolationEvidence.VIOLATION_TYPE_REPEAT_BROADCAST_BLOCK && !Arrays.equals(punishmentAccount, ouputAccount)) {
							result.setResult(false, "严重违规,重复出块,保证金的输出不合法,应该是罚没接收账户");
							return validatorResult;
						}
					}
				} else {
					//验证赎回脚本
					input.getScriptSig().execute(verifyScript);
				}
			}
			//验证本次交易的输出
			List<TransactionOutput> outputs = tx.getOutputs();
			for (Output output : outputs) {
				Coin outputCoin = Coin.valueOf(output.getValue());
				//输出金额不能为负数
				if(outputCoin.isLessThan(Coin.ZERO)) {
					result.setResult(false, "输出金额不能为负数");
					return validatorResult;
				}
				txOutputFee = txOutputFee.add(outputCoin);
			}
			//验证不能给自己转账
			if(tx.getType() == Definition.TYPE_PAY) {
				Script inputScript = new Script(scriptBytes);
				byte[] sender = inputScript.getChunks().get(2).data;
				TransactionOutput output = outputs.get(0);
				byte[] receiver = output.getScript().getChunks().get(2).data;
				if(Arrays.equals(sender, receiver)) {
					//不能给自己转账，因为毫无意义，一种情况除外
					//锁仓的时候，除外，但是锁仓需要大于24小时，并金额大于100
					Coin value = Coin.valueOf(output.getValue());
					long lockTime = output.getLockTime();

					//发送的金额必须大于100
					if(value.compareTo(Coin.COIN.multiply(100)) < 0) {
						result.setResult(false, "锁仓的金额需达到100");
						return validatorResult;
					}
					//锁仓的时间必须大于24小时
					if(lockTime - tx.getTime() < 24 * 60 * 60) {
						result.setResult(false, "锁仓时间必须大于24小时");
						return validatorResult;
					}
				}
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
				if(atx.getHasProduct() == 0) {
					TransactionStore txStore = blockStoreProvider.getTransaction(atx.getProductTx().getBytes());
					if (txStore == null || txStore.getTransaction() == null) {
						result.setResult(false, "产品不存在");
						return validatorResult;
					}
					ProductTransaction ptx = (ProductTransaction) txStore.getTransaction();
					if (!Arrays.equals(ptx.getHash160(), atx.getHash160())) {
						result.setResult(false, "不合法的产品引用");
						return validatorResult;
					}

					//检查产品状态是否可用  facjas
					if (ptx.getProduct().getStatus() == 1) {
						result.setResult(false, "产品状态为不可用");
						return validatorResult;
					}
				}
				//检查用户是否为认证账户，检查用户状态是否可用
				byte[] hash160 = atx.getHash160();
				AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
				if(accountInfo == null || accountInfo.getType() != network.getCertAccountVersion()|| accountInfo.getStatus() != 0||accountInfo.getLevel()>Configure.MAX_CERT_LEVEL) {
					result.setResult(false, "只有激活状态下的认证账户才能创建防伪码");
					return validatorResult;
				}

				//检查账户是否被吊销
				if(chainstateStoreProvider.isCertAccountRevoked(hash160)){
					result.setResult(false, "认证账户被吊销");
					return validatorResult;
				}

				//防伪码不能重复
				try {
					byte[] txid = chainstateStoreProvider.getBytes(atx.getAntifakeCode());
					if(txid != null) {
						result.setResult(false, "重复的防伪码");
						return validatorResult;
					}
				} catch (IOException e) {
					result.setResult(false, "验证防伪码是否重复时出错，错误信息：" + e.getMessage());
					return validatorResult;
				}
			}else if(tx.getType() == Definition.TYPE_CREATE_PRODUCT){
				//检查账户是否被吊销
				ProductTransaction ptx = (ProductTransaction) tx;
				if(chainstateStoreProvider.isCertAccountRevoked(ptx.getHash160())){
					result.setResult(false, "认证账户被吊销");
					return validatorResult;
				}

			}else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_BIND){
				AntifakeCodeBindTransaction bindtx = (AntifakeCodeBindTransaction) tx;
				byte[] antifakeCodeVerifyMakeTxHash = chainstateStoreProvider.getBytes(bindtx.getAntifakeCode());
				if(antifakeCodeVerifyMakeTxHash == null) {
					result.setResult(false, "防伪码不存在");
					return validatorResult;
				}
				if(antifakeCodeVerifyMakeTxHash.length == 2*Sha256Hash.LENGTH){
					result.setResult(false, "防伪码已经绑定到产品");
					return validatorResult;
				}
				byte[] makebyte = new byte[Sha256Hash.LENGTH];
				System.arraycopy(antifakeCodeVerifyMakeTxHash,0,makebyte,0,Sha256Hash.LENGTH);
				TransactionStore txStore = blockStoreProvider.getTransaction(antifakeCodeVerifyMakeTxHash);
				if(txStore == null || txStore.getTransaction() == null) {
					result.setResult(false, "防伪码生成交易不存在");
					return validatorResult;
				}
			}
			else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
				//防伪码验证交易
				AntifakeCodeVerifyTransaction acvtx = (AntifakeCodeVerifyTransaction) tx;

				byte[] antifakeCodeVerifyMakeTxHash = chainstateStoreProvider.getBytes(acvtx.getAntifakeCode());
				if(antifakeCodeVerifyMakeTxHash == null) {
					result.setResult(false, "防伪码不存在");
					return validatorResult;
				}

				byte[] makebyte = new byte[Sha256Hash.LENGTH];
				byte[] bindbyte = new byte[Sha256Hash.LENGTH];
				System.arraycopy(antifakeCodeVerifyMakeTxHash,0,makebyte,0,Sha256Hash.LENGTH);
				if(antifakeCodeVerifyMakeTxHash.length == 2*Sha256Hash.LENGTH){
					System.arraycopy(antifakeCodeVerifyMakeTxHash,0+Sha256Hash.LENGTH,bindbyte,0,Sha256Hash.LENGTH);
				}

				TransactionStore txStore = blockStoreProvider.getTransaction(makebyte);
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
				if(status != null && Arrays.equals(status, new byte[] { 2 })) {
					result.setResult(false, "防伪码已被验证");
					return validatorResult;
				}
			} else if(tx.getType() == Definition.TYPE_REG_CONSENSUS) {
				//申请成为共识节点
				RegConsensusTransaction regConsensusTx = (RegConsensusTransaction) tx;
				byte[] hash160 = regConsensusTx.getHash160();
				//获取申请人信息，包括信用和可用余额
				AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
				if(accountStore == null && regConsensusTx.isCertAccount()) {
					result.setResult(false, "账户不存在");
					return validatorResult;
				}

				//判断是否达到共识条件
				long credit = (accountStore == null ? 0 : accountStore.getCert());
				BlockHeader blockHeader = blockStoreProvider.getBlockHeaderByperiodStartTime(regConsensusTx.getPeriodStartTime());
				long consensusCredit = ConsensusCalculationUtil.getConsensusCredit(blockHeader.getHeight());
				if(credit < consensusCredit) {
					//信用不够
					result.setResult(false, "共识账户信用值过低 " + credit + "  " + consensusCredit);
					return validatorResult;
				}

				//判断是否已经是共识节点
				if(consensusPool.contains(hash160)) {
					//已经是共识节点了
					result.setResult(false, "已经是共识节点了,勿重复申请");
					return validatorResult;
				}
				//验证时段
				long periodStartTime = regConsensusTx.getPeriodStartTime();
				//必须是最近的几轮里
				if(dataSynchronizeHandler.hasComplete() && consensusMeeting.getMeetingItem(periodStartTime) == null) {
					throw new VerificationException("申请时段不合法");
				}
				//验证保证金
				//当前共识人数
				int currentConsensusSize = consensusMeeting.analysisConsensusSnapshots(periodStartTime).size();
				//共识保证金
				Coin recognizance = ConsensusCalculationUtil.calculatRecognizance(currentConsensusSize, blockHeader.getHeight());
				if(!Coin.valueOf(outputs.get(0).getValue()).equals(recognizance)) {
					result.setResult(false, "保证金不正确");
					currentConsensusSize = consensusMeeting.analysisConsensusSnapshots(periodStartTime).size();
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
			} else if(tx instanceof ViolationTransaction) {
				//违规处罚交易，验证违规证据是否合法
				ViolationTransaction vtx = (ViolationTransaction)tx;

				//违规证据
				ViolationEvidence violationEvidence = vtx.getViolationEvidence();
				if(violationEvidence == null) {
					result.setResult(false, "处罚交易违规证据不能为空");
					return validatorResult;
				}
				//违规证据是否已经被处理
				Sha256Hash evidenceHash = violationEvidence.getEvidenceHash();
				byte[] ptxHashBytes = chainstateStoreProvider.getBytes(evidenceHash.getBytes());
				if(ptxHashBytes != null) {
					result.setResult(false, "该违规已经被处理，不需要重复处理");
					return validatorResult;
				}

				//验证证据合法性
				if(violationEvidence.getViolationType() == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
					//超时未出块处罚
					NotBroadcastBlockViolationEvidence notBroadcastBlock = (NotBroadcastBlockViolationEvidence) violationEvidence;
					//验证逻辑
					byte[] hash160 = notBroadcastBlock.getAudienceHash160();
					long currentPeriodStartTime = notBroadcastBlock.getCurrentPeriodStartTime();
					long previousPeriodStartTime = notBroadcastBlock.getPreviousPeriodStartTime();

					BlockHeader startBlockHeader = blockStoreProvider.getBestBlockHeader().getBlockHeader();
					//取得当前轮的最后一个块
					while(true) {
						BlockHeaderStore lastHeaderStore = blockStoreProvider.getHeader(startBlockHeader.getPreHash().getBytes());
						if(lastHeaderStore != null) {
							BlockHeader lastHeader = lastHeaderStore.getBlockHeader();
							if(lastHeader.getPeriodStartTime() >= currentPeriodStartTime && !Sha256Hash.ZERO_HASH.equals(lastHeader.getPreHash())) {
								startBlockHeader = lastHeader;
							} else {
								startBlockHeader = lastHeader;
								break;
							}
						}
					}

					//原本应该打包的上一个块
					if(startBlockHeader == null || (!Sha256Hash.ZERO_HASH.equals(startBlockHeader.getPreHash()) && startBlockHeader.getPeriodStartTime() != previousPeriodStartTime)) {
						result.setResult(false, "违规证据中的两轮次不相连");
						return validatorResult;
					}

					//验证该轮的时段
					int index = getConsensusPeriod(hash160, currentPeriodStartTime);
					if(index == -1) {
						result.setResult(false, "证据不成立，该人不在本轮共识列表中");
						return validatorResult;
					}
					BlockHeaderStore currentStartBlockHeaderStore = blockStoreProvider.getHeaderByHeight(startBlockHeader.getHeight() + 1);
					if(currentStartBlockHeaderStore == null) {
						result.setResult(false, "证据不成立，当前轮还没有打包数据");
						return validatorResult;
					}
					BlockHeader currentStartBlockHeader = currentStartBlockHeaderStore.getBlockHeader();
					while(true) {
						if(currentStartBlockHeader.getTimePeriod() == index) {
							result.setResult(false, "证据不成立,本轮有出块");
							return validatorResult;
						}
						if(currentStartBlockHeader.getTimePeriod() < index) {
							BlockHeaderStore preBlockHeaderStoreTemp = blockStoreProvider.getHeaderByHeight(currentStartBlockHeader.getHeight() + 1);

							if(preBlockHeaderStoreTemp == null || preBlockHeaderStoreTemp.getBlockHeader() == null
									|| preBlockHeaderStoreTemp.getBlockHeader().getPeriodStartTime() != currentPeriodStartTime) {
								break;
							}

							currentStartBlockHeader = preBlockHeaderStoreTemp.getBlockHeader();
						} else {
							break;
						}
					}
					//验证上一轮的时段
					index = getConsensusPeriod(hash160, previousPeriodStartTime);
					if(index == -1) {
						result.setResult(false, "证据不成立，该人不在上一轮共识列表中");
						return validatorResult;
					}
					while(true) {
						if(startBlockHeader.getTimePeriod() == index) {
							result.setResult(false, "证据不成立,上一轮有出块");
							return validatorResult;
						}
						if(startBlockHeader.getTimePeriod() < index) {
							BlockHeaderStore preBlockHeaderStoreTemp = blockStoreProvider.getHeader(startBlockHeader.getPreHash().getBytes());

							if(preBlockHeaderStoreTemp == null || preBlockHeaderStoreTemp.getBlockHeader() == null
									|| preBlockHeaderStoreTemp.getBlockHeader().getPeriodStartTime() != previousPeriodStartTime) {
								break;
							}

							startBlockHeader = preBlockHeaderStoreTemp.getBlockHeader();
						} else {
							break;
						}
					}
				} else if(violationEvidence.getViolationType() == ViolationEvidence.VIOLATION_TYPE_REPEAT_BROADCAST_BLOCK) {
					//重复出块的验证
					//验证证据的合法性
					//违规证据
					RepeatBlockViolationEvidence repeatBlockViolationEvidence = (RepeatBlockViolationEvidence) violationEvidence;

					List<BlockHeader> blockHeaders = repeatBlockViolationEvidence.getBlockHeaders();

					//证据不能为空，且必须是2条记录
					if(blockHeaders == null || blockHeaders.size() != 2) {
						result.setResult(false, "证据个数不正确");
						return validatorResult;
					}

					BlockHeader blockHeader1 = blockHeaders.get(0);
					BlockHeader blockHeader2 = blockHeaders.get(1);
					if(!Arrays.equals(blockHeader1.getHash160(), blockHeader2.getHash160()) ||
							!Arrays.equals(blockHeader1.getHash160(), repeatBlockViolationEvidence.getAudienceHash160())) {
						result.setResult(false, "违规证据里的两个块打包人不相同,或者证据与被处理人不同");
						return validatorResult;
					}
					if(blockHeader1.getPeriodStartTime() != blockHeader2.getPeriodStartTime()) {
						result.setResult(false, "违规证据里的两个块时段不相同");
						return validatorResult;
					}
					//验证签名
					try {
						blockHeader1.verifyScript();
						blockHeader2.verifyScript();
					} catch (Exception e) {
						result.setResult(false, "违规证据里的两个块验证签名不通过");
						return validatorResult;
					}
				}
			} else if(tx.getType() == Definition.TYPE_ASSETS_REGISTER) {
				//资产登记
				//验证登记费用是否正确
				if(!Coin.valueOf(outputs.get(0).getValue()).equals(Configure.ASSETS_REG_FEE)) {
					result.setResult(false, "资产登记费用不正确");
					return validatorResult;
				}
				//验证社区账号

				byte[] receiveHash160 = outputs.get(0).getScript().getChunks().get(2).data;
				if(!Arrays.equals(receiveHash160, network.getCommunityManagerHash160())) {
					result.setResult(false, "收取手续费账号不正确");
					return validatorResult;
				}

				//验证编码是否重复
				AssetsRegisterTransaction artx = (AssetsRegisterTransaction) tx;
				if(chainstateStoreProvider.hasAssetsReg(artx.getCode())) {
					result.setResult(false, "资产代码已注册，请勿重复使用");
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

			if(regTx.getLevel()>Configure.MAX_CERT_LEVEL){
				result.setResult(false, "签发该账户的上级账户不具备该权限");
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

		} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
			//认证账户修改信息
			CertAccountUpdateTransaction updateTx = (CertAccountUpdateTransaction) tx;
			byte[] hash160 = updateTx.getHash160();

			//必须是自己最新的账户状态
			byte[] verTxid = updateTx.getScript().getChunks().get(1).data;
			byte[] verTxBytes = chainstateStoreProvider.getBytes(verTxid);
			if(verTxBytes == null) {
				result.setResult(false, "签名错误");
				return validatorResult;
			}
			//检查用户是否为认证账户，检查用户状态是否可用
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountInfo == null || accountInfo.getType() != network.getCertAccountVersion() || accountInfo.getStatus() !=0 ) {
				result.setResult(false, "只有激活状态下的认证账户才能修改");
				return validatorResult;
			}


			CertAccountRegisterTransaction verTx = new CertAccountRegisterTransaction(network, verTxBytes);

			//认证帐户，就需要判断是否经过认证的
			if(!Arrays.equals(verTx.getHash160(), hash160)) {
				result.setResult(false, "错误的签名，账户不匹配");
				return validatorResult;
			}
		}else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REVOKE){
			CertAccountRevokeTransaction revokeTx = (CertAccountRevokeTransaction) tx;
			byte[] hash160 = revokeTx.getHash160();
			byte[] revokehash160 = revokeTx.getRevokeHash160();

			if(revokeTx.getLevel() >= Configure.MAX_CERT_LEVEL){
				result.setResult(false, "签发该账户的上级账户不具备该权限");
				return validatorResult;
			}
			//检查用户是否为认证账户，检查用户状态是否可用
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
			AccountStore raccountinfo  = chainstateStoreProvider.getAccountInfo(revokehash160);


			if(accountInfo == null || accountInfo.getType() != network.getCertAccountVersion() || accountInfo.getStatus() !=0 ) {
				result.setResult(false, "只有激活状态下的认证账户才能修改");
				return validatorResult;
			}
			if(raccountinfo == null){
				result.setResult(false, "被吊销的账户不存在");
				return validatorResult;
			}
			//检查账户是否被吊销
			if(chainstateStoreProvider.isCertAccountRevoked(hash160)){
				result.setResult(false, "本地管理员账户已经被吊销");
				return validatorResult;
			}
			if(chainstateStoreProvider.isCertAccountRevoked(revokehash160)){
				result.setResult(false, "将要吊销的账户已经被吊销");
				return validatorResult;
			}

			if((accountInfo.getLevel() == 3 && !Arrays.equals(raccountinfo.getSupervisor(),accountInfo.getHash160())) || accountInfo.getLevel()>3|| accountInfo.getLevel()>= raccountinfo.getLevel()){
				result.setResult(false, "不具备吊销该账户的权限");
				return validatorResult;
			}


		}
		else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {
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
		} else if(tx.getType() == Definition.TYPE_REG_ALIAS) {
			//注册别名
			RegAliasTransaction rtx = (RegAliasTransaction) tx;
			//账户必须达到规定的信用，才能注册别名
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(rtx.getHash160());
			if(accountInfo == null || accountInfo.getCert() < Configure.REG_ALIAS_CREDIT) {
				result.setResult(false, "账户信用达到" + Configure.REG_ALIAS_CREDIT + "之后才能注册别名");
				return validatorResult;
			}
			//是否已经设置过别名了
			byte[] alias = accountInfo.getAlias();
			if(alias != null && alias.length > 0) {
				result.setResult(false, "已经设置别名，不能重复设置");
				return validatorResult;
			}
			//别名是否已经存在
			accountInfo = chainstateStoreProvider.getAccountInfoByAlias(alias);
			if(accountInfo != null) {
				result.setResult(false, "别名已经存在");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_UPDATE_ALIAS) {
			//修改别名
			UpdateAliasTransaction utx = (UpdateAliasTransaction) tx;
			//账户必须达到规定的信用，才能修改别名
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(utx.getHash160());
			if(accountInfo == null || accountInfo.getCert() < Configure.UPDATE_ALIAS_CREDIT) {
				result.setResult(false, "账户信用达到" + Configure.UPDATE_ALIAS_CREDIT + "之后才能修改别名");
				return validatorResult;
			}
			//是否已经设置过别名了
			byte[] alias = accountInfo.getAlias();
			if(alias == null || alias.length == 0) {
				result.setResult(false, "没有设置别名，不能修改");
				return validatorResult;
			}
			//新别名是否已经存在
			accountInfo = chainstateStoreProvider.getAccountInfoByAlias(utx.getAlias());
			if(accountInfo != null) {
				result.setResult(false, "新别名已经存在");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_RELEVANCE_SUBACCOUNT) {
			//注册子账户
			RelevanceSubAccountTransaction rst = (RelevanceSubAccountTransaction) tx;
			//交易账户必须是认证账户
			byte[] hash160 = rst.getHash160();
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountInfo == null || accountInfo.getType() != network.getCertAccountVersion()) {
				result.setResult(false, "只有认证账户才能添加子账户");
				return validatorResult;
			}
			if(accountInfo.getLevel()==Configure.REVOKED_CERT_LEVEL){
				result.setResult(false, "该认证账户已经被吊销");
				return validatorResult;
			}
			//检查账户是否被吊销
			if(chainstateStoreProvider.isCertAccountRevoked(hash160)){
				result.setResult(false, "认证账户被吊销");
				return validatorResult;
			}
			//添加的账户是否达到上限,100个
			int count = chainstateStoreProvider.getSubAccountCount(hash160);
			if(count > 100) {
				result.setResult(false, "已达到子账户上限100个，不能继续添加");
				return validatorResult;
			}
			//验证是否重复关联
			Sha256Hash txHash = chainstateStoreProvider.checkIsSubAccount(hash160, rst.getRelevanceHashs());
			if(txHash != null) {
				result.setResult(false, "重复注册");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_REMOVE_SUBACCOUNT) {
			//移除子账户
			RemoveSubAccountTransaction rst = (RemoveSubAccountTransaction) tx;
			//交易账户必须是认证账户
			byte[] hash160 = rst.getHash160();
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountInfo == null || accountInfo.getType() != network.getCertAccountVersion()) {
				result.setResult(false, "只有认证账户才能添加子账户");
				return validatorResult;
			}
			//检查账户是否被吊销
			if(chainstateStoreProvider.isCertAccountRevoked(hash160)){
				result.setResult(false, "认证账户被吊销");
				return validatorResult;
			}
			//验证是否存在
			Sha256Hash txHash = chainstateStoreProvider.checkIsSubAccount(hash160, rst.getRelevanceHashs());
			if(txHash == null) {
				result.setResult(false, "欲删除不存在的关联");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CIRCULATION) {
			//防伪码流转信息
			CirculationTransaction ctx = (CirculationTransaction) tx;
			//通过防伪码查询到防伪信息
			byte[] antifakeCode = ctx.getAntifakeCode();
			//先验证防伪码是否是合法状态
			byte[] antifakeCodeVerifyMakeTxHash = chainstateStoreProvider.getBytes(antifakeCode);
			if(antifakeCodeVerifyMakeTxHash == null) {
				result.setResult(false, "防伪码不存在");
				return validatorResult;
			}

			TransactionStore txStore = blockStoreProvider.getTransaction(antifakeCodeVerifyMakeTxHash);
			if(txStore == null || txStore.getTransaction() == null) {
				result.setResult(false, "防伪码生成交易不存在");
				return validatorResult;
			}
			Transaction avmTx = txStore.getTransaction();
			if(avmTx.getType() != Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				result.setResult(false, "错误的防伪码");
				return validatorResult;
			}
			AntifakeCodeMakeTransaction antifakeMakeTx = (AntifakeCodeMakeTransaction) avmTx;
			if(antifakeMakeTx.getHasProduct() == 1){
				result.setResult(false, "防伪码尚未关联产品");
				return validatorResult;
			}
			//保证该防伪码没有被验证
			byte[] txStatus = antifakeMakeTx.getHash().getBytes();
			byte[] txIndex = new byte[txStatus.length + 1];

			System.arraycopy(txStatus, 0, txIndex, 0, txStatus.length);
			txIndex[txIndex.length - 1] = 0;

			byte[] status = chainstateStoreProvider.getBytes(txIndex);
			if(status != null && Arrays.equals(status, new byte[] { 2 })) {
				result.setResult(false, "防伪码已被验证");
				return validatorResult;
			}

			//操作人必须是商家自己，或者关联的子账户，否则验证失败
			//先验证是否是商家自己
			byte[] certHash160 = antifakeMakeTx.getHash160();
			byte[] hash160 = ctx.getHash160();
			if(Arrays.equals(certHash160, hash160)) {
				result.setResult(true, "ok");
				return validatorResult;
			}
			Address address = new Address(network, ctx.isCertAccount() ? network.getCertAccountVersion() : network.getSystemAccountVersion(), hash160);
			//不是商家自己，那么验证是否子账户
			Sha256Hash subAccountTx = chainstateStoreProvider.checkIsSubAccount(certHash160, address.getHash());
			if(subAccountTx == null) {
				result.setResult(false, "非法的操作,添加流转信息必须是商家自己或者商家关联的子账户");
				return validatorResult;
			}
			//每个子账户对应每个防伪码只能关联5条流转信息
			int count = chainstateStoreProvider.getCirculationCount(antifakeCode, address.getHash());
			if(count > CirculationTransaction.SUB_ACCOUNT_MAX_SIZE) {
				result.setResult(false, "添加的流转信息超过数量限制，不能大于" + CirculationTransaction.SUB_ACCOUNT_MAX_SIZE + "条");
				return validatorResult;
			}
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_TRANSFER) {
			AntifakeTransferTransaction attx = (AntifakeTransferTransaction) tx;

			//账户必须达到规定的信用，才能转让防伪码
			AccountStore accountInfo = chainstateStoreProvider.getAccountInfo(attx.getHash160());
			if(accountInfo == null || accountInfo.getCert() < Configure.TRANSFER_ANTIFAKECODE_CREDIT) {
				result.setResult(false, "账户信用达到" + Configure.TRANSFER_ANTIFAKECODE_CREDIT + "之后才能转让");
				return validatorResult;
			}

			byte[] antifakeCode = attx.getAntifakeCode();

			//先验证防伪码是否是合法状态
			byte[] makebind = chainstateStoreProvider.getBytes(antifakeCode);

			if(makebind == null) {
				result.setResult(false, "防伪码不存在");
				return validatorResult;
			}
			byte[] makebyte = new byte[Sha256Hash.LENGTH];
			System.arraycopy(makebind,0,makebyte,0,Sha256Hash.LENGTH);

			TransactionStore txStore = blockStoreProvider.getTransaction(makebyte);
			if(txStore == null || txStore.getTransaction() == null) {
				result.setResult(false, "防伪码生成交易不存在");
				return validatorResult;
			}
			Transaction avmTx = txStore.getTransaction();
			if(avmTx.getType() != Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				result.setResult(false, "错误的防伪码");
				return validatorResult;
			}
			AntifakeCodeMakeTransaction antifakeCodeMakeTx = (AntifakeCodeMakeTransaction) avmTx;
			//保证该防伪码已经被验证
			byte[] txStatus = antifakeCodeMakeTx.getHash().getBytes();
			byte[] txIndex = new byte[txStatus.length + 1];

			System.arraycopy(txStatus, 0, txIndex, 0, txStatus.length);
			txIndex[txIndex.length - 1] = 0;

			byte[] status = chainstateStoreProvider.getBytes(txIndex);
			if(status != null && Arrays.equals(status, new byte[] { 1 })) {
				result.setResult(false, "防伪码未被验证，不能转让");
				return validatorResult;
			}
			//必须是拥有者才能转让
			Address owner = accountKit.queryAntifakeOwner(antifakeCode);
			if(owner == null || !Arrays.equals(owner.getHash160(), attx.getHash160())) {
				result.setResult(false, "没有权限转让");
				return validatorResult;
			}
		}else if(tx.getType() == Definition.TYPE_CREATE_PRODUCT ){
			ProductTransaction ptx = (ProductTransaction)tx;
			if (chainstateStoreProvider.isCertAccountRevoked(((ProductTransaction) tx).getHash160())){
				result.setResult(false, "账户已经被吊销，无法创建商品");
				return validatorResult;
			}
		}  else if(tx.getType() == Definition.TYPE_ASSETS_ISSUED) {
			//资产发行
			AssetsIssuedTransaction aitx = (AssetsIssuedTransaction) tx;

			byte[] opertioner =  aitx.getHash160();
			Sha256Hash assetsTxHash = aitx.getAssetsHash();
			//验证操作人是否合法
			TransactionStore assetsRegisterTxStore = blockStoreProvider.getTransaction(assetsTxHash.getBytes());
			if(assetsRegisterTxStore == null) {
				result.setResult(false, "资产不存在");
				return validatorResult;
			}
			//验证资产发行账户是否是注册资产的账户
			AssetsRegisterTransaction assetsRegisterTx = (AssetsRegisterTransaction) assetsRegisterTxStore.getTransaction();
			if(!Arrays.equals(assetsRegisterTx.getHash160(), opertioner)) {
				result.setResult(false, "没有权限发行该资产");
				return validatorResult;
			}

		} else if(tx.getType() == Definition.TYPE_ASSETS_TRANSFER) {
			//资产转让验证
			AssetsTransferTransaction assetsTransferTx = (AssetsTransferTransaction) tx;
			Sha256Hash assetsTxHash = assetsTransferTx.getAssetsHash();
			//验证资产是否存在
			TransactionStore assetsRegisterTxStore = blockStoreProvider.getTransaction(assetsTxHash.getBytes());
			if(assetsRegisterTxStore == null) {
				result.setResult(false, "资产不存在");
				return validatorResult;
			}

			//验证sender余额是否充足, 通过交易的hash160
			byte[] sender =  assetsTransferTx.getHash160();
			//获取转让人的资产信息
			AssetsRegisterTransaction assetsRegisterTx = (AssetsRegisterTransaction)assetsRegisterTxStore.getTransaction();
			Assets assets = chainstateStoreProvider.getMyAssetsByCode(sender, Sha256Hash.hash(assetsRegisterTx.getCode()));
			if(assets == null) {
				result.setResult(false, "转让人没有与资产相关的信息");
				return validatorResult;
			}
			if(assets.getBalance() < assetsTransferTx.getAmount()) {
				result.setResult(false, "转让人资产余额不足");
				return validatorResult;
			}
		}

		result.setSuccess(true);
		result.setMessage("ok");
		return validatorResult;
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
//		log.info("被处理人： {} , 开始时间： {} ,  列表： {}", new Address(network, hash160).getBase58(), DateUtil.convertDate(new Date(periodStartTime*1000)), consensusList);
		if(log.isDebugEnabled()) {
			log.debug("被处理人： {} , 开始时间： {} ,  列表： {}", new Address(network, hash160).getBase58(), DateUtil.convertDate(new Date(periodStartTime*1000)), consensusList);
		}
		//获取位置
		for (int i = 0; i < consensusList.size(); i++) {
			ConsensusAccount consensusAccount = consensusList.get(i);
			if(Arrays.equals(hash160, consensusAccount.getHash160())) {
				return i;
			}
		}
		return -1;
	}
}
