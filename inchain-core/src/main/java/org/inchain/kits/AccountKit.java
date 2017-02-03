package org.inchain.kits;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.TimeHelper;
import org.inchain.core.BroadcastResult;
import org.inchain.core.BroadcasterComponent;
import org.inchain.core.exception.MoneyNotEnoughException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.listener.NoticeListener;
import org.inchain.listener.TransactionListener;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.Message;
import org.inchain.network.NetworkParams;
import org.inchain.script.ScriptBuilder;
import org.inchain.signers.LocalTransactionSigner;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.StoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.validator.ValidatorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	private StoreProvider chainstateStoreProvider;
	//交易存储服务
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionValidator transactionValidator;
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
	 * 获取余额
	 */
	public Coin getBalance() {
		if(accountList == null || accountList.size() == 0) {
			return Coin.ZERO;
		}
		return getBalance(accountList.get(0));
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
		return getCanUseBalance(accountList.get(0).getAddress());
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
		return getCanNotUseBalance(accountList.get(0).getAddress());
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
	 * 获取交易列表
	 */
	public void getTransaction(String accountId) {
		
	}
	
	/**
	 * 发送普通交易到指定地址
	 * @param to   base58的地址
	 * @param money	发送金额
	 * @param fee	手续费
	 * @return String
	 * @throws MoneyNotEnoughException
	 */
	public String sendMoney(String to, Coin money, Coin fee) throws MoneyNotEnoughException {
		//参数不能为空
		Utils.checkNotNull(to);
		
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
		
		//当前余额
		Account account = accountList.get(0);
		Address myAddress = account.getAddress();
		
		//可用余额
		Coin balance = myAddress.getBalance();
		
		//检查余额是否充足
		if(money.add(fee).compareTo(balance) > 0) {
			throw new MoneyNotEnoughException("余额不足");
		}
		
		Transaction tx = new Transaction(network);
		tx.setTime(TimeHelper.currentTimeMillis());
		tx.setLockTime(TimeHelper.currentTimeMillis());
		tx.setType(TransactionDefinition.TYPE_PAY);
		tx.setVersion(TransactionDefinition.VERSION);
		
		Coin totalInputCoin = Coin.ZERO;
		
		ECKey key = ECKey.fromPrivate(new BigInteger(account.getPriSeed()));
		
		//选择输入
		List<TransactionOutput> fromOutputs = selectNotSpentTransaction(money.add(fee), myAddress);
		
		for (TransactionOutput output : fromOutputs) {
			TransactionInput input = new TransactionInput(output);
			//创建一个输入的空签名
			input.setScriptSig(ScriptBuilder.createInputScript(null, key));
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
		signer.signInputs(tx, key);

		//验证交易是否合法
		ValidatorResult<TransactionValidatorResult> rs = transactionValidator.valDo(tx, null);
		if(!rs.getResult().isSuccess()) {
			throw new MoneyNotEnoughException(rs.getResult().getMessage());
		}

		//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
		boolean success = MempoolContainerMap.getInstace().add(tx);
		
		if(success) {
		
			//TODO 广播结果
			try {
				BroadcastResult broadcastResult = peerKit.broadcast(tx).get();
				
				//成功
				
				//更新交易记录
				transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
				
				return tx.getHash().toString();
			} catch (Exception e) {
				return "error";
			}
		} else {
			return "error";
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
	 * 交易选择
	 * 查找并返回最接近该金额的未花费的交易
	 */
	private List<TransactionOutput> selectNotSpentTransaction(Coin amount, Address myAddress) {
		
		//获取到所有未花费的交易
		List<TransactionOutput> outputs = transactionStoreProvider.getNotSpentTransactionOutputs(myAddress.getHash160());
		
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
	 * 初始化一个认证帐户
	 * @param mgPw	帐户管理密码
	 * @param trPw  帐户交易密码
	 * @return Address
	 * @throws Exception 
	 */
	public Account createNewCertAccount(String mgPw, String trPw, byte[] body) throws Exception {
		
		Utils.checkNotNull(mgPw);
		Utils.checkNotNull(trPw);
		//强制交易密码和帐户管理密码不一样
		Utils.checkState(!mgPw.equals(trPw), "admin password and transaction password can not be the same");
		
		locker.lock();
		try {
			Account account = genAccountInfos(mgPw, trPw, body);
//			loadAccount();
			return account;
		} finally {
			locker.unlock();
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
			
			Account account = new Account();
			
			account.setPriSeed(key.getPrivKeyBytes());
			
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
			
			accountList.add(account);
			
			return address;
		} finally {
			locker.unlock();
		}
	}

	/**
	 * 生成帐户信息
	 */
	public Account genAccountInfos(String mgPw, String trPw, byte[] body) throws FileNotFoundException, IOException {
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
		
		if(body == null) {
			account.setBody(new byte[0]);
		} else {
			account.setBody(body);
		}
		
		//签名帐户
		account.signAccount(mgkey1, mgkey2);
		
		File accountFile = new File(accountDir + File.separator + "gen", address.getBase58()+".dat");
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
		
		tx.verfifyScript();
		
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

	//加载现有的帐户
	public void loadAccount() throws IOException {
		this.accountList.clear();
		
		File accountDirFile = new File(accountDir);

		if(!accountDirFile.exists() || !accountDirFile.isDirectory()) {
			throw new IOException("account base dir not exists");
		}
		
		//加载帐户目录下的所有帐户
		for (File accountFile : accountDirFile.listFiles()) {
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
				//查询可用余额和等待中的余额
				Coin[] balances = transactionStoreProvider.getBalanceAndUnconfirmedBalance(address.getHash160());
				
				address.setBalance(balances[0]);
				address.setUnconfirmedBalance(balances[1]);
			}
		}catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public List<Account> getAccountList() {
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
	
	/**
	 * 设置通知监听器
	 * @param noticeListener
	 */
	public void setNoticeListener(NoticeListener noticeListener) {
		transactionStoreProvider.setNoticeListener(noticeListener);
	}
}
