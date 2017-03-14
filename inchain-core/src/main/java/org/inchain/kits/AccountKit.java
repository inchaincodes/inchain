package org.inchain.kits;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusPoolCacher;
import org.inchain.core.AntifakeCode;
import org.inchain.core.BroadcastMakeAntifakeCodeResult;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.Result;
import org.inchain.core.TimeService;
import org.inchain.core.exception.MoneyNotEnoughException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.listener.NoticeListener;
import org.inchain.listener.TransactionListener;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.LocalTransactionSigner;
import org.inchain.store.AccountStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.CertAccountUpdateTransaction;
import org.inchain.transaction.business.RegConsensusTransaction;
import org.inchain.transaction.business.RemConsensusTransaction;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.validator.ValidatorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 账户管理
 * @author ln
 *
 */
@Service
public class AccountKit {
	
	private final static Logger log = LoggerFactory.getLogger(AccountKit.class);

	private final static Lock locker = new ReentrantLock();
	
	//账户文件路径
	private String accountDir;
	private List<Account> accountList = new ArrayList<Account>();
	//状态连存储服务
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	//交易存储服务
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionValidator transactionValidator;
	@Autowired
	private ConsensusPoolCacher consensusPoolCacher;
	//网络
	@Autowired
	private NetworkParams network;
	//节点管理器
	@Autowired
	private PeerKit peerKit;

	//交易监听器
	private TransactionListener transactionListener;
	
	public AccountKit() throws IOException {
		//帐户信息保存于数据目录下的account目录，以account开始的dat文件，一个文件一个帐户，支持多帐户
		this.accountDir = Configure.DATA_ACCOUNT;
		
//		//初始化交易存储服务，保存与帐户有关的所有交易，保存于数据目录下的transaction文件夹
//		this.transactionStoreProvider = TransactionStoreProvider.getInstace(Configure.DATA_TRANSACTION, network);
//		//初始化状态链存储服务，该目录保存的所有未花费的交易，保存于数据目录下的chainstate文件夹
//		this.chainstateStoreProvider = TransactionStoreProvider.getInstace(Configure.DATA_CHAINSTATE, network);
		
	}
	
	/**
	 * 初始化账户信息
	 */
	public synchronized void init() throws IOException {
		maybeCreateAccountDir();
		loadAccount();
		initListeners();
	}

	/**
	 * 关闭资源
	 * @throws IOException 
	 */
	public void close() throws IOException {
		chainstateStoreProvider.close();
	}
	
	/**
	 * 账户列表
	 */
	public void listAccount() {
		
	}
	
	/**
	 * 地址列表
	 */
	public void listAddress() {
		
	}
	
	/**
	 * 地址列表
	 */
	public void listAddress(String accountId) {
		
	}
	
	/**
	 * 获取默认账户
	 * @return Account
	 */
	public Account getDefaultAccount() {
		if(accountList == null || accountList.size() == 0) {
			return null;
		}
		return accountList.get(0);
	}
	
	/**
	 * 获取一个系统账户，如果没有则返回null
	 * @return Account
	 */
	public Account getSystemAccount() {
		if(accountList == null || accountList.size() == 0) {
			return null;
		}
		for (Account account : accountList) {
			if(!account.isCertAccount()) {
				return account;
			}
		}
		return null;
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance() {
		if(accountList == null || accountList.size() == 0) {
			return Coin.ZERO;
		}
		return getBalance(getDefaultAccount());
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance(String address) {
		return getBalance(Address.fromBase58(network, address));
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance(Account account) {
		return getBalance(account.getAddress());
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance(Address address) {
		if(address == null) {
			return Coin.ZERO;
		}
		return address.getBalance();
	}
	
	/**
	 * 获取可用余额
	 */
	public Coin getCanUseBalance() {
		if(accountList == null || accountList.size() == 0) {
			return Coin.ZERO;
		}
		return getCanUseBalance(getDefaultAccount().getAddress());
	}
	
	/**
	 * 获取可用余额
	 */
	public Coin getCanUseBalance(Address address) {
		if(address == null) {
			return Coin.ZERO;
		}
		return address.getBalance();
	}
	
	/**
	 * 获取不可用余额
	 */
	public Coin getCanNotUseBalance() {
		if(accountList == null || accountList.size() == 0) {
			return Coin.ZERO;
		}
		return getCanNotUseBalance(getDefaultAccount().getAddress());
	}
	
	/**
	 * 获取不可用余额
	 */
	public Coin getCanNotUseBalance(Address address) {
		if(address == null) {
			return Coin.ZERO;
		}
		return address.getUnconfirmedBalance();
	}
	

	/**
	 * 通过交易ID查询交易
	 * @param hash
	 * @return TransactionStore
	 */
	public TransactionStore getTransaction(Sha256Hash hash) {
		return blockStoreProvider.getTransaction(hash.getBytes());
	}
	
	/**
	 * 获取交易列表
	 */
	public List<TransactionStore> getTransactions() {
		return transactionStoreProvider.getTransactions();
	}
	
	/**
	 * 获取链上交易状态
	 */
	public byte[] getChainstate(byte[] hash) {
		return chainstateStoreProvider.getBytes(hash);
	}
	
	/**
	 * 获取交易列表
	 */
	public void getTransaction(String accountId) {
		
	}
	
	/**
	 * 认证账户，生产防伪码
	 * @param productTx 关联的商品
	 * @param reward    是否附带验证奖励
	 * @return BroadcastMakeAntifakeCodeResult
	 * @throws VerificationException
	 */
	public BroadcastMakeAntifakeCodeResult makeAntifakeCode(String productTx, Coin reward) throws VerificationException {
		//必须是认证账户才可以生成防伪码
		Account account = getDefaultAccount();
		
		if(account == null || !account.isCertAccount()) {
			throw new VerificationException("非认证账户，不能生成防伪码");
		}
		if(account.isEncryptedOfTr()) {
			throw new VerificationException("账户已加密，无法签名信息");
		}
		
		//对应的商品不能为空
		if(productTx == null || productTx.isEmpty()) {
			throw new VerificationException("需要生成防伪码的商品不能为空");
		}
		Sha256Hash productTxHash = Sha256Hash.wrap(productTx);
		
		AntifakeCodeMakeTransaction tx = new AntifakeCodeMakeTransaction(network, productTxHash);
		
		//是否附带奖励
		Coin money = Coin.ZERO;
		if(reward != null && reward.isGreaterThan(money)) {
			money = reward;
		}
		//输入金额
		Coin totalInputCoin = Coin.ZERO;
		if(money.isGreaterThan(Coin.ZERO)) {
			//选择输入
			List<TransactionOutput> fromOutputs = selectNotSpentTransaction(money, account.getAddress());
			if(fromOutputs == null || fromOutputs.size() == 0) {
				throw new VerificationException("余额不足，无法奖励");
			}
			for (TransactionOutput output : fromOutputs) {
				TransactionInput input = new TransactionInput(output);
				//认证账户的签名
				input.setScriptSig(ScriptBuilder.createCertAccountInputScript(null, account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160()));
				tx.addInput(input);
				
				totalInputCoin = totalInputCoin.add(Coin.valueOf(output.getValue()));
			}
		}
		
		//交易输出
		Sha256Hash antifakeCode = null;
		try {
			antifakeCode = tx.getAntifakeHash();
		} catch (Exception e) {
			throw new VerificationException("获取防伪码出错：" + e.getMessage());
		}
		
		Script out = ScriptBuilder.createAntifakeOutputScript(account.getAddress().getHash160(), antifakeCode);
		tx.addOutput(money, out);
		
		//是否找零
		if(totalInputCoin.isGreaterThan(money)) {
			tx.addOutput(totalInputCoin.subtract(money), account.getAddress());
		}
		
		//签名交易，如果有输入
		if(money.isGreaterThan(Coin.ZERO)) {
			final LocalTransactionSigner signer = new LocalTransactionSigner();
			//认证账户的签名
			signer.signCertAccountInputs(tx, account.getTrEckeys(), account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160());
		}
		tx.sign(account);
		
		tx.verify();
		tx.verifyScript();
		
		//验证交易是否合法
		ValidatorResult<TransactionValidatorResult> rs = transactionValidator.valDo(tx, null);
		if(!rs.getResult().isSuccess()) {
			throw new VerificationException(rs.getResult().getMessage());
		}

		//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
		boolean success = MempoolContainer.getInstace().add(tx);
		if(!success) {
			throw new VerificationException("加入内存池失败，可能原因[交易重复]");
		}
		
		try {
			BroadcastResult result = peerKit.broadcast(tx).get();
			
			BroadcastMakeAntifakeCodeResult maResult = new BroadcastMakeAntifakeCodeResult(result.isSuccess(), result.getMessage());
			//等待广播回应
			if(result.isSuccess()) {
				//更新交易记录
				transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
				//签名防伪码
				byte[][] signs = LocalTransactionSigner.signHash(account, tx.getAntifakeHash());
				AntifakeCode ac = new AntifakeCode(tx.getAntifakeHash(), account.getAccountTransaction().getHash(), signs);
				maResult.setAntifakeCode(ac);;
			}
			return maResult;
		} catch (Exception e) {
			return new BroadcastMakeAntifakeCodeResult(false, "广播失败，失败信息：" + e.getMessage());
		}
	}
	
	/**
	 * 防伪码验证
	 * @param antifakeCodeContent 防伪码内容，这个是按一定规则组合的
	 * @return BroadcastResult
	 * @throws VerificationException
	 */
	public BroadcastResult verifyAntifakeCode(String antifakeCodeContent) throws VerificationException {
		//解析防伪码字符串
		AntifakeCode antifakeCode = AntifakeCode.base58Decode(antifakeCodeContent);
		
		//判断验证码是否存在
		byte[] txBytes = chainstateStoreProvider.getBytes(antifakeCode.getAntifakeTx().getBytes());
		if(txBytes == null) {
			throw new VerificationException("防伪码不存在");
		}
		
		TransactionStore txStore = blockStoreProvider.getTransaction(txBytes);
		//必须存在
		if(txStore == null) {
			throw new VerificationException("防伪码生产交易不存在");
		}
		
		Transaction fromTx = txStore.getTransaction();
		//交易类型必须是防伪码生成交易
		if(fromTx.getType() != Definition.TYPE_ANTIFAKE_CODE_MAKE) {
			throw new VerificationException("防伪码类型错误");
		}
		AntifakeCodeMakeTransaction codeMakeTx = (AntifakeCodeMakeTransaction) fromTx;
		
		//验证防伪码是否已经被验证了
		//保证该防伪码没有被验证
		byte[] txStatus = codeMakeTx.getHash().getBytes();
		byte[] txIndex = new byte[txStatus.length + 1];
		
		System.arraycopy(txStatus, 0, txIndex, 0, txStatus.length);
		txIndex[txIndex.length - 1] = 0;
		
		byte[] status = chainstateStoreProvider.getBytes(txIndex);
		if(status == null) {
			throw new VerificationException("验证失败，该防伪码已被验证");
		}
		
		//防伪码验证脚本
		Script inputSig = ScriptBuilder.createAntifakeInputScript(antifakeCode.getCertAccountTx(), antifakeCode.getSigns());
		
		TransactionInput input = new TransactionInput((TransactionOutput) codeMakeTx.getOutput(0));
		input.setScriptSig(inputSig);
		
		AntifakeCodeVerifyTransaction tx = new AntifakeCodeVerifyTransaction(network, input);
		
		//验证账户，不能是认证账户
		Account systemAccount = getSystemAccount();
		if(systemAccount == null) {
			throw new VerificationException("账户不存在，不能验证");
		}
		
		//添加奖励输出
		Coin rewardCoin = codeMakeTx.getRewardCoin();
		if(rewardCoin != null && rewardCoin.isGreaterThan(Coin.ZERO)) {
			tx.addOutput(rewardCoin, systemAccount.getAddress());
		}
		
		//签名即将广播的信息
		tx.sign(systemAccount);
		
		//验证成功才广播
		tx.verify();
		tx.verifyScript();
		
		//验证交易合法才广播
		//这里面同时会判断是否被验证过了
		TransactionValidatorResult rs = transactionValidator.valDo(tx, null).getResult();
		if(!rs.isSuccess()) {
			String message = rs.getMessage();
			if(rs.getErrorCode() == TransactionValidatorResult.ERROR_CODE_USED) {
				message = "验证失败，该防伪码已被验证";
			}
			throw new VerificationException(message);
		}

		//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
		boolean success = MempoolContainer.getInstace().add(tx);
		if(!success) {
			throw new VerificationException("验证失败，该防伪码已被验证");
		}
		
		try {
			BroadcastResult result = peerKit.broadcast(tx).get();
			
			//等待广播回应
			if(result.isSuccess()) {
				//更新交易记录
				transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
				result.setMessage("恭喜您，验证通过");
			}
			return result;
		} catch (Exception e) {
			return new BroadcastResult(false, "广播失败，失败信息：" + e.getMessage());
		}
	}
	
	/**
	 * 发送普通交易到指定地址
	 * @param to   base58的地址
	 * @param money	发送金额
	 * @param fee	手续费
	 * @return String
	 * @throws MoneyNotEnoughException
	 */
	public BroadcastResult sendMoney(String to, Coin money, Coin fee) throws MoneyNotEnoughException {
		//参数不能为空
		Utils.checkNotNull(to);
		
		locker.lock();
		try {
			Address receiveAddress = null;
			try {
				receiveAddress = Address.fromBase58(network, to);
			} catch (Exception e) {
				throw new VerificationException("错误的接收地址");
			}
			
			//发送的金额必须大于0
			if(money.compareTo(Coin.ZERO) <= 0) {
				throw new RuntimeException("发送的金额需大于0");
			}
			if(fee == null || fee.compareTo(Coin.ZERO) < 0) {
				fee = Coin.ZERO;
			}
			
			if(accountList == null || accountList.size() == 0) {
				throw new VerificationException("没有可用账户");
			}
			
			//账户是否已加密
			Account account = getDefaultAccount();
			if((account.getAccountType() == network.getSystemAccountVersion() && account.isEncrypted()) ||
					(account.getAccountType() == network.getCertAccountVersion() && account.isEncryptedOfTr())) {
				throw new VerificationException("账户已加密");
			}
			
			//如果是认证账户，但是没有被收录进链里，则账户不可用
			if(account.isCertAccount() && account.getAccountTransaction() == null) {
				throw new VerificationException("账户不可用");
			}
			
			Address myAddress = account.getAddress();
			
			//当前余额可用余额
			Coin balance = myAddress.getBalance();
			
			//检查余额是否充足
			if(money.add(fee).compareTo(balance) > 0) {
				throw new MoneyNotEnoughException("余额不足");
			}
			
			Transaction tx = new Transaction(network);
			tx.setTime(TimeService.currentTimeMillis());
			tx.setLockTime(TimeService.currentTimeMillis());
			tx.setType(Definition.TYPE_PAY);
			tx.setVersion(Definition.VERSION);
			
			Coin totalInputCoin = Coin.ZERO;
			
			//选择输入
			List<TransactionOutput> fromOutputs = selectNotSpentTransaction(money.add(fee), myAddress);
			
			for (TransactionOutput output : fromOutputs) {
				TransactionInput input = new TransactionInput(output);
				//创建一个输入的空签名
				if(account.getAccountType() == network.getSystemAccountVersion()) {
					//普通账户的签名
					input.setScriptSig(ScriptBuilder.createInputScript(null, account.getEcKey()));
				} else {
					//认证账户的签名
					input.setScriptSig(ScriptBuilder.createCertAccountInputScript(null, account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160()));
				}
				tx.addInput(input);
				
				totalInputCoin = totalInputCoin.add(Coin.valueOf(output.getValue()));
			}
			
			//交易输出
			tx.addOutput(money, receiveAddress);
			//是否找零
			if(totalInputCoin.compareTo(money.add(fee)) > 0) {
				tx.addOutput(totalInputCoin.subtract(money.add(fee)), myAddress);
			}
			
			//签名交易
			final LocalTransactionSigner signer = new LocalTransactionSigner();
			try {
				if(account.getAccountType() == network.getSystemAccountVersion()) {
					//普通账户的签名
					signer.signInputs(tx, account.getEcKey());
				} else {
					//认证账户的签名
					signer.signCertAccountInputs(tx, account.getTrEckeys(), account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160());
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				BroadcastResult broadcastResult = new BroadcastResult();
				broadcastResult.setSuccess(false);
				broadcastResult.setMessage("签名失败");
				return broadcastResult;
			}
			//验证交易是否合法
			ValidatorResult<TransactionValidatorResult> rs = transactionValidator.valDo(tx, null);
			if(!rs.getResult().isSuccess()) {
				throw new VerificationException(rs.getResult().getMessage());
			}
	
			//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
			boolean success = MempoolContainer.getInstace().add(tx);
			
			BroadcastResult broadcastResult = null;
			
			if(success) {
				//广播结果
				try {
					log.info("交易大小：{}", tx.baseSerialize().length);
					broadcastResult = peerKit.broadcast(tx);
					//等待广播回应
					broadcastResult.get();
					
					//成功
					if(broadcastResult.isSuccess()) {
						//更新交易记录
						transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
					}
				} catch (Exception e) {
					broadcastResult.setSuccess(false);
					broadcastResult.setMessage("广播出错，"+e.getMessage());
				}
			} else {
				broadcastResult = new BroadcastResult();
				broadcastResult.setSuccess(false);
				broadcastResult.setMessage("重复的交易，禁止广播");
			}
			return broadcastResult;
		} finally {
			locker.unlock();
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
	
	/**
	 * 交易选择
	 * 查找并返回最接近该金额的未花费的交易
	 * @param amount 金额
	 * @param address 账户地址
	 * @return List<TransactionOutput>
	 */
	public List<TransactionOutput> selectNotSpentTransaction(Coin amount, Address address) {
		
		//获取到所有未花费的交易
		List<TransactionOutput> outputs = transactionStoreProvider.getNotSpentTransactionOutputs(address.getHash160());
		
		//选择结果存放列表
		List<TransactionOutput> thisOutputs = new ArrayList<TransactionOutput>();
		
		if(outputs == null || outputs.size() == 0) {
			return thisOutputs;
		}
		
		//遍历选择，原则是尽量少的数据，也就是笔数最少
		
		//小于amount的集合
		List<TransactionOutput> lessThanList = new ArrayList<TransactionOutput>();
		//大于amount的集合
		List<TransactionOutput> moreThanList = new ArrayList<TransactionOutput>();
		
		for (TransactionOutput transactionOutput : outputs) {
			if(transactionOutput.getValue() == amount.value) {
				//如果刚好相等，则立即返回
				thisOutputs.add(transactionOutput);
				return thisOutputs;
			} else if(transactionOutput.getValue() > amount.value) {
				//加入大于集合
				moreThanList.add(transactionOutput);
			} else {
				//加入小于于集合
				lessThanList.add(transactionOutput);
			}
		}
		
		if(Configure.TRANSFER_PREFERRED == 2) {
			//优先使用零钱
			transferPreferredWithSmallChange(amount, lessThanList, moreThanList, thisOutputs);
		} else {
			//以交易数据小优先，该种机制尽量选择一笔输入，默认方式
			transferPreferredWithLessNumber(amount, lessThanList, moreThanList, thisOutputs);
		}
		//依然按照交易时间排序
		if(thisOutputs.size() > 0) {
			Collections.sort(thisOutputs, new Comparator<TransactionOutput>() {
				@Override
				public int compare(TransactionOutput o1, TransactionOutput o2) {
					return o1.getParent().getTime() > o2.getParent().getTime() ? 1:-1;
				}
			});
		}
		return thisOutputs;
	}
	
	/*
	 * 交易选择 -- 优先使用零钱
	 */
	private void transferPreferredWithSmallChange(Coin amount, List<TransactionOutput> lessThanList,
			List<TransactionOutput> moreThanList, List<TransactionOutput> thisOutputs) {
		if(lessThanList.size() > 0) {
			//计算所有零钱，是否足够
			Coin lessTotal = Coin.ZERO;
			for (TransactionOutput transactionOutput : lessThanList) {
				lessTotal = lessTotal.add(Coin.valueOf(transactionOutput.getValue()));
			}
			
			if(lessTotal.isLessThan(amount)) {
				//不够，那么必定有大的
				selectOneOutput(moreThanList, thisOutputs);
			} else {
				//选择零钱
				selectSmallChange(amount, lessThanList, thisOutputs);
			}
		} else {
			//没有比本次交易最大的未输出交易
			selectOneOutput(moreThanList, thisOutputs);
		}
	}

	/*
	 * 交易选择 -- 以交易数据小优先，该种机制尽量选择一笔输入
	 */
	private void transferPreferredWithLessNumber(Coin amount, List<TransactionOutput> lessThanList, List<TransactionOutput> moreThanList, List<TransactionOutput> outputs) {
		if(moreThanList.size() > 0) {
			//有比本次交易大的未输出交易，直接使用其中最小的一个
			selectOneOutput(moreThanList, outputs);
		} else {
			//没有比本次交易最大的未输出交易
			selectSmallChange(amount, lessThanList, outputs);
		}
	}

	/*
	 * 选择列表里面金额最小的一笔作为输出
	 */
	private void selectOneOutput(List<TransactionOutput> moreThanList, List<TransactionOutput> outputs) {
		if(moreThanList == null || moreThanList.size() == 0) {
			return;
		}
		Collections.sort(moreThanList, new Comparator<TransactionOutput>() {
			@Override
			public int compare(TransactionOutput o1, TransactionOutput o2) {
				return o1.getValue() > o2.getValue() ? 1:-1;
			}
		});
		outputs.add(moreThanList.get(0));
	}

	/*
	 * 选择零钱，原则是尽量少的找钱，尽量少的使用输出笔数
	 */
	private void selectSmallChange(Coin amount, List<TransactionOutput> lessThanList, List<TransactionOutput> outputs) {
		if(lessThanList == null || lessThanList.size() == 0) {
			return;
		}
		//排序
		Collections.sort(lessThanList, new Comparator<TransactionOutput>() {
			@Override
			public int compare(TransactionOutput o1, TransactionOutput o2) {
				return o1.getValue() > o2.getValue() ? 1:-1;
			}
		});
		
		//已选择的金额
		Coin total = Coin.ZERO;
		//从小到大选择
		for (TransactionOutput transactionOutput : lessThanList) {
			outputs.add(transactionOutput);
			total = total.add(Coin.valueOf(transactionOutput.getValue()));
			if(total.isGreaterThan(amount)) {
				//判断是否可以移除最小的几笔交易
				List<TransactionOutput> removeList = new ArrayList<TransactionOutput>();
				for (TransactionOutput to : outputs) {
					total = total.subtract(Coin.valueOf(to.getValue()));
					if(total.isGreaterThan(amount)) {
						removeList.add(to);
					} else {
						break;
					}
				}
				if(removeList.size() > 0) {
					outputs.removeAll(removeList);
				}
				break;
			}
		}
	}
	
	/**
	 * 初始化一个普通帐户
	 * @return Address
	 * @throws Exception 
	 */
	public Address createNewAccount() throws Exception {
		
		locker.lock();
		try {
			
//			ECKey key = ECKey.fromPrivate(new BigInteger(""));
			ECKey key = new ECKey();
			
			Address address = Address.fromP2PKHash(network, network.getSystemAccountVersion(), Utils.sha256hash160(key.getPubKey(false)));
			
			address.setBalance(Coin.ZERO);
			address.setUnconfirmedBalance(Coin.ZERO);
			
			Account account = new Account(network);
			
			account.setPriSeed(key.getPrivKeyBytes());
			account.setAccountType(address.getVersion());
			account.setAddress(address);
			account.setMgPubkeys(new byte[][] {key.getPubKey(true)});
			account.signAccount(key, null);
			
			File accountFile = new File(accountDir, address.getBase58()+".dat");
			
			FileOutputStream fos = new FileOutputStream(accountFile);
			try {
				//数据存放格式，type+20字节的hash160+私匙长度+私匙+公匙长度+公匙，钱包加密后，私匙是
				fos.write(account.serialize());
			} finally {
				fos.close();
			}
			
			account.setEcKey(key);
			accountList.add(account);
			
			return address;
		} finally {
			locker.unlock();
		}
	}

	/**
	 * 初始化一个认证帐户
	 * @param mgPw			帐户管理密码
	 * @param trPw  		帐户交易密码
	 * @param accountBody   帐户信息
	 * @return Address
	 * @throws Exception 
	 */
	public Account createNewCertAccount(String mgPw, String trPw, AccountBody accountBody) throws Exception {
		
		//密码位数和难度检测
		if(!validPassword(mgPw) || !validPassword(trPw)) {
			throw new VerificationException("密码需6位或以上，且包含字母和数字");
		}
		
		//强制交易密码和帐户管理密码不一样
		Utils.checkState(!mgPw.equals(trPw), "账户管理密码和交易密码不能一样");
		
		locker.lock();
		try {
			Account account = genAccountInfos(mgPw, trPw, accountBody);
			return account;
		} finally {
			locker.unlock();
		}
	}

	/**
	 * 生成帐户信息
	 */
	public Account genAccountInfos(String mgPw, String trPw, AccountBody accountBody) throws FileNotFoundException, IOException {
		//生成新的帐户信息
		//生成私匙公匙对
		ECKey key = new ECKey();
		//取生成的未压缩的公匙做为该帐户的永久私匙种子
		byte[] prikeySeed = key.getPubKey(false);
		
		//生成账户管理的私匙
		BigInteger mgPri1 = AccountTool.genPrivKey1(prikeySeed, mgPw.getBytes());
		//生成交易的私匙
		BigInteger trPri1 = AccountTool.genPrivKey1(prikeySeed, trPw.getBytes());
		
		BigInteger mgPri2 = AccountTool.genPrivKey2(prikeySeed, mgPw.getBytes());
		BigInteger trPri2 = AccountTool.genPrivKey2(prikeySeed, trPw.getBytes());

		//默认生成一个系统帐户

		//随机生成一个跟前面没关系的私匙公匙对，用于产出地址
		ECKey addressKey = new ECKey();
		//以base58的帐户地址来命名帐户文件
		Address address = AccountTool.newAddress(network, network.getCertAccountVersion(), addressKey);

		ECKey mgkey1 = ECKey.fromPrivate(mgPri1);
		ECKey mgkey2 = ECKey.fromPrivate(mgPri2);
		
		ECKey trkey1 = ECKey.fromPrivate(trPri1);
		ECKey trkey2 = ECKey.fromPrivate(trPri2);
		
		//帐户信息
		Account account = new Account();
		account.setStatus((byte) 0);
		account.setAccountType(network.getSystemAccountVersion());
		account.setAddress(address);
		account.setPriSeed(key.getPubKey(true)); //存储压缩后的种子私匙
		account.setMgPubkeys(new byte[][] {mgkey1.getPubKey(true), mgkey2.getPubKey(true)});	//存储帐户管理公匙
		account.setTrPubkeys(new byte[][] {trkey1.getPubKey(true), trkey2.getPubKey(true)});//存储交易公匙
		
		if(accountBody == null) {
			account.setBody(AccountBody.empty());
		} else {
			account.setBody(accountBody);
		}
		
		//签名帐户
		account.signAccount(mgkey1, mgkey2);
		
		File accountFile = new File(accountDir + File.separator + "gen",  address.getBase58()+".dat");
		if(!accountFile.getParentFile().exists()) {
			accountFile.getParentFile().mkdir();
		}
		
		FileOutputStream fos = new FileOutputStream(accountFile);
		try {
			//数据存放格式，type+20字节的hash160+私匙长度+私匙+公匙长度+公匙，钱包加密后，私匙是
			fos.write(account.serialize());
		} finally {
			fos.close();
		}
		//广播帐户注册消息
//		broadcastAccountReg(account, mgPw);
		//TODO
		return account;
	}
	
	/*
	 * 广播帐户注册消息至全网
	 * 
	 */
	private void broadcastAccountReg(Account account, String pwd) {
		//TODO
		CertAccountRegisterTransaction tx = new CertAccountRegisterTransaction(network, account.getAddress().getHash160(), account.getMgPubkeys(), account.getTrPubkeys(), account.getBody());
		//根据密码计算出私匙
		ECKey seedPri = ECKey.fromPublicOnly(account.getPriSeed());
		byte[] seedPribs = seedPri.getPubKey(false);
		
		tx.calculateSignature(Sha256Hash.ZERO_HASH, ECKey.fromPrivate(AccountTool.genPrivKey1(seedPribs, pwd.getBytes())), 
				ECKey.fromPrivate(AccountTool.genPrivKey2(seedPribs, pwd.getBytes())));
		
		tx.verifyScript();
		
//		//序列化和反序列化
//		byte[] txContent = tx.baseSerialize();
//		
//		RegisterTransaction tx1 = new RegisterTransaction(network, txContent);
//		tx1.verfifyScript();
		
		if(log.isDebugEnabled()) {
			log.debug("accreg tx id : {}", tx.getHash());
			log.debug("accreg tx content : {}", Hex.encode(tx.baseSerialize()));
		}
		peerKit.broadcastMessage(tx);
	}
	
	/**
	 * 备份钱包
	 * @param backupFilePath 备份文件路径
	 * @return Result 成功则result.message返回备份文件的完整路径
	 * @throws IOException 
	 */
	public Result backupWallet(String backupFilePath) throws IOException {
		//目录是否存在，不存在则创建，如果传入的是一个目录，则自动生成备份的文件名
		if(StringUtils.isEmpty(backupFilePath)) {
			return new Result(false, "备份路径为空");
		}
		//账户存在才能备份
		if(accountList == null || accountList.size() == 0) {
			log.warn("系统内没有可备份的账户");
			return new Result(false, "系统内没有可备份的账户");
		}
		File backupFile = new File(backupFilePath);
		//判断上级目录是否存在，不存在则创建
		if(!backupFile.getParentFile().exists() && !backupFile.getParentFile().mkdirs()) {
			return new Result(false, "创建目录失败");
		}
		//如果传入的文件夹，则生成备份文件
		if(backupFile.isDirectory()) {
			if(!backupFile.exists() && !backupFile.mkdir()) {
				return new Result(false, "创建目录失败");
			}
			backupFile = new File(backupFile, "wallet_backup_".concat(DateUtil.convertDate(new Date(TimeService.currentTimeMillis()), "yyyyMMddHHmm")).concat(".dat"));
		}
		//创建备份文件
		if(!backupFile.exists() && !backupFile.createNewFile()) {
			return new Result(false, "创建文件失败");
		}
		//备份账户
		FileOutputStream fos = new FileOutputStream(backupFile);
		try {
			for (Account account : accountList) {
				fos.write(account.serialize());
			}
			return new Result(true, backupFile.getAbsolutePath());
		} finally {
			fos.close();
		}
	}
	
	/**
	 * 导入钱包
	 * @param walletFilePath 钱包文件路径
	 * @return boolean 是否导入成功
	 * @throws IOException 
	 */
	public Result importWallet(String walletFilePath) throws IOException {
		//导入的文件路径不能为空
		if(StringUtils.isEmpty(walletFilePath)) {
			return new Result(false, "导入的文件路径为空");
		}
		File walletFile = new File(walletFilePath);
		//判断将要导入的钱包文件是否存在
		if(!walletFile.exists()) {
			return new Result(false, "要导入的钱包文件不存在");
		}
		//覆盖账户
		FileInputStream fis = new FileInputStream(walletFile);
		try {
			byte[] datas = new byte[fis.available()];
			fis.read(datas);
	
			int index = 0;
			//导入的账户列表
			List<Account> importAccountList = new ArrayList<Account>();
			while(index < datas.length) {
				Account ac = Account.parse(datas, index, network);
				index += ac.serialize().length;
				try {
					//验证不通过的忽略
					ac.verify();
					importAccountList.add(ac);
				} catch (Exception e) {
					log.warn("导入{}时出错", ac.getAddress().getBase58(), e);
				}
			}
			
			if(importAccountList.size() == 0) {
				return new Result(false, "导入了0个账户");
			}
			//备份原账户
			for (Account account : accountList) {
				String base58 = account.getAddress().getBase58();
				String newBackupFile = base58 + "_auto_backup_".concat(DateUtil.convertDate(new Date(TimeService.currentTimeMillis()), "yyyyMMddHHmmss")).concat(".dat.temp");
				new File(accountDir, base58 + ".dat")
					.renameTo(new File(accountDir, newBackupFile));
			}
			for (Account account : importAccountList) {
				File accountFile = new File(accountDir, account.getAddress().getBase58()+".dat");
				
				FileOutputStream fos = new FileOutputStream(accountFile);
				try {
					fos.write(account.serialize());
				} finally {
					fos.close();
				}
			}
			//重新加载账户
			loadAccount();
			//更新余额
			loadBalanceFromChainstateAndUnconfirmedTransaction(getAccountHash160s());
			return new Result(true, "成功导入了"+importAccountList.size()+"个账户");
		} catch (Exception e) {
			log.error("导入钱包失败，{}", e.getMessage(), e);
			return new Result(false, "导入钱包失败,"+e.getMessage());
		} finally {
			fis.close();
		}
	}
	
	/**
	 * 加密钱包
	 * @param password  密码
	 * @return Result
	 */
	public Result encryptWallet(String password) {
		//密码位数和难度检测
		if(!validPassword(password)) {
			return new Result(false, "输入的密码需6位或以上，且包含字母和数字");
		}
		
		int successCount = 0; //成功个数
		//加密钱包
		for (Account account : accountList) {
			//判断是否已经加密了
			if(account.isEncrypted()) {
				continue;
			}
			ECKey eckey = account.getEcKey();
			try {
				ECKey newKey = eckey.encrypt(password);
				account.setEcKey(newKey);
				account.setPriSeed(newKey.getEncryptedPrivateKey().getEncryptedBytes());
				
				//重新签名
				account.signAccount(eckey, null);
				
				//回写到钱包文件
				File accountFile = new File(accountDir, account.getAddress().getBase58()+".dat");
				
				FileOutputStream fos = new FileOutputStream(accountFile);
				try {
					//数据存放格式，type+20字节的hash160+私匙长度+私匙+公匙长度+公匙，钱包加密后，私匙是
					fos.write(account.serialize());
					successCount++;
				} finally {
					fos.close();
				}
			} catch (Exception e) {
				log.error("加密 {} 失败: {}", account.getAddress().getBase58(), e.getMessage(), e);
				return new Result(false, String.format("加密 %s 失败: %s", account.getAddress().getBase58(), e.getMessage()));
			} finally {
				eckey = null;
			}
		}
		String message = null;
		if(successCount > 0) {
			message = "成功加密"+successCount+"个账户";
		} else {
			message = "账户已加密，无需重复加密";
		}
		return new Result(true, message);
	}
	
	/**
	 * 解密钱包
	 * @param password  密码
	 * @return Result
	 */
	public Result decryptWallet(String password) {
		return decryptWallet(password, Definition.TX_VERIFY_MG);
	}
	
	/**
	 * 解密钱包
	 * @param password  密码
	 * @param type  1账户管理私钥 ，2交易私钥
	 * @return Result
	 */
	public Result decryptWallet(String password, int type) {
		//密码位数和难度检测
		if(!validPassword(password)) {
			return new Result(false, "密码错误");
		}
		for (Account account : accountList) {
			if(account.getAccountType() == network.getSystemAccountVersion()) {
				//普通账户的解密
				account.resetKey(password);
				ECKey eckey = account.getEcKey();
				try {
					account.setEcKey(eckey.decrypt(password));
				} catch (Exception e) {
					log.error("解密失败, "+e.getMessage(), e);
					account.setEcKey(eckey);
					return new Result(false, "密码错误");
				}
			} else if(account.getAccountType() == network.getCertAccountVersion()) {
				//认证账户的解密
				ECKey[] keys = null;
				if(type == Definition.TX_VERIFY_MG) {
					keys = account.decryptionMg(password);
				} else {
					keys = account.decryptionTr(password);
				}
				if(keys == null) {
					return new Result(false, "密码错误");
				}
			}
		}
		return new Result(true, "解密成功");
	}
	
	/**
	 * 修改钱包密码
	 * 如果没有加密的账户，会被新密码加密
	 * @param oldPassword   原密码
	 * @param newPassword 	新密码
	 * @return Result
	 */
	public Result changeWalletPassword(String oldPassword, String newPassword) {
		return changeWalletPassword(oldPassword, newPassword, 1);
	}
	
	/**
	 * 修改认证账户的密码
	 * @param oldPassword	旧密码
	 * @param newPassword	新密码
	 * @param type  1账户管理私钥 ，2交易私钥
	 * @return Result
	 */
	public Result changeWalletPassword(String oldPassword, String newPassword, int type) {
		//密码位数和难度检测
		if(!validPassword(oldPassword) || !validPassword(newPassword)) {
			return new Result(false, "密码需6位或以上，且包含字母和数字");
		}
		
		//先解密
		//如果修改认证账户，如果修改的是账户管理密码，这里的原密码就是账户管理密码 ，如果修改的是交易密码，这里的原密码也是账户管理密码，因为必须要账户管理密码才能修改
		Result res = decryptWallet(oldPassword);
		if(!res.isSuccess()) {
			return res;
		}
		
		int successCount = 0; //成功个数
		//加密钱包
		for (Account account : accountList) {
			try {
				if(account.isCertAccount()) {
					
					//认证账户
					//生成私匙
					ECKey seedPri = ECKey.fromPublicOnly(account.getPriSeed());
					byte[] seedPribs = seedPri.getPubKey(false);
					
					BigInteger pri1 = AccountTool.genPrivKey1(seedPribs, newPassword.getBytes());
					BigInteger pri2 = AccountTool.genPrivKey2(seedPribs, newPassword.getBytes());
					
					ECKey key1 = ECKey.fromPrivate(pri1);
					ECKey key2 = ECKey.fromPrivate(pri2);
					
					//重新设置账户的公钥
					ECKey[] oldMgEckeys = account.getMgEckeys();
					if(type == 1) {
						account.setMgEckeys(new ECKey[] {key1, key2});
						account.setMgPubkeys(new byte[][] {key1.getPubKey(true), key2.getPubKey(true)});
					} else {
						account.setTrPubkeys(new byte[][] {key1.getPubKey(true), key2.getPubKey(true)});
					}
					//重新签名
					account.signAccount();
					account.verify();
					
					//广播
					CertAccountUpdateTransaction rtx = new CertAccountUpdateTransaction(network, account.getAddress().getHash160(), 
							account.getMgPubkeys(), account.getTrPubkeys(), account.getBody());
					
					rtx.calculateSignature(account.getAccountTransaction().getHash(), oldMgEckeys[0], oldMgEckeys[1], account.getAddress().getHash160(), Definition.TX_VERIFY_MG);
					rtx.verify();
					rtx.verifyScript();

					MempoolContainer.getInstace().add(rtx);
					
					BroadcastResult broadcastResult = peerKit.broadcast(rtx).get();
					if(broadcastResult.isSuccess()) {
						account.setAccountTransaction(rtx);
						//回写到钱包文件
						File accountFile = new File(accountDir, account.getAddress().getBase58()+".dat");
						FileOutputStream fos = new FileOutputStream(accountFile);
						try {
							//数据存放格式，type+20字节的hash160+私匙长度+私匙+公匙长度+公匙，钱包加密后，私匙是
							fos.write(account.serialize());
							successCount++;
						} finally {
							fos.close();
						}
					} else {
						log.error(broadcastResult.getMessage());
					}
				} else {
					//普通账户，也就无所谓管理或者交易密码了
					ECKey eckey = account.getEcKey();
					ECKey newKey = eckey.encrypt(newPassword);
					account.setEcKey(newKey);
					account.setPriSeed(newKey.getEncryptedPrivateKey().getEncryptedBytes());
					
					//重新签名
					account.signAccount(eckey, null);
					
					account.verify();
					
					//回写到钱包文件
					File accountFile = new File(accountDir, account.getAddress().getBase58()+".dat");
					
					FileOutputStream fos = new FileOutputStream(accountFile);
					try {
						//数据存放格式，type+20字节的hash160+私匙长度+私匙+公匙长度+公匙，钱包加密后，私匙是
						fos.write(account.serialize());
						successCount++;
					} finally {
						fos.close();
					}
					eckey = null;
				}
			} catch (Exception e) {
				log.error("加密 {} 失败: {}", account.getAddress().getBase58(), e.getMessage(), e);
				return new Result(false, String.format("加密 %s 失败: %s", account.getAddress().getBase58(), e.getMessage()));
			} finally {
				account.resetKey();
			}
		}
		String message = "成功修改"+successCount+"个账户的密码";
		return new Result(true, message);
	}
	
	/**
	 * 加载现有的帐户
	 * @throws IOException
	 */
	public void loadAccount() throws IOException {
		this.accountList.clear();
		
		File accountDirFile = new File(accountDir);

		if(!accountDirFile.exists() || !accountDirFile.isDirectory()) {
			throw new IOException("account base dir not exists");
		}
		
		//加载帐户目录下的所有帐户
		File[] accountFiles = accountDirFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".dat");
			}
		});
		
		for (File accountFile : accountFiles) {
			if(accountFile.isDirectory()) {
				continue;
			}
			//读取私匙
			FileInputStream fis = new FileInputStream(accountFile);
			try {
				byte[] datas = new byte[fis.available()];
				fis.read(datas);
				Account account = Account.parse(datas, network);
				if(account == null) {
					log.warn("parse account err, file {}", accountFile);
					continue;
				}
				//验证帐户
				account.verify();
				
				accountList.add(account);
				
				if(log.isDebugEnabled()) {
					log.debug("load account {} success", account.getAddress().getBase58());
				}
			} catch (VerificationException e) {
				log.warn("read account file {} err", accountFile);
				throw e;
			} finally {
				fis.close();
			}
			
		}
		//判断账户不存在时是否自动创建
		if(accountList.size() == 0 && Configure.ACCOUNT_AUTO_INIT) {
			try {
				createNewAccount();
			} catch (Exception e) {
				log.error("自动初始化账户失败", e);
			}
		}
		
		//加载账户信息
		List<byte[]> hash160s = getAccountHash160s();
		
		//初始化账户交易过滤器
		initAccountFilter(hash160s);
		
		//或许重新加载账户相关的交易记录
		maybeReLoadTransaction(hash160s);
		
		//加载各地址的余额
		loadBalanceFromChainstateAndUnconfirmedTransaction(hash160s);
		
		//加载认证账户信息对应的最新的账户信息交易
		loadAccountInfosNewestTransaction();
	}

	//加载认证账户信息对应的最新的交易记录
	private void loadAccountInfosNewestTransaction() {
		for (Account account : accountList) {
			if(account.isCertAccount()) {
				account.setAccountTransaction(transactionStoreProvider.getAccountInfosNewestTransaction(account.getAddress().getHash160()));
			}
		}
	}

	//是否重新加载账户交易
	private void maybeReLoadTransaction(List<byte[]> hash160s) {
		
		//判断上次加载的和本次的账户是否完全一致
		List<byte[]> hash160sStore = transactionStoreProvider.getAddresses();

		//如果个数一样，则判断是否完全相同
		if(hash160s.size() == hash160sStore.size()) {
			Comparator<byte[]> comparator = new Comparator<byte[]>() {
				@Override
				public int compare(byte[] o1, byte[] o2) {
					return Hex.encode(o1).compareTo(Hex.encode(o2));
				}
			};
			Collections.sort(hash160s, comparator);
			Collections.sort(hash160sStore, comparator);
			boolean fullSame = true;
			for (int i = 0; i < hash160s.size(); i++) {
				if(!Arrays.equals(hash160sStore.get(i), hash160s.get(i))) {
					fullSame = false;
					break;
				}
			}
			if(fullSame) {
				return;
			}
		}
		transactionStoreProvider.reloadTransaction(hash160s);
	}

	//初始化账户交易过滤器
	private void initAccountFilter(List<byte[]> hash160s) {
		blockStoreProvider.initAccountFilter(hash160s);
	}
	
	//获取账户对应的has160
	private List<byte[]> getAccountHash160s() {
		CopyOnWriteArrayList<byte[]> hash160s = new CopyOnWriteArrayList<byte[]>();
		for (Account account : accountList) {
			Address address = account.getAddress();
			byte[] hash160 = address.getHash160();
			
			hash160s.add(hash160);
		}
		return hash160s;
	}

	//如果钱包目录不存在则创建
	private void maybeCreateAccountDir() throws IOException {
		//检查账户目录是否存在
		File accountDirFile = new File(accountDir);
		if(!accountDirFile.exists() || !accountDirFile.isDirectory()) {
			accountDirFile.mkdir();
		}
	}
	
	/*
	 * 从状态链（未花费的地址集合）和未确认的交易加载余额
	 */
	private void loadBalanceFromChainstateAndUnconfirmedTransaction(List<byte[]> hash160s) {
		
		try {
			for (Account account : accountList) {
				Address address = account.getAddress();
				loadAddressBalance(address);
			}
		}catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	//加载单个地址的余额信息
	private void loadAddressBalance(Address address) {
		//查询可用余额和等待中的余额
		Coin[] balances = transactionStoreProvider.getBalanceAndUnconfirmedBalance(address.getHash160());
		
		address.setBalance(balances[0]);
		address.setUnconfirmedBalance(balances[1]);
	}
	
	/**
	 * 获取账户列表，其中包含了余额信息
	 * 如果有冻结余额，那么重新加载一次，因为冻结的余额由可能发生变法
	 * @return
	 */
	public List<Account> getAccountList() {
		//如果某个账户有冻结余额，则重新加载
		for (Account account : accountList) {
			Address address = account.getAddress();
			if(address.getUnconfirmedBalance().isGreaterThan(Coin.ZERO)) {
				loadAddressBalance(address);
			}
		}
		return accountList;
	}
	
	public void clearAccountList() {
		accountList.clear();;
	}
	
	/*
	 * 初始化监听器
	 */
	private void initListeners() {
		TransactionListener tl = new TransactionListener() {
			@Override
			public void newTransaction(TransactionStore tx) {
				//更新余额
				loadBalanceFromChainstateAndUnconfirmedTransaction(getAccountHash160s());
				if(transactionListener != null) {
					transactionListener.newTransaction(tx);
				}
			}
		};
		transactionStoreProvider.setTransactionListener(tl);
	}
	
	/**
	 * 设置新交易监听器
	 * @param transactionListener
	 */
	public void setTransactionListener(TransactionListener transactionListener) {
		this.transactionListener = transactionListener;
	}
	
	public TransactionListener getTransactionListener() {
		return transactionListener;
	}
	
	/**
	 * 设置通知监听器
	 * @param noticeListener
	 */
	public void setNoticeListener(NoticeListener noticeListener) {
		transactionStoreProvider.setNoticeListener(noticeListener);
	}
	
	/**
	 * 校验密码难度
	 * @param password
	 * @return boolean
	 */
	public static boolean validPassword(String password) {
		if(StringUtils.isEmpty(password)){  
            return false;  
        } 
		if(password.length() < 6){  
            return false;  
        }  
        if(password.matches("(.*)[a-zA-z](.*)") && password.matches("(.*)\\d+(.*)")){  
            return true;  
        } else {
        	return false;
        }
	}
	
	/**
	 * 判断账户实际已加密
	 * 规则，只要有一个账户已加密，则代表已加密 ，因为不能用多个密码加密不同的账户，这样用户管理起来非常麻烦
	 * @return boolean
	 */
	public boolean accountIsEncrypted() {
		return accountIsEncrypted(1);
	}

	
	/**
	 * 判断账户实际已加密
	 * 规则，只要有一个账户已加密，则代表已加密 ，因为不能用多个密码加密不同的账户，这样用户管理起来非常麻烦
	 * @param type  1账户管理私钥 ，2交易私钥
	 * @return boolean
	 */
	public boolean accountIsEncrypted(int type) {
		for (Account account : accountList) {
			if(!account.isCertAccount() && account.isEncrypted()) {
				return true;
			} else if(account.isCertAccount()) {
				if(type == Definition.TX_VERIFY_MG && account.isEncryptedOfMg()) {
					return true;
				} else if(type == Definition.TX_VERIFY_TR && account.isEncryptedOfTr()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 重新设置账户的私钥
	 */
	public void resetKeys() {
		for (Account account : accountList) {
			account.resetKey();
		}
	}

	/**
	 * 账户是否是认证账户
	 * @return boolean
	 */
	public boolean isCertAccount() {
		for (Account account : accountList) {
			if(account.isCertAccount()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取认证账户列表
	 * @return List<AccountStore>
	 */
	public List<AccountStore> getCertAccounts() {
		return getCertAccounts(null);
	}
	
	/**
	 * 获取认证账户列表
	 * @param certAccountList   //是否重新获取
	 * @return List<AccountStore>
	 */
	public List<AccountStore> getCertAccounts(List<AccountStore> certAccountList) {
		byte[] certAccounts = chainstateStoreProvider.getBytes(Configure.CERT_ACCOUNT_KEYS);
		if(certAccountList != null && certAccountList.size() == certAccounts.length / Address.LENGTH) {
			//没有变化，则直接返回
			return certAccountList;
		}
		certAccountList = new ArrayList<AccountStore>();
		if(certAccounts == null) {
			return certAccountList;
		}
		for (int i = 0; i < certAccounts.length; i+=Address.LENGTH) {
			byte[] hash160 = Arrays.copyOfRange(certAccounts, i, i + Address.LENGTH);
			AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountStore != null) {
				certAccountList.add(accountStore);
			}
		}
		return certAccountList;
	}
	
	/**
	 * 获取共识账户列表
	 * @return List<AccountStore>
	 */
	public List<AccountStore> getConsensusAccounts() {
		byte[] consensusAccounts = chainstateStoreProvider.getBytes(Configure.CONSENSUS_ACCOUNT_KEYS);
		List<AccountStore> consensusAccountList = new ArrayList<AccountStore>();
		if(consensusAccounts == null) {
			return consensusAccountList;
		}
		for (int i = 0; i < consensusAccounts.length; i+=Address.LENGTH) {
			byte[] hash160 = Arrays.copyOfRange(consensusAccounts, i, i + Address.LENGTH);
			AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountStore == null) {
				continue;
			}
			consensusAccountList.add(accountStore);
		}
		return consensusAccountList;
	}

	/**
	 * 获取自己的账户信息
	 * @return AccountStore
	 */
	public AccountStore getAccountInfo() {
		List<byte[]> hash160s = getAccountHash160s();
		if(hash160s == null || hash160s.size() == 0) {
			return null;
		}
		byte[] hash160 = hash160s.get(0);
		AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
		if(accountStore == null) {
			accountStore = new AccountStore(network);
			accountStore.setAccountBody(AccountBody.empty());
			accountStore.setCert(0);
			accountStore.setHash160(hash160);
			accountStore.setType(network.getSystemAccountVersion());
			accountStore.setBalance(getBalance().value);
		}
		return accountStore;
	}

	/**
	 * 检查当前账户是否在共识中状态
	 * @return boolean
	 */
	public boolean checkConsensusing() {
		if(accountList == null || accountList.size() == 0) {
			return false;
		}
		for (Account account : accountList) {
			if(consensusPoolCacher.contains(account.getAddress().getHash160())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 注册成为共识节点
	 * @return Result
	 */
	public Result registerConsensus() {
		//选取第一个可注册共识的账户进行广播
		try {
			for (Account account : accountList) {
				AccountStore accountStore = chainstateStoreProvider.getAccountInfo(account.getAddress().getHash160());
				if((accountStore != null && accountStore.getCert() >= Configure.CONSENSUS_CREDIT)
						|| (Configure.CONSENSUS_CREDIT == 0l && accountStore == null)) {
					RegConsensusTransaction regConsensus = new RegConsensusTransaction(network, Definition.VERSION, TimeService.currentTimeMillis());
					regConsensus.sign(account);
					
					regConsensus.verify();
					regConsensus.verifyScript();
					
					//加入内存池
					MempoolContainer.getInstace().add(regConsensus);
					
					BroadcastResult broadcastResult = peerKit.broadcast(regConsensus).get();
					if(broadcastResult.isSuccess()) {
						return new Result(true, "注册为共识节点请求已成功发送到网络");
					} else {
						MempoolContainer.getInstace().remove(regConsensus.getHash());
					}
				}
			}
		} catch (Exception e) {
			return new Result(false, "共识请求出错");
		}
		return new Result(false, "没有可参与共识的账户");
	}

	/**
	 * 退出共识
	 * @return Result
	 */
	public Result quitConsensus() {
		//选取共识中的账户进行广播
		try {
			for (Account account : accountList) {
				if(consensusPoolCacher.contains(account.getAddress().getHash160())) {
					RemConsensusTransaction remConsensus = new RemConsensusTransaction(network, Definition.VERSION, TimeService.currentTimeMillis());
					remConsensus.sign(account);
					
					remConsensus.verify();
					remConsensus.verifyScript();
					
					//加入内存池
					MempoolContainer.getInstace().add(remConsensus);
					
					BroadcastResult broadcastResult = peerKit.broadcast(remConsensus).get();
					if(broadcastResult.isSuccess()) {
						return new Result(true, "退出共识请求已成功发送到网络");
					} else {
						MempoolContainer.getInstace().remove(remConsensus.getHash());
					}
				}
			}
		} catch (Exception e) {
			return new Result(false, e.getMessage());
		}
		return new Result(false, "没有共识中的账户");
	}
}
