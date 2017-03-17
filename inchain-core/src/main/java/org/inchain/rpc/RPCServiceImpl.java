package org.inchain.rpc;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.account.AccountBody.ContentType;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.KeyValuePair;
import org.inchain.core.NotBroadcastBlockViolationEvidence;
import org.inchain.core.Peer;
import org.inchain.core.Product;
import org.inchain.core.Product.ProductType;
import org.inchain.core.Result;
import org.inchain.core.ViolationEvidence;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.AccountStore;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.BaseCommonlyTransaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.CreditTransaction;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.transaction.business.ViolationTransaction;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.utils.StringUtil;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RPCServiceImpl implements RPCService {
	
	private final static Logger log = LoggerFactory.getLogger(RPCServiceImpl.class);

	@Autowired
	private NetworkParams network;
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private AccountKit accountKit;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;

	/**
	 * 获取区块的数量
	 */
	@Override
	public long getBlockCount() {
		return network.getBestBlockHeight();
	}
	
	/**
	 * 获取最新区块的高度 
	 */
	@Override
	public long getBestBlockHeight() {
		return network.getBestBlockHeight();
	}
	
	/**
	 * 获取最新区块的hash
	 */
	@Override
	public String getBestBlockHash() {
		return network.getBestBlockHeader().getHash().toString();
	}
	
	/**
	 * 获取指定区块高度的hash
	 */
	@Override
	public String getBlockHashByHeight(long height) {
		BlockHeaderStore blockStore = blockStoreProvider.getHeaderByHeight(height);
		if(blockStore == null) {
			return "";
		}
		return blockStore.getBlockHeader().getHash().toString();
	}
	
	/**
	 * 通过区块的hash或者高度获取区块的头信息
	 * @throws JSONException 
	 */
	@Override
	public JSONObject getBlockHeader(String hashOrHeight) throws JSONException {
		BlockHeader blockHeader = null;
		try {
			Long height = Long.parseLong(hashOrHeight);
			BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeaderByHeight(height);
			if(blockHeaderStore != null) {
				blockHeader = blockHeaderStore.getBlockHeader();
			}
		} catch (Exception e) {
			BlockHeaderStore blockHeaderStore = blockStoreProvider.getHeader(Hex.decode(hashOrHeight));
			if(blockHeaderStore != null) {
				blockHeader = blockHeaderStore.getBlockHeader();
			}
		}
		
		JSONObject json = new JSONObject();
		if(blockHeader == null) {
			json.put("message", "not found");
			return json;
		}
		json.put("version", blockHeader.getVersion())
		.put("height", blockHeader.getHeight())
		.put("hash", blockHeader.getHash())
		.put("preHash", blockHeader.getPreHash())
		.put("merkleHash", blockHeader.getMerkleHash())
		.put("time", blockHeader.getTime())
		.put("periodStartTime", blockHeader.getPeriodStartTime())
		.put("timePeriod", blockHeader.getTimePeriod())
		.put("packAddress", blockHeader.getScriptSig().getAccountBase58(network))
		.put("scriptSig", blockHeader.getScriptSig())
		.put("txCount", blockHeader.getTxCount())
		.put("txs", blockHeader.getTxHashs());
		
		return json;
	}
	
	/**
	 * 通过区块的hash或者高度获取区块的完整信息
	 * @throws JSONException 
	 */
	@Override
	public JSONObject getBlock(String hashOrHeight) throws JSONException {
		Block block = null;
		try {
			Long height = Long.parseLong(hashOrHeight);
			BlockStore blockStore = blockStoreProvider.getBlockByHeight(height);
			if(blockStore != null) {
				block = blockStore.getBlock();
			}
		} catch (Exception e) {
			BlockStore blockStore = blockStoreProvider.getBlock(Hex.decode(hashOrHeight));
			if(blockStore != null) {
				block = blockStore.getBlock();
			}
		}
		
		JSONObject json = new JSONObject();
		if(block == null) {
			json.put("message", "not found");
			return json;
		}
		json.put("version", block.getVersion())
		.put("height", block.getHeight())
		.put("hash", block.getHash())
		.put("preHash", block.getPreHash())
		.put("merkleHash", block.getMerkleHash())
		.put("time", block.getTime())
		.put("periodStartTime", block.getPeriodStartTime())
		.put("timePeriod", block.getTimePeriod())
		.put("consensusAddress", block.getScriptSig().getAccountBase58(network))
		.put("scriptSig", block.getScriptSig())
		.put("txCount", block.getTxCount())
		.put("txs", block.getTxs());
		
		List<Transaction> txList = block.getTxs();
		
		JSONArray txs = new JSONArray();
		
		long bestHeight = network.getBestBlockHeight();
		List<Account> accountList = accountKit.getAccountList();
		
		for (Transaction transaction : txList) {
			txs.put(txConver(new TransactionStore(network, transaction, block.getHeight(), new byte[] {1}), bestHeight, accountList));
		}
		
		json.put("txs", txs);
		
		return json;
	}
	
	/**
	 * 获取内存里的count条交易
	 */
	@Override
	public String getmempoolinfo(String count) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 同时必需指定帐户管理密码和交易密码
	 */
	@Override
	public String newaccount(String mgpw, String trpw) {
		try {
			Account account = accountKit.createNewCertAccount(mgpw, trpw, AccountBody.empty());
			return account.getAddress().getBase58();
		} catch (Exception e) {
			e.printStackTrace();
			return  e.getMessage();
		}

	}
	
	/**
	 * 获取帐户的地址
	 */
	@Override
	public String getaccountaddress() {
//		return accountKit.getAccountList().;
		return null;
	}
	
	/**
	 * 获取帐户的公钥
	 */
	@Override
	public String getaccountpubkeys() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 备份私钥种子，同时显示帐户的hash160
	 */
	@Override
	public String dumpprivateseed() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 获取账户的余额
	 * @return Coin[]
	 */
	@Override
	public Coin[] getAccountBalance() {
		Coin[] balances = new Coin[2];
		balances[0] = accountKit.getCanUseBalance();
		balances[1] = accountKit.getCanNotUseBalance();
		return balances;
	}
	
	/**
	 * 获取账户的信用
	 * @return long
	 */
	@Override
	public long getAccountCredit() {
		return accountKit.getAccountInfo().getCert();
	}
	
	/**
	 * 获取账户的详细信息
	 * @return long
	 * @throws JSONException 
	 */
	public JSONObject getAccountInfo() throws JSONException {
		JSONObject json = new JSONObject();
		
		AccountStore info = accountKit.getAccountInfo();
		json.put("type", info.getType());
		json.put("adderss", new Address(network, info.getType(), info.getHash160()).getBase58());
		
		Coin[] blanaces = getAccountBalance();
		json.put("blanace", blanaces[0].add(blanaces[1]).value);
		json.put("canUseBlanace", blanaces[0].value);
		json.put("cannotUseBlanace", blanaces[1].value);
		
		json.put("cert", info.getCert());
		
		if(network.getSystemAccountVersion() == info.getType()) {
			json.put("pubkey", Hex.encode(info.getPubkeys()[0]));
		} else {
			json.put("infoTx", info.getInfoTxid());
			
			JSONArray mgarrays = new JSONArray();
			mgarrays.put(Hex.encode(info.getPubkeys()[0]));
			mgarrays.put(Hex.encode(info.getPubkeys()[1]));
			
			json.put("mgpubkey", mgarrays);
			
			JSONArray trarrays = new JSONArray();
			trarrays.put(Hex.encode(info.getPubkeys()[2]));
			trarrays.put(Hex.encode(info.getPubkeys()[3]));
			
			json.put("trpubkey", trarrays);
		}
		
		return json;
	}
	
	/**
	 * 加密钱包
	 * @param password 
	 * @return JSONObject
	 */
	public JSONObject encryptWallet(String password) throws JSONException {
		
		JSONObject json = new JSONObject();
		
		//判断账户是否已经加密
		if(accountKit.accountIsEncrypted()) {
			json.put("success", false);
			json.put("message", "钱包已经加密,如果需要修改密码,请使用password命令");
			return json;
		}
		
		//是否输入密码
		if(StringUtil.isEmpty(password)) {
			json.put("needInput", true);
			json.put("inputType", 2);	//设置密码
			json.put("inputTip", "输入钱包密码");
			return json;
		}
		//输入的密码是否合法
		if(!StringUtil.validPassword(password)) {
			json.put("success", false);
			json.put("message", "密码不合法,必须是6位以上,且包含数字和字母,请重新加密");
			return json;
		}
		
		Result result = accountKit.encryptWallet(password);
		json.put("success", result.isSuccess());
		json.put("message", result.getMessage());
		
		return json;
	}

	/**
	 * 修改密码
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject changePassword(String oldPassword, String newPassword) throws JSONException {
		JSONObject json = new JSONObject();
		
		//判断账户是否已经加密
		if(!accountKit.accountIsEncrypted()) {
			json.put("success", false);
			json.put("message", "钱包尚未加密，请使用encryptwallet命令对钱包加密");
			return json;
		}
		
		//是否输入旧密码
		if(StringUtil.isEmpty(oldPassword) || StringUtil.isEmpty(newPassword)) {
			json.put("needInput", true);
			json.put("inputType", 3);	//设置密码
			return json;
		}
		//输入的密码是否合法
		if(!StringUtil.validPassword(oldPassword)) {
			json.put("success", false);
			json.put("message", "钱包旧密码不正确");
			return json;
		}
		
		//输入的密码是否合法
		if(!StringUtil.validPassword(newPassword)) {
			json.put("success", false);
			json.put("message", "新密码不合法,必须是6位以上,且包含数字和字母,请重新加密");
			return json;
		}
		
		Result result = accountKit.changeWalletPassword(oldPassword, newPassword);
		
		json.put("success", result.isSuccess());
		json.put("message", result.getMessage());
		
		return json;
	}
	
	/**
	 * 获取帐户的交易记录
	 * @return JSONArray
	 * @throws JSONException 
	 */
	public JSONArray getTransaction() throws JSONException {
		List<TransactionStore> mineList = transactionStoreProvider.getMineTxList();
		
		JSONArray array = new JSONArray();
		
		long bestHeight = network.getBestBlockHeight();
		List<Account> accountList = accountKit.getAccountList();
		
		for (TransactionStore transactionStore : mineList) {
			array.put(txConver(transactionStore, bestHeight, accountList));
		}
		
		return array;
	}

	/**
	 * 通过交易hash获取条交易详情
	 * @param txid
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject getTx(String txid) throws JSONException {
		TransactionStore txs = blockStoreProvider.getTransaction(Hex.decode(txid));
		
		if(txs == null) {
			JSONObject json = new JSONObject();
			json.put("message", "not found");
			return json;
		}
		return txConver(txs);
	}
	
	/**
	 * 发送交易
	 * @param toAddress
	 * @param money
	 * @param fee
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject sendMoney(String toAddress, String money, String fee, String password) throws JSONException {
		JSONObject json = new JSONObject();
		
		if(StringUtil.isEmpty(toAddress) || StringUtil.isEmpty(money) ||
				StringUtil.isEmpty(fee)) {
			json.put("success", false);
			json.put("message", "params error");
			return json;
		}
		
		Coin moneyCoin = null;
		Coin feeCoin = null;
		try {
			moneyCoin = Coin.parseCoin(money);
			feeCoin = Coin.parseCoin(fee);
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", "金额不正确");
			return json;
		}
		
		//账户是否加密
		if(accountKit.accountIsEncrypted()) {
			if(StringUtil.isEmpty(password)) {
				json.put("needInput", true);
				json.put("inputType", 1);	//输入密码
				json.put("inputTip", "输入钱包密码进行转账");
				return json;
			} else {
				Result re = accountKit.decryptWallet(password);
				if(!re.isSuccess()) {
					json.put("success", false);
					json.put("message", re.getMessage());
					return json;
				}
			}
		}
		try {
			BroadcastResult br = accountKit.sendMoney(toAddress, moneyCoin, feeCoin);
			
			json.put("success", br.isSuccess());
			json.put("message", br.getMessage());
			
			return json;
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", e.getMessage());
			return json;
		} finally {
			accountKit.resetKeys();
		}
	}
	
	/**
	 * 获取共识节点列表
	 * @return JSONArray
	 * @throws JSONException 
	 */
	public JSONArray getConsensus() throws JSONException {
		List<AccountStore> accountList = accountKit.getConsensusAccounts();
		
		JSONArray array = new JSONArray();
		
		for (AccountStore account : accountList) {
			
			JSONObject json = new JSONObject();
			
			json.put("type", account.getType());
			json.put("address", new Address(network, account.getType(), account.getHash160()).getBase58());
			json.put("cert", account.getCert());
			
			array.put(json);
		}
		
		return array;
	}

	/**
	 * 注册共识
	 * @param password
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject regConsensus(String password) throws JSONException {
		
		JSONObject json = new JSONObject();
		
		//判断信用是否足够
		long cert = getAccountCredit();
		if(cert < Configure.CONSENSUS_CREDIT) {
			json.put("success", false);
			json.put("message", "信用值不够,不能参加共识,当前"+cert+",共识所需"+Configure.CONSENSUS_CREDIT+",还差"+(Configure.CONSENSUS_CREDIT - cert));
			return json;
		}
		
		//当前是否已经在共识了
		if(accountKit.checkConsensusing()) {
			json.put("success", false);
			json.put("message", "当前已经在共识状态中了");
			return json;
		}
		
		//判断账户是否加密
		if(accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR) && password == null) {
			json.put("needInput", true);
			json.put("inputType", 1);	//输入密码
			json.put("inputTip", "输入钱包密码参与共识");
			return json;
		}
		
		//解密钱包
		if(accountKit.accountIsEncrypted()) {
			Result result = accountKit.decryptWallet(password, Definition.TX_VERIFY_TR);
			if(!result.isSuccess()) {
				json.put("success", false);
				json.put("message", result.getMessage());
				return json;
			}
		}
		try {
			Result result = accountKit.registerConsensus();
			if(!result.isSuccess()) {
				json.put("success", false);
				json.put("message", result.getMessage());
				return json;
			}
		} finally {
			new Thread() {
				public void run() {
					//延迟重置账户
					ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
					consensusMeeting.waitMining();
					accountKit.resetKeys();
				};
			}.start();
		}

		json.put("success", true);
		json.put("message", "已成功申请");
		
		return json;
	}

	/**
	 * 退出共识
	 * @param password
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject remConsensus(String password) throws JSONException {
		
		JSONObject json = new JSONObject();
		
		//判断信用是否足够
		
		//当前是否已经在共识了
		if(!accountKit.checkConsensusing()) {
			json.put("success", false);
			json.put("message", "当前没有在共识中");
			return json;
		}
		
		//判断账户是否加密
		if(accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR) && password == null) {
			json.put("needInput", true);
			json.put("inputType", 1);	//输入密码
			json.put("inputTip", "输入钱包密码退出共识");
			return json;
		}
		
		//解密钱包
		if(accountKit.accountIsEncrypted()) {
			Result result = accountKit.decryptWallet(password, Definition.TX_VERIFY_TR);
			if(!result.isSuccess()) {
				json.put("success", false);
				json.put("message", result.getMessage());
				return json;
			}
		}
		try {
			Result result = accountKit.quitConsensus();
			if(!result.isSuccess()) {
				json.put("success", false);
				json.put("message", result.getMessage());
				return json;
			}
		} finally {
			accountKit.resetKeys();
		}
		
		json.put("success", true);
		json.put("message", "已成功申请退出共识");
		
		return json;
	}
	
	/**
	 * 获取连接节点信息
	 * @return JSONObject
	 */
	@Override
	public JSONObject getPeers() throws JSONException {
		List<Peer> peerList = peerKit.findAvailablePeers();
		JSONArray array = new JSONArray();
		
		for (Peer peer : peerList) {
			JSONObject peerJson = new JSONObject();
			
			peerJson.put("host", peer.getAddress().getAddr().getHostAddress());
			peerJson.put("port", peer.getAddress().getPort());
			peerJson.put("version", peer.getPeerVersionMessage().getSubVer());
			peerJson.put("bestBlockHeight", peer.getBestBlockHeight());
			peerJson.put("time", DateUtil.convertDate(new Date(peer.getSendVersionMessageTime())));
			peerJson.put("timeOffset", peer.getTimeOffset());
			peerJson.put("timeLength", new Date().getTime() - peer.getSendVersionMessageTime());
			
			array.put(peerJson);
		}
		
		JSONObject json = new JSONObject();
		
		json.put("count", array.length());
		json.put("peers", array);
		
		return json;
	}
	
	/*
	 * 转换tx为json
	 */
	private JSONObject txConver(TransactionStore txs) throws JSONException {
		return txConver(txs, getBestBlockHeight(), accountKit.getAccountList());
	}

	/*
	 * 转换tx为json
	 */
	private JSONObject txConver(TransactionStore txs, long bestHeight, List<Account> accountList) throws JSONException {
		JSONObject json = new JSONObject();
		
		Transaction tx = txs.getTransaction();
		
		json.put("version", tx.getVersion());
		json.put("hash", tx.getHash());
		json.put("type", tx.getType());
		json.put("time", tx.getTime());
		json.put("locakTime", tx.getLockTime());
		
		json.put("confirmation", bestHeight - txs.getHeight());
		
		if(tx instanceof BaseCommonlyTransaction) {
			BaseCommonlyTransaction bctx = (BaseCommonlyTransaction) tx;
			try {
				json.put("address", bctx.getScriptSig().getAccountBase58(network));
			} catch (Exception e) {
				log.error("出错 {}", bctx, e);
			}
			json.put("scriptSig", bctx.getScriptSig());
		}
		
		if(tx.getType() == Definition.TYPE_COINBASE || 
				tx.getType() == Definition.TYPE_PAY) {
			
			//是否是转出
			boolean isSendout = false;
			
			JSONArray inputArray = new JSONArray();
			JSONArray outputArray = new JSONArray();
			
			Coin inputFee = Coin.ZERO;
			Coin outputFee = Coin.ZERO;
			
			
			List<Input> inputs = tx.getInputs();
			if(tx.getType() != Definition.TYPE_COINBASE && inputs != null && inputs.size() > 0) {
				for (Input input : inputs) {
					
					JSONObject inputJson = new JSONObject();
					
					inputJson.put("fromTx", input.getFrom().getParent().getHash());
					inputJson.put("fromIndex", input.getFrom().getIndex());
					
					TransactionOutput from = input.getFrom();
					TransactionStore fromTx = blockStoreProvider.getTransaction(from.getParent().getHash().getBytes());
					
					Transaction ftx = null;
					if(fromTx == null) {
						//交易不存在区块里，那么应该在内存里面
						ftx = MempoolContainer.getInstace().get(from.getParent().getHash());
					} else {
						ftx = fromTx.getTransaction();
					}
					if(ftx == null) {
						continue;
					}
					Output fromOutput = ftx.getOutput(from.getIndex());
					
					Script script = fromOutput.getScript();
					for (Account account : accountList) {
						if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, account.getAddress().getHash160())) {
							isSendout = true;
							break;
						}
					}
					
					if(script.isSentToAddress()) {
						inputJson.put("address", new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58());
					}
					
					Coin value = Coin.valueOf(fromOutput.getValue());
					inputJson.put("value", value.value);
					
					inputFee = inputFee.add(value);
					
					inputArray.put(inputJson);
				}
			}
			
			List<Output> outputs = tx.getOutputs();
			
			for (Output output : outputs) {
				Script script = output.getScript();
				
				Coin value = Coin.valueOf(output.getValue());
				outputFee = outputFee.add(value);
				
				JSONObject outputJson = new JSONObject();
				outputJson.put("value", value.value);
				outputJson.put("lockTime", tx.getLockTime());
				
				if(script.isSentToAddress()) {
					outputJson.put("address", new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58());
				}

				outputJson.put("scriptSig", script);
				
				outputArray.put(outputJson);
			}

			if(tx.getType() != Definition.TYPE_COINBASE) {
				json.put("fee", inputFee.subtract(outputFee).value);
			} else {
				json.put("fee", Coin.ZERO.value);
			}
			if(isSendout) {
				json.put("txType", "send");
			} else {
				json.put("txType", "receive");
			}
			
			json.put("inputs", inputArray);
			json.put("outputs", outputArray);
			
		} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER || 
				tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
			//认证账户注册
			CertAccountRegisterTransaction crt = (CertAccountRegisterTransaction) tx;
			
			JSONArray infos = new JSONArray();
			
			List<KeyValuePair> bodyContents = crt.getBody().getContents();
			for (KeyValuePair keyValuePair : bodyContents) {
				if(ContentType.from(keyValuePair.getKey()) == ContentType.LOGO) {
					//图标
					infos.put(new JSONObject().put(keyValuePair.getKeyName(), Base64.getEncoder().encodeToString(keyValuePair.getValue())));
				} else {
					infos.put(new JSONObject().put(keyValuePair.getKeyName(), keyValuePair.getValueToString()));
				}
			}
			json.put("infos", infos);
			
		} else if(tx.getType() == Definition.TYPE_CREATE_PRODUCT) {
			
			ProductTransaction ptx = (ProductTransaction) tx;
			
			List<KeyValuePair> bodyContents = ptx.getProduct().getContents();
			
			JSONArray product = new JSONArray();
			for (KeyValuePair keyValuePair : bodyContents) {
				if(ProductType.from(keyValuePair.getKey()) == ProductType.CREATE_TIME) {
					//时间
					product.put(new JSONObject().put(keyValuePair.getKeyName(), DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)))));
				} else {
					product.put(new JSONObject().put(keyValuePair.getKeyName(), keyValuePair.getValueToString()));
				}
			}
			json.put("product", product);
			
		} else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {

			GeneralAntifakeTransaction gtx = (GeneralAntifakeTransaction) tx;
			
			if(gtx.getProduct() != null) {
				JSONArray product = new JSONArray();
				for (KeyValuePair keyValuePair : gtx.getProduct().getContents()) {
					if(ProductType.from(keyValuePair.getKey()) == ProductType.CREATE_TIME) {
						//时间
						product.put(new JSONObject().put(keyValuePair.getKeyName(), DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)))));
					} else {
						product.put(new JSONObject().put(keyValuePair.getKeyName(), keyValuePair.getValueToString()));
					}
				}
				json.put("product", product);
			}
			if(gtx.getProductTx() != null) {
				TransactionStore ptx = accountKit.getTransaction(gtx.getProductTx());
				//必要的NPT验证
				if(ptx == null) {
					return json;
				}
				json.put("productTx", ((ProductTransaction)ptx.getTransaction()).getHash());
			}
			
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {

			AntifakeCodeVerifyTransaction atx = (AntifakeCodeVerifyTransaction) tx;
			
			byte[] makeCodeTxBytes = accountKit.getChainstate(atx.getAntifakeCode().getBytes());
			//必要的NPT验证
			if(makeCodeTxBytes == null) {
				return json;
			}
			TransactionStore makeCodeTxStore = accountKit.getTransaction(Sha256Hash.wrap(makeCodeTxBytes));
			if(makeCodeTxStore == null) {
				return json;
			}
			AntifakeCodeMakeTransaction makeCodeTx = (AntifakeCodeMakeTransaction)makeCodeTxStore.getTransaction();
			
			TransactionStore productTxStore = accountKit.getTransaction(makeCodeTx.getProductTx());
			if(productTxStore == null) {
				return json;
			}
			json.put("antifakeCodeTx", atx.getAntifakeCode());
			json.put("productTx", productTxStore.getTransaction().getHash());
			
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE) {
			AntifakeCodeMakeTransaction atx = (AntifakeCodeMakeTransaction) tx;
			
			TransactionStore ptx = blockStoreProvider.getTransaction(atx.getProductTx().getBytes());
			//必要的NPT验证
			if(ptx != null) {
				Product product = ((ProductTransaction)ptx.getTransaction()).getProduct();
				json.put("productName", product.getName());
				json.put("productTx", ptx.getTransaction().getHash());
				json.put("rewardCoin", atx.getRewardCoin().toText());
			}
			
		} else if(tx.getType() == Definition.TYPE_CREDIT) {
			CreditTransaction ctx = (CreditTransaction) tx;
			
			String reason = "初始化";
			if(ctx.getReasonType() == Definition.CREDIT_TYPE_PAY) {
				reason = String.format("%s小时内第一笔转账", Configure.CERT_CHANGE_PAY_INTERVAL/3600000l);
			}
			reason = "信用 +" + ctx.getCredit() + " 原因：" + reason;
			
			json.put("credit", ctx.getCredit());
			json.put("reason", reason);
			
		} else if(tx.getType() == Definition.TYPE_VIOLATION) {
			ViolationTransaction vtx = (ViolationTransaction) tx;
			
			ViolationEvidence evidence = vtx.getViolationEvidence();
			int violationType = evidence.getViolationType();
			String reason = "";
			long credit = 0;
			if(violationType == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
				NotBroadcastBlockViolationEvidence nbve = (NotBroadcastBlockViolationEvidence) evidence;
				reason = String.format("共识过程中，开始时间为%s的轮次及后面一轮次超时未出块", DateUtil.convertDate(new Date(nbve.getCurrentPeriodStartTime() * 1000)));
				credit = Configure.CERT_CHANGE_TIME_OUT;
			}
			reason = "信用 " + credit + " 原因：" + reason;
			
			json.put("credit", credit);
			json.put("reason", reason);
		}
		
		return json;
	}

}
