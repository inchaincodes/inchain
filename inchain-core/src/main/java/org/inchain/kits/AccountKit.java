package org.inchain.kits;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.account.Account.AccountType;
import org.inchain.core.Coin;
import org.inchain.core.exception.MoneyNotEnoughException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.network.NetworkParameters;
import org.inchain.store.StoreProvider;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.RegisterTransaction;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账户管理
 * @author ln
 *
 */
public class AccountKit {
	
	private final static Logger log = LoggerFactory.getLogger(AccountKit.class);

	private final static Lock locker = new ReentrantLock();
	
	private NetworkParameters network;
	//账户文件路径
	private String accountDir;
	private List<Account> accountList = new ArrayList<Account>();
	//状态连存储服务
	private StoreProvider chainstateStoreProvider;
	//交易存储服务
	private TransactionStoreProvider transactionStoreProvider;
	
	//节点管理器
	private PeerKit peerKit;
	
	public AccountKit(NetworkParameters network, PeerKit peerKit) throws IOException {
		
		this.network = Utils.checkNotNull(network);
		this.peerKit = Utils.checkNotNull(peerKit);
		
		//帐户信息保存于数据目录下的account目录，以account开始的dat文件，一个文件一个帐户，支持多帐户
		this.accountDir = Configure.DATA_ACCOUNT;
		
		//初始化交易存储服务，保存与帐户有关的所有交易，保存于数据目录下的transaction文件夹
		this.transactionStoreProvider = TransactionStoreProvider.getInstace(Configure.DATA_TRANSACTION, network);
		//初始化状态链存储服务，该目录保存的所有未花费的交易，保存于数据目录下的chainstate文件夹
		this.chainstateStoreProvider = TransactionStoreProvider.getInstace(Configure.DATA_CHAINSTATE, network);
		
		init();
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
		return null;
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance(String accountId) {
		return null;
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance(Account account) {
		return null;
	}
	
	/**
	 * 获取余额
	 */
	public Coin getBalance(Address address) {
		return null;
	}
	
	/**
	 * 获取余额
	 */
	public Coin getAddressBalance(String address) {
		return null;
	}
	
	/**
	 * 获取交易列表
	 */
	public void getTransaction() {
		
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
	 * @return Future
	 * @throws MoneyNotEnoughException
	 */
	public Future sendMoney(String to, Coin money, Coin fee) throws MoneyNotEnoughException {
		//参数不能为空
		Utils.checkNotNull(to);
		Utils.checkNotNull(to);
		
		//发送的金额必须大于0
		if(money.compareTo(Coin.ZERO) <= 0) {
			throw new RuntimeException("发送的金额需大于0");
		}
		if(fee == null || fee.compareTo(Coin.ZERO) < 0) {
			fee = Coin.ZERO;
		}
		//当前余额
		Coin balance = getBalance();
		//检查余额是否充足
		if(money.add(fee).compareTo(balance) > 0) {
			throw new MoneyNotEnoughException();
		}
		return null;
	}
	
	/**
	 * 初始化一个普通帐户
	 * @param mgPw	帐户管理密码
	 * @param trPw  帐户交易密码
	 * @return Address
	 * @throws Exception 
	 */
	public Address createNewAccount(String mgPw, String trPw) throws Exception {
		
		Utils.checkNotNull(mgPw);
		Utils.checkNotNull(trPw);
		//强制交易密码和帐户管理密码不一样
		Utils.checkState(!mgPw.equals(trPw), "admin password and transaction password can not be the same");
		
		locker.lock();
		try {
			Address address = genAccountInfos(mgPw, trPw);
			loadAccount();
			return address;
		} finally {
			locker.unlock();
		}
	}

	/**
	 * 生成帐户信息
	 */
	public Address genAccountInfos(String mgPw, String trPw) throws FileNotFoundException, IOException {
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
		AccountType accountType = AccountType.SYSTEM;
		
		//随机生成一个跟前面没关系的私匙公匙对，用于产出地址
		ECKey addressKey = new ECKey();
		//以base58的帐户地址来命名帐户文件
		Address address = AccountTool.newAddress(network, accountType.value(), addressKey);

		ECKey mgkey1 = ECKey.fromPrivate(mgPri1);
		ECKey mgkey2 = ECKey.fromPrivate(mgPri2);
		
		ECKey trkey1 = ECKey.fromPrivate(trPri1);
		ECKey trkey2 = ECKey.fromPrivate(trPri2);
		
		//帐户信息
		Account account = new Account();
		account.setStatus((byte) 0);
		account.setAccountType(accountType);
		account.setAddress(address);
		account.setPriSeed(key.getPubKey(true)); //存储压缩后的种子私匙
		account.setMgPubkeys(new byte[][] {mgkey1.getPubKey(true), mgkey2.getPubKey(true)});	//存储帐户管理公匙
		account.setTrPubkeys(new byte[][] {trkey1.getPubKey(true), trkey2.getPubKey(true)});//存储交易公匙
		account.setBody(new byte[0]);
		//签名帐户
		account.signAccount(mgkey1, mgkey2);
		
		File accountFile = new File(accountDir, address.getBase58()+".dat");
		
		FileOutputStream fos = new FileOutputStream(accountFile);
		try {
			//数据存放格式，type+20字节的hash160+私匙长度+私匙+公匙长度+公匙，钱包加密后，私匙是
			fos.write(account.serialize());
		} finally {
			fos.close();
		}
		//广播帐户注册消息
		broadcastAccountReg(account, mgPw);
		return address;
	}
	
	/*
	 * 广播帐户注册消息至全网
	 * 
	 */
	private void broadcastAccountReg(Account account, String pwd) {
		//TODO
		RegisterTransaction tx = new RegisterTransaction(network, account);
		//根据密码计算出私匙
		ECKey seedPri = ECKey.fromPublicOnly(account.getPriSeed());
		byte[] seedPribs = seedPri.getPubKey(false);
		
		tx.calculateSignature(ECKey.fromPrivate(AccountTool.genPrivKey1(seedPribs, pwd.getBytes())), 
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
		peerKit.broadcastTransaction(tx);
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
		//加载各地址的余额
		loadBalanceFromChainstateAndUnconfirmedTransaction();
	}
	
	/**
	 * 关闭资源
	 * @throws IOException 
	 */
	public void close() throws IOException {
		chainstateStoreProvider.close();
	}
	
	/*
	 * 初始化账户信息
	 */
	private synchronized void init() throws IOException {
		maybeCreateAccountDir();
		loadAccount();
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
	private void loadBalanceFromChainstateAndUnconfirmedTransaction() {
		for (Account account : accountList) {
			Address address = account.getAddress();
			byte[] hash160 = address.getHash160();
			//查询是否
			Coin[] balances = transactionStoreProvider.getBalanceAndUnconfirmedBalance(hash160);
			address.setBalance(balances[0]);
			address.setUnconfirmedBalance(balances[1]);
		}
	}
	
	public List<Account> getAccountList() {
		return accountList;
	}
}
