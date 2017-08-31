package org.inchain.rpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.account.AccountBody;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.consensus.MiningInfos;
import org.inchain.core.*;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.AccountStore;
import org.inchain.store.BlockForkStore;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.*;
import org.inchain.utils.Base58;
import org.inchain.utils.ConsensusCalculationUtil;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.utils.StringUtil;
import org.inchain.utils.Utils;
import org.inchain.validator.TransactionValidator;
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
	private ConsensusPool consensusPool;
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private TransactionStoreProvider transactionStoreProvider;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	@Autowired
	private TransactionValidator transactionValidator;

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
			json.put("success", "false");
			json.put("message", "not found");
			return json;
		}

        JSONObject blockJson = new JSONObject();
        blockJson.put("version", blockHeader.getVersion())
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

        json.put("success", true);
        json.put("blockheader", blockJson);
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
			json.put("success", false);
			json.put("message", "not found");
			return json;
		}

		JSONObject blockJson = new JSONObject();

		blockJson.put("version", block.getVersion())
			.put("height", block.getHeight())
			.put("hash", block.getHash())
			.put("preHash", block.getPreHash())
			.put("merkleHash", block.getMerkleHash())
			.put("time", block.getTime())
			.put("periodStartTime", block.getPeriodStartTime())
			.put("timePeriod", block.getTimePeriod())
			.put("consensusAddress", block.getScriptSig().getAccountBase58(network))
			.put("scriptSig", block.getScriptSig())
			.put("txCount", block.getTxCount());
			//.put("txs", block.getTxs());

		List<Transaction> txList = block.getTxs();

		JSONArray txs = new JSONArray();

		long bestHeight = network.getBestBlockHeight();
		List<Account> accountList = accountKit.getAccountList();

		for (Transaction transaction : txList) {
			txs.put(txConver(new TransactionStore(network, transaction, block.getHeight(), new byte[] {1}), bestHeight, accountList));
		}

		blockJson.put("txs", txs);
        json.put("success", true);
        json.put("block", blockJson);
		return json;
	}

	/**
	 * 通过hash获取一个分叉块
	 * @throws JSONException
	 */
	@Override
	public JSONObject getForkBlock(String hash) throws JSONException {

		JSONObject json = new JSONObject();
		byte[] content = chainstateStoreProvider.getBytes(Sha256Hash.wrap(hash).getBytes());
		if(content == null) {
			json.put("message", "not found");
			return json;
		}
		BlockForkStore blockForkStore = new BlockForkStore(network, content);

		Block block = blockForkStore.getBlock();

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
	 * 创建一个普通账户
	 * @return JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	@Override
	public JSONObject newAccount() throws IOException, JSONException {
		Address address = accountKit.createNewAccount();

		JSONObject result = new JSONObject();

		if(address == null) {
			result.put("success", false);
			result.put("message", "创建失败");
		} else {
			result.put("success", true);
			result.put("message", "成功");
			result.put("address", address.getBase58());
		}
		return result;
	}

	/**
	 * 创建count普通账户
	 * @return JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	@Override
	public JSONObject newAccount(int count) throws IOException, JSONException {
		JSONObject addresses = accountKit.createNewAccount(count);

		JSONObject result = new JSONObject();

		if(addresses == null) {
			result.put("success", false);
			result.put("message", "创建失败");
		} else {
			result.put("success", true);
			result.put("message", "成功");
			result.put("addresses", addresses.get("addresses"));
		}
		return result;
	}

	/**
	 * 创建一个认证账户
	 * @param mgpw
	 * @param trpw
	 * @param body
	 * @param certpw
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject newCertAccount(String mgpw, String trpw, AccountBody body,String certpw ,String managerAddress) throws JSONException {
		JSONObject result = new JSONObject();
		try {
			Account account = accountKit.createNewCertAccount(mgpw, trpw, body,certpw,managerAddress);
			if(account == null) {
				result.put("success", false);
				result.put("message", "创建失败");
			} else {
				result.put("success", true);
				result.put("message", "创建成功");
				result.put("address", account.getAddress().getBase58());
				result.put("prikey", Hex.encode(account.getPriSeed()));

				result.put("mgPubkeys", new JSONArray().put(Hex.encode(account.getMgPubkeys()[0])).put(Hex.encode(account.getMgPubkeys()[1])));
				result.put("trPubkeys", new JSONArray().put(Hex.encode(account.getTrPubkeys()[0])));

				result.put("txhash", account.getAccountTransaction().getHash());
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 修改认证账户信息
	 * @param body
	 * @param mgpw
	 * @param address
	 * @return JSONObject
	 */
	@Override
	public JSONObject updateCertAccount(AccountBody body, String mgpw, String address) throws JSONException {
		JSONObject result = new JSONObject();
		try {
			BroadcastResult res = accountKit.updateCertAccountInfo(mgpw, address, body);
			if(!res.isSuccess()) {
				result.put("success", false);
				result.put("message", res.getMessage());
			} else {
				result.put("success", true);
				result.put("txhash", res.getHash());
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 吊销认证账户信息
	 * @param revokeAddress
	 * @param mgpw
	 * @param address
	 * @return JSONObject
	 */
	@Override
	public JSONObject revokeCertAccount(String revokeAddress, String mgpw, String address) throws JSONException {
		JSONObject result = new JSONObject();
		try {
			BroadcastResult res = accountKit.revokeCertAccount(revokeAddress, mgpw, address);
			if(!res.isSuccess()) {
				result.put("success", false);
				result.put("message", res.getMessage());
			} else {
				result.put("success", true);
				result.put("txhash", res.getHash());
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 认证账户修改密码
	 * @param oldMgpw
	 * @param newMgpw
	 * @param newTrpw
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject certAccountEditPassword(String oldMgpw, String newMgpw, String newTrpw, String address) throws JSONException {
		JSONObject result = new JSONObject();
		try {
			BroadcastResult res = accountKit.certAccountEditPassword(oldMgpw, newMgpw, newTrpw, address);
			if(!res.isSuccess()) {
				result.put("success", false);
				result.put("message", res.getMessage());
			} else {
				result.put("success", true);
				result.put("txhash", res.getHash());
				Account account = null;
				if(address !=null) {
					account = accountKit.getAccount(address);
				}else {
					account = accountKit.getDefaultAccount();
				}
				result.put("mgPubkeys", new JSONArray().put(Hex.encode(account.getMgPubkeys()[0])).put(Hex.encode(account.getMgPubkeys()[1])));
				result.put("trPubkeys", new JSONArray().put(Hex.encode(account.getTrPubkeys()[0])));
			}
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 认证账户创建商品
	 * @param product
	 * @param certpw
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject createProduct(Product product, String certpw, String address) throws JSONException {
		JSONObject result = new JSONObject();
		try {
			Account account = null;

			if(address == null) {
				account = accountKit.getDefaultAccount();
			} else {
				account = accountKit.getAccount(address);
			}
			if(account == null || !account.isCertAccount()) {
				result.put("success", false);
				result.put("message", "认证账户不存在");
				return result;
			}

			if(account.isEncryptedOfTr()) {
				ECKey[] eckey = account.decryptionTr(certpw);
				if(eckey == null) {
					result.put("success", false);
					result.put("message", "密码不正确");
					return result;
				}
			}
			BroadcastResult res = accountKit.createProduct(product, account);

			result.put("success", res.isSuccess());
			result.put("message", res.getMessage());
			result.put("txhash", res.getHash());
		} catch (Exception e) {
			log.error("创建商品出错：", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 认证账户创建防伪码
	 * @param productTx 商品id
	 * @param count 数量
	 * @param sources 来源
	 * @param reward 奖励
	 * @param trpw 账户交易密码
	 * @param address	账户地址
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject createAntifake(String productTx, int count, JSONArray sources, Coin reward, String trpw, String address) throws JSONException {
		JSONObject result = new JSONObject();
		Account account = null;
		try {
			if(address == null) {
				account = accountKit.getDefaultAccount();
			} else {
				account = accountKit.getAccount(address);
			}
			if(account == null || !account.isCertAccount()) {
				result.put("success", false);
				result.put("message", "认证账户不存在");
				return result;
			}

			if(reward == null || reward.isLessThan(Coin.ZERO)) {
				reward = Coin.ZERO;
			}
			//如果有奖励，验证余额是否充足
			if(reward.isGreaterThan(Coin.ZERO)) {
				Coin balance = accountKit.getBalance(account);
				if(reward.multiply(count).isGreaterThan(balance)) {
					result.put("success", false);
					result.put("message", "余额不足，无法奖励 ");
					return result;
				}
			}

			if(Configure.MAX_ANTICODE_COUNT<count){
				result.put("success", false);
				result.put("message", "每次最多生成防伪码："+Configure.MAX_ANTICODE_COUNT);
				return result;
			}

			JSONArray antifakeList = new JSONArray();
			JSONArray errormgs = new JSONArray();

			if(count > 0) {
				for (int i = 0; i < count; i++) {
					//来源
					List<String> sourcesList = null;
					if(sources != null) {
						sourcesList = new ArrayList<String>();
						JSONArray sourcesArray = sources.getJSONArray(i);

						for (int j = 0; j < sourcesArray.length(); j++) {
							sourcesList.add(sourcesArray.getString(j));
						}
					}
					BroadcastMakeAntifakeCodeResult broadcastResult = accountKit.makeAntifakeCode(productTx, reward, sourcesList, account, trpw);

					if(broadcastResult.isSuccess()) {
						JSONObject antifakeJson = new JSONObject();
						antifakeJson.put("antifakeCode", Base58.encode(broadcastResult.getAntifakeCode().getAntifakeCode()));
						antifakeJson.put("verifyCode", broadcastResult.getAntifakeCode().getVerifyCode());
						antifakeJson.put("antifakeContent", broadcastResult.getAntifakeCode().base58Encode());
						antifakeJson.put("txHash", broadcastResult.getHash());

						antifakeList.put(antifakeJson);
					} else {
						errormgs.put(broadcastResult.getMessage());
					}
				}
			}
			result.put("success", true);
			result.put("message", "成功生成" + antifakeList.length() + "个防伪码");
			result.put("antifakeList", antifakeList);
			result.put("errormgs", errormgs);

		} catch (Exception e) {
			log.error("创建防伪码出错：", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		} finally {
			if(account != null) {
				account.resetKey();
			}
		}
		return result;
	}

	@Override
	public JSONObject bindAntifake(String antiCode,String productTx,String trpw,String address) throws JSONException{
		JSONObject result = new JSONObject();
		Account account = null;
		try {
			if(address == null) {
				account = accountKit.getDefaultAccount();
			} else {
				account = accountKit.getAccount(address);
			}
			if(account == null || !account.isCertAccount()) {
				result.put("success", false);
				result.put("message", "认证账户不存在");
				return result;
			}


			BroadcastResult rs = accountKit.bindAnticode(antiCode, productTx, trpw, account);

			if(!rs.isSuccess()) {
				result.put("success", rs.isSuccess());
				result.put("message", rs.getMessage());
			}else{
				result.put("success", true);
				result.put("txhash",rs.getMessage());
			}

		}catch (Exception e){
			log.error("绑定防伪码出错："+e.getMessage());
			result.put("success", false);
			result.put("message", e.getMessage());
		}finally {
			if(account != null) {
				account.resetKey();
			}
		}
		return result;
	}

	@Override
	public JSONObject regAssets(String name, String description, String code, String logo, String remark, String address, String password) throws JSONException {
		JSONObject result = new JSONObject();
		Account account = null;
		try {
			if(address == null) {
				account = accountKit.getDefaultAccount();
			} else {
				account = accountKit.getAccount(address);
			}
			if(account == null) {
				throw new VerificationException("账户不存在");
			}
			if(account.isEncrypted()) {
				if(StringUtil.isEmpty(password)) {
					throw new VerificationException("账户已加密，请解密或者传入密码");
				}

				//解密钱包
				Result pwdResult = accountKit.decryptAccount(password ,address,Definition.TX_VERIFY_TR);
				if(!pwdResult.isSuccess()) {
					throw new VerificationException("密码错误");
				}

			}

			//创建交易
			AssetsRegisterTransaction assetsRegisterTx = new AssetsRegisterTransaction(network,
					name.getBytes(Utils.UTF_8),
					description.getBytes(Utils.UTF_8),
					code.getBytes(Utils.UTF_8),
					logo.getBytes(Utils.UTF_8),
					remark.getBytes(Utils.UTF_8));


			//资产注册认证需要10000个INS手续费，这里验证余额是否充足
			Coin balance = accountKit.getBalance(account);
			if(Configure.ASSETS_REG_FEE.isGreaterThan(balance)) {
				result.put("success", false);
				result.put("message", "余额不足10000INS，无法做资产注册");
				return result;
			}

			BroadcastResult br = accountKit.regAssets(account, assetsRegisterTx);
			result.put("success",  br.isSuccess());
			result.put("message", br.getMessage());
		}catch (VerificationException ve) {
			log.error("资产注册出错：", ve);
			result.put("success", false);
			result.put("message", ve.getMessage());
		}
		catch (Exception e) {
			log.error("资产注册出错：", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}finally {
			if(account != null) {
				account.resetKey();
			}
		}
		return result;
	}

	/**
	 * 获取注册资产列表
	 * @return
	 */
	public JSONObject getAssetsRegList() throws JSONException{
		List<TransactionStore> list = chainstateStoreProvider.getAssetRegList();
		List<JSONObject> jsonList = new ArrayList<>();

		for(TransactionStore regTx : list) {
			JSONObject json = txConver(regTx);
			jsonList.add(json);
		}
		return new JSONObject().put("list",jsonList);
	}

	/**
	 *  资产发行
	 *  必须是先注册资产才能做资产发行
	 * @param code
	 * @param receiver
	 * @param amount
	 * @param address
	 * @param password
	 * @return
	 * @throws JSONException
	 */
	public JSONObject assetsIssue(String code, String receiver, Long amount,String remark, String address, String password) throws JSONException {
		JSONObject result = new JSONObject();
		Account account = null;

		try {
			//1、首先判断账户是否存在，是否加密
			account = checkAndGetAccount(address, password, Definition.TX_VERIFY_TR);

			//2、判断接收人地址是否合法   PS：通常底层存储都是address的hash160，长度为20
			byte[] hashReceiver = null;
			try {
				Address receiverAddr = Address.fromBase58(network, receiver);
				//如果是认证账户需要判断认证账户地址是否存在
				if(receiverAddr.getVersion() == network.getCertAccountVersion()) {
					AccountStore certAccount = accountKit.getAccountInfo(receiver);
					if(certAccount == null) {
						throw new VerificationException("接收人地址错误");
					}
				}
				hashReceiver = receiverAddr.getHash160();
			}catch (Exception e) {
				throw new VerificationException("接收人地址错误");
			}


			//3、根据code获取资产注册交易是否存在
			AssetsRegisterTransaction assetsRegisterTx = chainstateStoreProvider.getAssetsRegisterTxByCode(code.getBytes(Utils.UTF_8));
			if(assetsRegisterTx == null) {
				throw new VerificationException("注册资产不存在");
			}


			BroadcastResult br = accountKit.assetsIssue(account, assetsRegisterTx, hashReceiver, amount, remark);
			result.put("success",  br.isSuccess());
			result.put("message", br.getMessage());

		}catch (VerificationException ve) {
			log.error("资产发行出错：", ve);
			result.put("success", false);
			result.put("message", ve.getMessage());
		}
		catch (Exception e) {
			log.error("资产发行出错：", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}finally {
			if(account != null) {
				account.resetKey();
			}
		}
		return result;
	}

	/**
	 *  获取资产发行列表
	 * @param code
	 * @return
	 * @throws JSONException
	 */
	public JSONObject getAssetsIssueList(String code) throws JSONException {
		JSONObject result = new JSONObject();

		//根据code获取资产注册交易是否存在
		AssetsRegisterTransaction assetsRegisterTx = chainstateStoreProvider.getAssetsRegisterTxByCode(code.getBytes(Utils.UTF_8));
		if(assetsRegisterTx == null) {
			result.put("success", false);
			result.put("message", "注册资产不存在");
			return result;
		}

		List<TransactionStore> list = chainstateStoreProvider.getAssetsIssueList(assetsRegisterTx.getCode());
		List<JSONObject> jsonList = new ArrayList<>();
		//组装资产注册json格式
		JSONObject regJson = new JSONObject();
		regJson.put("name", new String(assetsRegisterTx.getName(), Utils.UTF_8));
		regJson.put("description", new String(assetsRegisterTx.getDescription(), Utils.UTF_8));
		regJson.put("code", new String(assetsRegisterTx.getCode(), Utils.UTF_8));
		regJson.put("logo", new String(assetsRegisterTx.getLogo(), Utils.UTF_8));
		regJson.put("remark", new String(assetsRegisterTx.getRemark(), Utils.UTF_8));

		Long amount = 0L;   //资产发行总量

		for(TransactionStore issueTx : list) {
			AssetsIssuedTransaction aitx = (AssetsIssuedTransaction) issueTx.getTransaction();
			JSONObject json = txConver(issueTx);
			amount += aitx.getAmount();
			jsonList.add(json);
		}
		regJson.put("amount", amount);

		return new JSONObject().put("regTx",regJson).put("list",jsonList);
	}

	/**
	 *  获取我的资产账户列表
	 * @return
	 * @throws JSONException
	 */
	public JSONObject getMineAssets(String address, String password) throws JSONException {
		JSONObject result = new JSONObject();
		Account account = null;
		try {
			//1、首先判断账户是否存在，是否加密
			if(address == null) {
				account = accountKit.getDefaultAccount();
			} else {
				account = accountKit.getAccount(address);
			}

			List<JSONObject> jsonList = new ArrayList<>();
			List<Assets> list = chainstateStoreProvider.getMyAssetsAccount(account.getAddress().getHash160());
			for(Assets assets : list) {
				AssetsRegisterTransaction registerTx = chainstateStoreProvider.getAssetsRegisterTxByCodeHash256(assets.getCode());
				if(registerTx != null) {
					JSONObject object = new JSONObject();
					object.put("name", new String(registerTx.getName(), Utils.UTF_8));
					object.put("code", new String(registerTx.getCode(), Utils.UTF_8));
					object.put("banlance", assets.getBalance());
					jsonList.add(object);
				}
			}
			result.put("list", jsonList);

		}catch (VerificationException ve) {
			log.error("资产发行出错：", ve);
			result.put("success", false);
			result.put("message", ve.getMessage());
		}
		catch (Exception e) {
			log.error("资产发行出错：", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}finally {
			if(account != null) {
				account.resetKey();
			}
		}
		return result;
	}
	/**
	 * 资产转让
	 * @param code
	 * @param receiver
	 * @param amount
	 * @param remark
	 * @param address
	 * @param password
	 * @return
	 * @throws JSONException
	 */
	public JSONObject assetsTransfer(String code, String receiver, Long amount,String remark, String address, String password) throws JSONException {
		JSONObject result = new JSONObject();
		Account account = null;

		try {
			//1、首先判断账户是否存在，是否加密
			account = checkAndGetAccount(address, password, Definition.TX_VERIFY_TR);
			//2、判断接收人地址是否合法
			byte[] hashReceiver;
			try {
				hashReceiver = new Address(network, receiver).getHash160();
			} catch (Exception e) {
				throw new VerificationException("接收人地址错误");
			}
			//判断转账人和接收人是不是同一个人
			if(Arrays.equals(account.getAddress().getHash160(), hashReceiver)) {
				throw new VerificationException("接收人和转账人不能是同一人");
			}

			//3、根据code获取资产注册交易是否存在
			AssetsRegisterTransaction assetsRegisterTx = chainstateStoreProvider.getAssetsRegisterTxByCode(code.getBytes(Utils.UTF_8));
			if(assetsRegisterTx == null) {
				throw new VerificationException("注册资产不存在");
			}

			BroadcastResult br = accountKit.assetsTransfer(account, assetsRegisterTx, hashReceiver, amount, remark);
			result.put("success",  br.isSuccess());
			result.put("message", br.getMessage());

		}catch (VerificationException ve) {
			log.error("资产发行出错：", ve);
			result.put("success", false);
			result.put("message", ve.getMessage());
		}
		catch (Exception e) {
			log.error("资产发行出错：", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}finally {
			if(account != null) {
				account.resetKey();
			}
		}
		return result;
	}


	/**
	 * 通过防伪码查询防伪码相关的所有信息
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryAntifake(String antifakeCode) throws JSONException {
		JSONObject result = new JSONObject();

		try {
			if(StringUtil.isEmpty(antifakeCode)) {
				result.put("success", false);
				result.put("message", "防伪码为空");
				return result;
			}

			//解析防伪码字符串
			byte[] antifakeCodeBytes = Base58.decode(antifakeCode.trim());

			AntifakeInfosResult res = accountKit.getAntifakeInfos(antifakeCodeBytes);

			if(!res.isSuccess()) {
				result.put("success", false);
				result.put("message", res.getMessage());
				return result;
			}

			setResult(result, res);

			result.put("success", true);

		} catch (Exception e) {
			log.error("查询防伪码出错", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/*
	 * 设置防伪码结果
	 */
	private void setResult(JSONObject result, AntifakeInfosResult res) throws JSONException, IOException {
		ProductTransaction ptx = res.getProductTx();
		if(ptx!=null) {
			List<ProductKeyValue> productBodyContents = ptx.getProduct().getContents();

			JSONArray product = new JSONArray();
			String productName = null;
			for (ProductKeyValue keyValuePair : productBodyContents) {
				if (ProductKeyValue.CREATE_TIME.getCode().equals(keyValuePair.getCode())) {
					//时间
					product.put(new JSONObject().put("code", keyValuePair.getCode()).put("name", keyValuePair.getName()).put("value", DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)))));
				} else if (!ProductKeyValue.IMG.getCode().equals(keyValuePair.getCode()) && !ProductKeyValue.LOGO.getCode().equals(keyValuePair.getCode())) {
					if (ProductKeyValue.NAME.getCode().equals(keyValuePair.getCode())) {
						productName = keyValuePair.getValueToString();
					}
					product.put(new JSONObject().put("code", keyValuePair.getCode()).put("name", keyValuePair.getName()).put("value", keyValuePair.getValueToString()));
				}
			}
			result.put("product", new JSONObject().put("name", productName).put("values", product));
		}else {
			result.put("product", new JSONObject().put("message","防伪码尚未关联产品" ));
		}
		//商家信息
		AccountStore certAccountInfo = res.getBusiness();

		JSONArray infos = new JSONArray();

		List<AccountKeyValue> businessBodyContents = certAccountInfo.getAccountBody().getContents();

		String businessName = null;
		for (AccountKeyValue keyValuePair : businessBodyContents) {
			if(AccountKeyValue.LOGO.getCode().equals(keyValuePair.getCode())) {
				//图标
				infos.put(new JSONObject().put("code", keyValuePair.getCode()).put("name", keyValuePair.getName()).put("value", Base64.getEncoder().encodeToString(keyValuePair.getValue())));
			} else {
				if(AccountKeyValue.NAME.getCode().equals(keyValuePair.getCode())) {
					businessName = keyValuePair.getValueToString();
				}
				infos.put(new JSONObject().put("code", keyValuePair.getCode()).put("name", keyValuePair.getName()).put("value", keyValuePair.getValueToString()));
			}
		}
		result.put("business", new JSONObject().put("name", businessName).put("values", infos));

		//防伪码生成交易id

		result.put("hash", res.getMakeTx().getHash());
		result.put("antifakeCode", Base58.encode(res.getMakeTx().getAntifakeCode()));
		result.put("time", res.getMakeTx().getTime());
		//防伪码验证状态
		result.put("hasVerify", res.isHasVerify());
		//验证信息
		if(res.isHasVerify() && res.getVerifyTx() != null) {
			BaseCommonlyTransaction verifyTx = res.getVerifyTx();

			JSONObject verifyJson = new JSONObject();

			//类型，验证还是引用，1验证，2引用
			verifyJson.put("type", verifyTx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY ? 1 : 2);
			//验证人
			verifyJson.put("account", verifyTx.getOperator());
			//验证时间
			verifyJson.put("time", verifyTx.getTime());
			//验证奖励
			if(verifyTx.getOutputs() == null || verifyTx.getOutputs().size() == 0) {
				verifyJson.put("reward", 0);
			} else {
				verifyJson.put("reward", verifyTx.getOutputs().get(0).getValue());
			}

			result.put("verifyTx", verifyJson);
		} else {
			result.put("verifyTx", new JSONObject());
		}
		//防伪码流转信息
		List<CirculationTransaction> circulationList = res.getCirculationList();
		JSONArray array = new JSONArray();
		for (CirculationTransaction circulationTransaction : circulationList) {
			JSONObject json = new JSONObject();
			try {
				json.put("txHash", circulationTransaction.getHash());
				json.put("tag", new String(circulationTransaction.getTag(), "utf-8"));
				json.put("content", new String(circulationTransaction.getContent(), "utf-8"));

				Address address = null;
				if(circulationTransaction.isCertAccount()) {
					address = new Address(network, network.getCertAccountVersion(), circulationTransaction.getHash160());
				} else {
					address = new Address(network, circulationTransaction.getHash160());
				}
				json.put("address", address.getBase58());
				json.put("time", circulationTransaction.getTime());

				array.put(json);
			} catch (UnsupportedEncodingException e) {
				log.error("", e);
			}
		}
		result.put("circulations", array);

		//防伪码转让信息
		if(res.isHasVerify()) {
			List<AntifakeTransferTransaction> transactionList = res.getTransactionList();

			array = new JSONArray();
			for (AntifakeTransferTransaction transferTransactionTx : transactionList) {
				JSONObject json = new JSONObject();
				try {
					//接受着
					json.put("txHash", transferTransactionTx.getHash());
					json.put("receiveAddress", Address.fromHashs(network, transferTransactionTx.getReceiveHashs()).getBase58());
					json.put("remark", new String(transferTransactionTx.getRemark(), "utf-8"));
					json.put("time", transferTransactionTx.getTime());

					Address address = null;
					if(transferTransactionTx.isCertAccount()) {
						address = new Address(network, network.getCertAccountVersion(), transferTransactionTx.getHash160());
					} else {
						address = new Address(network, transferTransactionTx.getHash160());
					}
					//转让者
					json.put("transferAddress", address.getBase58());
					array.put(json);
				} catch (UnsupportedEncodingException e) {
					log.error("", e);
				}
			}
			result.put("transactions", array);
		}else {
			result.put("transactions", new JSONArray());
		}

		List<AntifakeInfosResult> sourceList = res.getSourceList();
		if(sourceList != null && sourceList.size() > 0) {
			JSONArray sourceArray = new JSONArray();

			for (AntifakeInfosResult antifakeInfosResult : sourceList) {
				JSONObject resultJson = new JSONObject();
				setResult(resultJson, antifakeInfosResult);
				sourceArray.put(resultJson);
			}
			result.put("sources", sourceArray);
		} else {
			result.put("sources", new JSONArray());
		}
	}

	/**
	 * 防伪码验证
	 * @param params
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject verifyAntifake(JSONArray params) throws JSONException {
		JSONObject result = new JSONObject();

		try {
			//解析参数
			if(params == null || params.length() == 0) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			String antifakeContent = null;

			//如果只有一个参数，那么则认为是完整的防伪码
			if(params.length() == 1) {
				antifakeContent = params.getString(0);
			}

			double longitude = 0d;
			double latitude = 0d;
			String antifakeCode = null;
			String verifyCode = null;
			String privateKey = null;

			//如果第二个是json格式，那么json格式是经纬度
			if(params.length() == 2) {
				//2个参数，那么有可能是经纬度，有可能是账户私钥，也有可能是密码，根据类型和长度来判断
				//如果第二个参数是json格式，则是经纬度
				try {
					antifakeContent = params.getString(0);
					JSONObject localtionJson = new JSONObject(params.getString(1));
					longitude = localtionJson.getDouble("longitude");
					latitude = localtionJson.getDouble("latitude");
				} catch (Exception e) {
					//判断第二个参数是不是私钥
					String param2 = params.getString(1);
					if(param2.length() > 30) {
						privateKey = param2;
					} else {
						antifakeCode = params.getString(0);
						verifyCode = param2;
					}
				}
			} else if(params.length() == 3) {
				//3个参数，有可能第三个是经纬度，有可能第二个是经纬度，如果都没有经纬度，则是分开的指定账户验证
				String param1 = params.getString(0);
				String param2 = params.getString(1);
				String param3 = params.getString(2);
				try {
					JSONObject localtionJson = new JSONObject(param2);
					longitude = localtionJson.getDouble("longitude");
					latitude = localtionJson.getDouble("latitude");
					//第二个是经纬度，那么第三个就是私钥了
					privateKey = param3;
					antifakeContent = param1;
				} catch (Exception e) {
					try {
						JSONObject localtionJson = new JSONObject(param3);
						longitude = localtionJson.getDouble("longitude");
						latitude = localtionJson.getDouble("latitude");
						//第三个是经纬度，那么第1个就是防伪码，第二个是验证码
						antifakeCode = param1;
						verifyCode = param2;
					} catch (Exception ex) {
						//没有经纬度
						antifakeCode = param1;
						verifyCode = param2;
						privateKey = param3;
					}
				}
			} else if(params.length() == 4) {
				String param1 = params.getString(0);
				String param2 = params.getString(1);
				String param3 = params.getString(2);
				String param4 = params.getString(3);

				antifakeCode = param1;
				verifyCode = param2;

				try {
					JSONObject localtionJson = new JSONObject(param3);
					longitude = localtionJson.getDouble("longitude");
					latitude = localtionJson.getDouble("latitude");
				} catch (Exception ex) {
					result.put("success", false);
					result.put("message", "经纬度格式错误");
					return result;
				}
				privateKey = param4;
			}

			if(antifakeContent == null || antifakeContent.length()<32) {
				if(StringUtil.isEmpty(antifakeCode) && StringUtil.isEmpty(verifyCode)) {
					result.put("success", false);
					result.put("message", "防伪码为空");
					return result;
				}
				long vc = 0l;
				try {
					vc = Long.parseLong(verifyCode);
				} catch (Exception e) {
					result.put("success", false);
					result.put("message", "验证码不正确");
					return result;
				}
				AntifakeCode code = new AntifakeCode(Base58.decode(antifakeCode), vc);
				antifakeContent = code.base58Encode();
			}

			Account account = null;
			if(StringUtil.isNotEmpty(privateKey)) {
				ECKey eckey = ECKey.fromPrivate(new BigInteger(Hex.decode(privateKey)));
				Address address = AccountTool.newAddress(network, network.getSystemAccountVersion(), eckey);
				account = new Account(network);
				account.setAccountType(network.getSystemAccountVersion());
				account.setAddress(address);
				account.setEcKey(eckey);
				account.setMgPubkeys(new byte[][] {eckey.getPubKey(true)});
			}

			VerifyAntifakeCodeResult vr = accountKit.verifyAntifakeCode(antifakeContent, account, longitude, latitude);
			if(vr.isSuccess()) {
				//设置商品和商家信息
				List<ProductKeyValue> productBodyContents = vr.getProductTx().getProduct().getContents();

				JSONArray product = new JSONArray();
				for (ProductKeyValue keyValuePair : productBodyContents) {
					if(ProductKeyValue.CREATE_TIME.getCode().equals(keyValuePair.getCode())) {
						//时间
						product.put(new JSONObject().put(keyValuePair.getName(), DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)))));
					} else {
						product.put(new JSONObject().put(keyValuePair.getName(), keyValuePair.getValueToString()));
					}
				}
				result.put("product", product);

				//商家信息
				if(vr.getBusinessBody() != null) {
					JSONArray infos = new JSONArray();
					List<AccountKeyValue> businessBodyContents = vr.getBusinessBody().getContents();
					for (AccountKeyValue keyValuePair : businessBodyContents) {
						if(AccountKeyValue.LOGO.getCode().equals(keyValuePair.getCode())) {
							//图标
							infos.put(new JSONObject().put(keyValuePair.getName(), Base64.getEncoder().encodeToString(keyValuePair.getValue())));
						} else {
							infos.put(new JSONObject().put(keyValuePair.getName(), keyValuePair.getValueToString()));
						}
					}
					result.put("business", infos);
				}

				result.put("success", true);
				result.put("message", "验证成功");
				result.put("txHash", vr.getHash());
			} else {
				result.put("success", false);
				result.put("message", vr.getMessage());
				return result;
			}
		} catch (Exception e) {
			log.error("查询防伪码出错", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 添加防伪码流转信息
	 * @param antifakeCode				防伪码
	 * @param tag						流转信息标签
	 * @param content					流转信息内容
	 * @param address 					地址
	 * @param privateKeyOrPassword		私钥或者地址密码，当地址为空，这个值不为空之代表私钥，否则代表密码
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject addCirculation(String antifakeCode, String tag, String content, String address,
									 String privateKeyOrPassword) throws JSONException {

		JSONObject result = new JSONObject();

		try {

			Account account = null;
			if(StringUtil.isEmpty(address) && StringUtil.isNotEmpty(privateKeyOrPassword)) {
				ECKey eckey = ECKey.fromPrivate(new BigInteger(Hex.decode(privateKeyOrPassword)));
				Address ad = AccountTool.newAddress(network, network.getSystemAccountVersion(), eckey);
				account = new Account(network);
				account.setAccountType(network.getSystemAccountVersion());
				account.setAddress(ad);
				account.setEcKey(eckey);
				account.setMgPubkeys(new byte[][] {eckey.getPubKey(true)});
			} else if(StringUtil.isNotEmpty(address)){
				account = accountKit.getAccount(address);
			} else if(StringUtil.isEmpty(address)) {
				account = accountKit.getDefaultAccount();
			}

			if(account == null) {
				result.put("success", false);
				result.put("message", "账户不存在");
				return result;
			}

			boolean reset = false;
			if(account.isCertAccount() && account.isEncryptedOfTr()) {
				ECKey[] eckeys = account.decryptionTr(privateKeyOrPassword);
				if(eckeys == null) {
					result.put("success", false);
					result.put("message", "密码错误");
					return result;
				}
				reset = true;
			} else if(!account.isCertAccount() && account.isEncrypted()) {
				ECKey eckey = account.getEcKey();
				try {
					account.setEcKey(eckey.decrypt(privateKeyOrPassword));
				} catch (Exception e) {
					log.error("解密失败, "+e.getMessage(), e);
					account.setEcKey(eckey);
					result.put("success", false);
					result.put("message", "密码错误");
					return result;
				}
				reset = true;
			}

			BroadcastResult rs = accountKit.addCirculation(antifakeCode, tag, content, account);

			if(reset) {
				account.resetKey();
			}

			result.put("success", rs.isSuccess());
			result.put("message", rs.getMessage());
			if(rs.isSuccess()) {
				result.put("txHash", rs.getHash());
			}

		} catch (Exception e) {
			log.error("新增防伪码流转信息出错", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}

		return result;
	}

	/**
	 * 查询防伪码流转信息
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryCirculations(String antifakeCode) throws JSONException {
		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(antifakeCode)) {
			result.put("success", false);
			result.put("message", "防伪码不能为空");
			return result;
		}

		List<CirculationTransaction> circulationTxList = accountKit.queryCirculations(antifakeCode);
		if(circulationTxList == null) {
			result.put("success", false);
			result.put("message", "防伪码错误或不存在");
			return result;
		}

		JSONArray array = new JSONArray();
		for (CirculationTransaction circulationTransaction : circulationTxList) {
			JSONObject json = new JSONObject();
			try {
				json.put("txHash", circulationTransaction.getHash());
				json.put("tag", new String(circulationTransaction.getTag(), "utf-8"));
				json.put("content", new String(circulationTransaction.getContent(), "utf-8"));

				Address address = null;
				if(circulationTransaction.isCertAccount()) {
					address = new Address(network, network.getCertAccountVersion(), circulationTransaction.getHash160());
				} else {
					address = new Address(network, circulationTransaction.getHash160());
				}
				json.put("address", address.getBase58());
				json.put("time", circulationTransaction.getTime());

				array.put(json);
			} catch (UnsupportedEncodingException e) {
				log.error("", e);
			}
		}

		result.put("success", true);
		result.put("message", "ok");
		result.put("list", array);

		return result;
	}

	/**
	 * 查询防伪码流转次数
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryCirculationCount(String antifakeCode) throws JSONException {
		return queryCirculationCount(antifakeCode, null);
	}

	/**
	 * 查询防伪码流转次数
	 * @param antifakeCode
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryCirculationCount(String antifakeCode, String address) throws JSONException {
		JSONObject result = new JSONObject();
		if(StringUtil.isEmpty(antifakeCode)) {
			result.put("success", false);
			result.put("message", "防伪码不能为空");
			return result;
		}
		//验证防伪码是否正确
		byte[] antifakeCodeBytes = Base58.decode(antifakeCode);
		if(antifakeCodeBytes == null || antifakeCodeBytes.length != 20) {
			result.put("success", false);
			result.put("message", "防伪码错误");
			return result;
		}
		int count = accountKit.queryCirculationCount(antifakeCode, address);

		result.put("success", true);
		result.put("message", "ok");
		result.put("count", count);

		return result;
	}

	/**
	 * 防伪码转让
	 * @param antifakeCode		防伪码
	 * @param receiver			接收人
	 * @param remark			备注
	 * @param address			转让者账户
	 * @param privateKeyOrPassword	转让者私钥或者账户密码
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject transferAntifake(String antifakeCode, String receiver, String remark, String address,
									   String privateKeyOrPassword) throws JSONException {

		JSONObject result = new JSONObject();

		try {
			Account account = null;
			if(StringUtil.isEmpty(address) && StringUtil.isNotEmpty(privateKeyOrPassword)) {
				ECKey eckey = ECKey.fromPrivate(new BigInteger(Hex.decode(privateKeyOrPassword)));
				Address ad = AccountTool.newAddress(network, network.getSystemAccountVersion(), eckey);
				account = new Account(network);
				account.setAccountType(network.getSystemAccountVersion());
				account.setAddress(ad);
				account.setEcKey(eckey);
				account.setMgPubkeys(new byte[][] {eckey.getPubKey(true)});
			} else if(StringUtil.isNotEmpty(address)){
				account = accountKit.getAccount(address);
			} else if(StringUtil.isEmpty(address)) {
				account = accountKit.getDefaultAccount();
			}
			boolean reset = false;
			if(account.isCertAccount() && account.isEncryptedOfTr()) {
				ECKey[] eckeys = account.decryptionTr(privateKeyOrPassword);
				if(eckeys == null) {
					result.put("success", false);
					result.put("message", "密码错误");
					return result;
				}
				reset = true;
			} else if(!account.isCertAccount() && account.isEncrypted()) {
				ECKey eckey = account.getEcKey();
				try {
					account.setEcKey(eckey.decrypt(privateKeyOrPassword));
				} catch (Exception e) {
					log.error("解密失败, "+e.getMessage(), e);
					account.setEcKey(eckey);
					result.put("success", false);
					result.put("message", "密码错误");
					return result;
				}
				reset = true;
			}

			BroadcastResult rs = accountKit.transferAntifake(antifakeCode, receiver, remark, account);

			if(reset) {
				account.resetKey();
			}

			result.put("success", rs.isSuccess());
			result.put("message", rs.getMessage());
			if(rs.isSuccess()) {
				result.put("txHash", rs.getHash());
			}

		} catch (Exception e) {
			log.error("防伪码转让出错", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	/**
	 * 查询防伪码转让记录
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryTransfers(String antifakeCode) throws JSONException {
		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(antifakeCode)) {
			result.put("success", false);
			result.put("message", "防伪码不能为空");
			return result;
		}

		List<AntifakeTransferTransaction> transferTransactionTxList = accountKit.queryTransfers(antifakeCode);
		if(transferTransactionTxList == null) {
			result.put("success", false);
			result.put("message", "防伪码错误或不存在");
			return result;
		}

		JSONArray array = new JSONArray();
		for (AntifakeTransferTransaction transferTransactionTx : transferTransactionTxList) {
			JSONObject json = new JSONObject();
			try {
				//接受着
				json.put("txHash", transferTransactionTx.getHash());
				json.put("receiveAddress", Address.fromHashs(network, transferTransactionTx.getReceiveHashs()).getBase58());
				json.put("remark", new String(transferTransactionTx.getRemark(), "utf-8"));
				json.put("time", transferTransactionTx.getTime());

				Address address = null;
				if(transferTransactionTx.isCertAccount()) {
					address = new Address(network, network.getCertAccountVersion(), transferTransactionTx.getHash160());
				} else {
					address = new Address(network, transferTransactionTx.getHash160());
				}
				//转让者
				json.put("transferAddress", address.getBase58());
				array.put(json);
			} catch (UnsupportedEncodingException e) {
				log.error("", e);
			}
		}

		result.put("success", true);
		result.put("message", "ok");
		result.put("list", array);

		return result;
	}

	/**
	 * 查询防伪码转让次数
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryTransferCount(String antifakeCode) throws JSONException {
		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(antifakeCode)) {
			result.put("success", false);
			result.put("message", "防伪码不能为空");
			return result;
		}
		//验证防伪码是否正确
		byte[] antifakeCodeBytes = Base58.decode(antifakeCode);
		if(antifakeCodeBytes == null || antifakeCodeBytes.length != 20) {
			result.put("success", false);
			result.put("message", "防伪码错误");
			return result;
		}
		int count = accountKit.queryTransferCount(antifakeCode);

		result.put("success", true);
		result.put("message", "ok");
		result.put("count", count);

		return result;
	}

	/**
	 * 查询防伪码拥有者
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject queryAntifakeOwner(String antifakeCode) throws JSONException {

		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(antifakeCode)) {
			result.put("success", false);
			result.put("message", "防伪码不能为空");
			return result;
		}
		//验证防伪码是否正确
		byte[] antifakeCodeBytes = Base58.decode(antifakeCode);
		if(antifakeCodeBytes == null || antifakeCodeBytes.length != 20) {
			result.put("success", false);
			result.put("message", "防伪码错误");
			return result;
		}

		Address ownerAddress = accountKit.queryAntifakeOwner(antifakeCode);
		if(ownerAddress == null) {
			result.put("success", false);
			result.put("message", "没有找到拥有者，可能原因是防伪码错误，或者没有被验证");
		} else {
			result.put("success", true);
			result.put("message", "ok");
			result.put("owner", ownerAddress.getBase58());
		}

		return result;
	}

	/**
	 * 认证商家关联子账户
	 * @param relevancer
	 * @param alias
	 * @param content
	 * @param trpw
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject relevanceSubAccount(String relevancer, String alias, String content, String trpw, String address)
			throws JSONException {

		BroadcastResult res = accountKit.relevanceSubAccount(relevancer, alias, content, trpw, address);

		JSONObject result = new JSONObject();

		result.put("success", res.isSuccess());
		result.put("message", res.getMessage());

		if(res.getHash() != null) {
			result.put("txHash", res.getHash());
		}

		return result;
	}

	/**
	 * 解除子账户的关联
	 * @param relevancer
	 * @param hashId
	 * @param trpw
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject removeSubAccount(String relevancer, String hashId, String trpw, String address)
			throws JSONException {

		BroadcastResult res = accountKit.removeSubAccount(relevancer, hashId, trpw, address);

		JSONObject result = new JSONObject();

		result.put("success", res.isSuccess());
		result.put("message", res.getMessage());

		if(res.getHash() != null) {
			result.put("txHash", res.getHash());
		}

		return result;
	}

	/**
	 * 获取认证商家子账户列表
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject getSubAccounts(String address) throws JSONException {
		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(address)) {
			result.put("success", false);
			result.put("message", "地址不能为空");
			return result;
		}
		//账户是否正确
		Address add = new Address(network, address);
		if(add == null || !add.isCertAccount()) {
			result.put("success", false);
			result.put("message", "账户地址不正确，必须是认证账户");
			return result;
		}

		JSONArray array = new JSONArray();

		List<RelevanceSubAccountTransaction> subAccountList = accountKit.getSubAccounts(address);
		if(subAccountList == null) {
			result.put("success", true);
			result.put("message", "ok");
			result.put("list", array);

			return result;
		}

		for (RelevanceSubAccountTransaction subAccountTx : subAccountList) {
			JSONObject json = new JSONObject();
			try {
				//接受着
				json.put("address", Address.fromHashs(network, subAccountTx.getRelevanceHashs()).getBase58());
				json.put("alias", new String(subAccountTx.getAlias(), "utf-8"));
				json.put("content", new String(subAccountTx.getContent(), "utf-8"));
				json.put("txHash", subAccountTx.getHash());

				array.put(json);
			} catch (Exception e) {
				log.error("", e);
			}
		}

		result.put("success", true);
		result.put("message", "ok");
		result.put("list", array);

		return result;
	}

	/**
	 * 获取认证商家子账户数量
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject getSubAccountCount(String address) throws JSONException {
		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(address)) {
			result.put("success", false);
			result.put("message", "账户不能为空");
			return result;
		}

		//账户是否正确
		Address add = new Address(network, address);
		if(add == null || !add.isCertAccount()) {
			result.put("success", false);
			result.put("message", "账户地址不正确，必须是认证账户");
			return result;
		}
		int count = accountKit.getSubAccountCount(address);

		result.put("success", true);
		result.put("message", "ok");
		result.put("count", count);

		return result;
	}

	/**
	 * 检查是否是商家的子账户
	 * @param certAddress
	 * @param address
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject checkIsSubAccount(String certAddress, String address) throws JSONException {

		JSONObject result = new JSONObject();

		if(StringUtil.isEmpty(certAddress) || StringUtil.isEmpty(address)) {
			result.put("success", false);
			result.put("message", "账户不能为空");
			return result;
		}

		try {
			//账户是否正确
			Address add1 = new Address(network, certAddress);
			if(add1 == null || !add1.isCertAccount()) {
				result.put("success", false);
				result.put("message", "账户地址不正确，必须是认证账户");
				return result;
			}
			new Address(network, address);
		} catch (Exception e) {
			result.put("success", false);
			result.put("message", "账户不正确");
			return result;
		}

		BroadcastResult res = accountKit.checkIsSubAccount(certAddress, address);

		result.put("success", res.isSuccess());
		result.put("message", res.getMessage());
		if(res.isSuccess()) {
			result.put("txHash", res.getHash());
		}

		return result;
	}

	/**
	 * 通过别名获取账户
	 * @param alias
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject getAccountByAlias(String alias) throws JSONException {
		JSONObject json = new JSONObject();

		AccountStore accountStore = chainstateStoreProvider.getAccountInfoByAlias(alias.getBytes());

		if(accountStore == null) {
			json.put("success", false);
			json.put("message", "别名不存在");
		} else {
			json.put("success", true);
			json.put("message", "ok");
			json.put("account", accountStore.getAddress());
		}

		return json;
	}

	/**
	 * 通过账户获取别名
	 * @param account
	 * @return JSONObject
	 * @throws JSONException
	 */
	@Override
	public JSONObject getAliasByAccount(String account) throws JSONException {
		JSONObject json = new JSONObject();

		Address address = null;
		try {
			address = Address.fromHashs(network, Base58.decode(account));
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", "账户不正确");
			return json;
		}
		AccountStore accountStore = chainstateStoreProvider.getAccountInfo(address.getHash160());

		if(accountStore == null || accountStore.getAlias() == null) {
			json.put("success", false);
			json.put("message", "账户没有关联别名");
		} else {
			json.put("success", true);
			json.put("message", "ok");
			try {
				json.put("alias", new String(accountStore.getAlias(), "utf-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		return json;
	}

	/**
	 * 获取帐户列表
	 * @return JSONArray
	 */
	public JSONArray getAccounts() throws JSONException {
		JSONArray array = new JSONArray();
		for (Account account : accountKit.getAccountList()) {
			array.put(account.getAddress().getBase58());
		}
		return array;
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
	 * @param address
	 * @return Coin[]
	 */
	@Override
	public Coin[] getAccountBalance(String address) {
		Coin[] balances = new Coin[2];
		balances[0] = accountKit.getCanUseBalance(address);
		balances[1] = accountKit.getCanNotUseBalance(address);
		return balances;
	}

	/**
	 * 获取所有账户的总余额
	 * @param address
	 * @return Coin[]
	 */
	public Coin[] getTotalBalance(){
		Coin[] balances = new Coin[2];
		balances[0] = accountKit.getTotalCanUseBalance();
		balances[1] = accountKit.getTotalCanNotUseBalance();
		return balances;
	}

	/**
	 * 获取账户的信用
	 * @param address
	 * @return long
	 * @throws VerificationException
	 */
	@Override
	public long getAccountCredit(String address) throws VerificationException {
		AccountStore accountStore = accountKit.getAccountInfo(address);
		if(accountStore == null) {
			return 0;
		}
		return accountStore.getCert();
	}

	/**
	 * 获取账户的详细信息
	 * @param address
	 * @return long
	 * @throws JSONException VerificationException
	 */
	public JSONObject getAccountInfo(String address) throws JSONException, VerificationException {
		JSONObject json = new JSONObject();

		AccountStore info = accountKit.getAccountInfo(address);
		json.put("type", info.getType());
		json.put("adderss", new Address(network, info.getType(), info.getHash160()).getBase58());
		json.put("status",info.getStatus());
		Coin[] blanaces = getAccountBalance(address);
		json.put("blanace", blanaces[0].add(blanaces[1]).value);
		json.put("canUseBlanace", blanaces[0].value);
		json.put("cannotUseBlanace", blanaces[1].value);
		json.put("cert", info.getCert());
		json.put("level",info.getLevel());
		json.put("supervisor",(info.getSupervisor()==null||info.getSupervisor().length!=20)?"":new Address(network,network.getCertAccountVersion(), info.getSupervisor()).getBase58());

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
			//trarrays.put(Hex.encode(info.getPubkeys()[3]));

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

		accountKit.decryptAccount(password,null);

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
		if(!accountKit.isWalletEncrypted()) {
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
	 * @param address
	 * @return JSONArray
	 * @throws JSONException
	 */
	public JSONArray getTransaction(String address) throws JSONException {
		List<TransactionStore> mineList = transactionStoreProvider.getMineTxList(address);

		JSONArray array = new JSONArray();

		long bestHeight = network.getBestBlockHeight();
		List<Account> accountList = accountKit.getAccountList();

		for (TransactionStore transactionStore : mineList) {
			array.put(txConver(transactionStore, bestHeight, accountList));
		}

		return array;
	}


	public JSONArray getTransferTx(Long height, Long confirm, String address) throws JSONException {
		List<TransactionStore> mineList = transactionStoreProvider.getMineTxList(address);

		JSONArray array = new JSONArray();

		long bestHeight = network.getBestBlockHeight();
		List<Account> accountList = accountKit.getAccountList();
		//这里只取与我相关的转账交易，且确认数等于confirm的
		for (TransactionStore transactionStore : mineList) {
			if(transactionStore.getTransaction().getType() == Definition.TYPE_PAY) {
				if(transactionStore.getHeight() == height &&  bestHeight - transactionStore.getHeight() > confirm) {
					array.put(txConver(transactionStore, bestHeight, accountList));
				}
			}
		}
		return array;
	}

	public JSONArray listtransactions(Integer limit, Integer confirm, String address) throws JSONException {
		JSONArray array = new JSONArray();

		List<TransactionStore> mineList = transactionStoreProvider.getMineTxList(address);
		//依然按照交易时间排序
		if(mineList.size() > 0) {
			Collections.sort(mineList, new Comparator<TransactionStore>() {
				@Override
				public int compare(TransactionStore o1, TransactionStore o2) {
					long v1 = o1.getTransaction().getTime();
					long v2 = o2.getTransaction().getTime();
					if(v1 == v2) {
						return 0;
					} else if(v1 > v2) {
						return -1;
					} else {
						return 1;
					}
				}
			});

			//这里只取与我相关的转账交易，且确认数等于confirm的
			long bestHeight = network.getBestBlockHeight();
			int count = 0;
			List<Account> accountList = accountKit.getAccountList();
			for(int i = 0;i < mineList.size(); i++) {
				TransactionStore txStore = mineList.get(i);
				if(txStore.getTransaction().getType() == Definition.TYPE_PAY) {
					if(bestHeight - txStore.getHeight() > confirm) {
						array.put(txConver(txStore, bestHeight, accountList));
						count++;
					}
				}
				if(count == limit) {
					break;
				}
			}
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
		if(txs.getTransaction().getType() == 0) {
			txs = null;
		}
		if(txs == null) {
			JSONObject json = new JSONObject();
			json.put("message", "not found");
			return json;
		}
		return txConver(txs);
	}

	/**
	 * 锁仓
	 * @param money
	 * @param lockTime
	 * @param address
	 * @param password
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject lockMoney(Coin money, long lockTime, String address, String password, String remark)throws JSONException {
		JSONObject json = new JSONObject();

		if(StringUtil.isEmpty(money)) {
			json.put("success", false);
			json.put("message", "params error");
			return json;
		}

		try {
			BroadcastResult br = accountKit.lockMoney(money, lockTime, address, password, remark);

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
	 * 发送交易
	 * @param toAddress
	 * @param money
	 * @param address
	 * @param password
	 * @param remark
	 * @param passwordOrRemark
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject sendMoney(String toAddress, String money, String address, String password, String remark, String passwordOrRemark) throws JSONException {
		JSONObject json = new JSONObject();

		if(StringUtil.isEmpty(toAddress) || StringUtil.isEmpty(money)) {
			json.put("success", false);
			json.put("message", "参数缺少");
			return json;
		}

		Coin moneyCoin = null;
		Coin feeCoin = null;
		try {
			moneyCoin = Coin.parseCoin(money);
			feeCoin = Coin.parseCoin("0.1");
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", "金额不正确");
			return json;
		}

		Account account = accountKit.getAccount(address);

		if(account == null) {
			json.put("success", false);
			json.put("message", "账户不存在");
			return json;
		}

		//账户是否加密
		if((account.getAccountType() == network.getSystemAccountVersion() && account.isEncrypted()) ||
				(account.getAccountType() == network.getCertAccountVersion() && account.isEncryptedOfTr())) {
			if(StringUtil.isEmpty(password) && StringUtil.isEmpty(passwordOrRemark)) {
				json.put("needInput", true);
				json.put("inputType", 1);	//输入密码
				json.put("inputTip", "输入钱包密码进行转账");
				return json;
			} else {
				if(StringUtil.isEmpty(password)) {
					password = passwordOrRemark;
				} else if(StringUtil.isNotEmpty(passwordOrRemark) && StringUtil.isEmpty(remark)) {
					remark = passwordOrRemark;
				}
				Result re = accountKit.decryptAccount(password,address);
				if(!re.isSuccess()) {
					json.put("success", false);
					json.put("message", re.getMessage());
					return json;
				}
			}
		} else if(StringUtil.isEmpty(remark) && StringUtil.isNotEmpty(passwordOrRemark)) {
			remark = passwordOrRemark;
		}

		try {
			BroadcastResult br = accountKit.sendMoney(toAddress, moneyCoin, feeCoin, remark == null ? null:remark.getBytes(), address, password);

			json.put("success", br.isSuccess());
			json.put("message", br.getMessage());
			json.put("txHash", br.getHash());
			return json;
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", e.getMessage());
			return json;
		} finally {
			accountKit.resetKeys();
		}
	}


	public JSONObject sendMoneyToAddress(JSONArray toaddressAndCoins,String pass,String remark) throws JSONException{
		JSONObject json = new JSONObject();

		if(toaddressAndCoins==null || toaddressAndCoins.length()==0){
			json.put("success", false);
			json.put("message", "参数缺少");
			return json;
		}
		Coin feeCoin = Coin.parseCoin("0.1");
		try {
			BroadcastResult br = accountKit.sendtoAddress(toaddressAndCoins, feeCoin, pass,remark == null ? null:remark.getBytes("utf-8"));

			json.put("success", br.isSuccess());
			json.put("message", br.getMessage());
			json.put("txHash", br.getHash());
			return json;
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", e.getMessage());
			return json;
		}
	}

	public JSONObject lockReward(String toAddress, Coin money, String address, String password, String remark, long lockTime) throws JSONException {
		JSONObject json = new JSONObject();
		try {

			//锁仓的时间必须大于24小时
			if(lockTime - TimeService.currentTimeSeconds() < 24 * 60 * 60) {
				throw new VerificationException("锁仓时间必须大于24小时");
			}

			Account account = null;
			if(StringUtil.isEmpty(address)) {
				account = accountKit.getDefaultAccount();
			} else {
				account = accountKit.getAccount(address);
			}

			if(account == null) {
				json.put("success", false);
				json.put("message", "账户不存在");
				return json;
			}

			//账户是否加密
			Result re = accountKit.decryptAccount(password,address,2);
			if(!re.isSuccess()) {
				json.put("success", false);
				json.put("message", re.getMessage());
				return json;
			}

			Coin feeCoin = Definition.MIN_PAY_FEE;

			BroadcastResult br = accountKit.sendLockMoney(toAddress, money, feeCoin, remark.getBytes(), address, password,lockTime);

			json.put("success", br.isSuccess());
			json.put("message", br.getMessage());
			json.put("txHash", br.getHash());
			return json;
		} catch (Exception e) {
			e.printStackTrace();
			json.put("success", false);
			json.put("message", e.getMessage());
			return json;
		} finally {
			accountKit.resetKeys();
		}
	}

	/**
	 * 广播交易
	 */
	@Override
	public JSONObject broadcast(String txContent) throws JSONException {
		JSONObject json = new JSONObject();

		try {
			Transaction tx = network.getDefaultSerializer().makeTransaction(Hex.decode(txContent), 0);

			//如果是转账交易，检查手续费
			if(tx.getType() == Definition.TYPE_PAY) {
				if(tx.getFee().compareTo(Definition.MIN_PAY_FEE) < 0) {
					json.put("success", false);
					json.put("message", "交易转账手续费至少为0.1个INS币");
					return json;
				}
			}

			try {
				MempoolContainer.getInstace().add(tx);

				BroadcastResult br = peerKit.broadcast(tx).get();

				json.put("success", br.isSuccess());
				json.put("message", br.getMessage());

				return json;
			} catch (Exception e) {
				MempoolContainer.getInstace().remove(tx.getHash());

				json.put("success", false);
				json.put("message", e.getMessage());
				return json;
			}
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", e.getMessage());
		}

		return json;
	}

	/**
	 * 广播交易
	 */
	@Override
	public JSONObject broadcastTransferTransaction(String amount,String privateKey, String toAddress, String remark, JSONArray jsonArray) throws JSONException {
		JSONObject json = new JSONObject();
		Account account = null;
		try {

			//验证金额是否正确
			Coin moneyCoin = null;             //转账的币
			Coin feeCoin = Definition.MIN_PAY_FEE;               //手续费
			try {
				moneyCoin = Coin.parseCoin(amount);
			} catch (Exception e) {
				throw new VerificationException("金额不正确");
			}
			if(moneyCoin.longValue() <= 0) {
				throw new VerificationException("金额不正确");
			}

			//验证接收地址
			Address receiveAddress = null;
			try {
				receiveAddress = Address.fromBase58(network, toAddress);
			} catch (Exception e) {
				throw new VerificationException("接收地址不正确");
			}

			//通过私钥获取我的地址
			ECKey eckey = ECKey.fromPrivate(new BigInteger(Hex.decode(privateKey)));
			Address myAddress = AccountTool.newAddress(network, eckey);

			account = new Account(network);
			account.setAddress(myAddress);
			account.setEcKey(eckey);
			account.setMgPubkeys(new byte[][]{ eckey.getPubKey()});

			//不能给自己转账
			if(Arrays.equals(receiveAddress.getHash160(), myAddress.getHash160())) {
				throw new VerificationException("不能给自己转账");
			}

			//转换交易输出，并检查交易输出是否已被使用
			List<TransactionOutput> fromOutputs = new ArrayList<>();
			for(int j = 0; j < jsonArray.length(); j++) {
				JSONObject object = jsonArray.getJSONObject(j);
				if(!myAddress.getBase58().equals(object.getString("walletAddress"))) {
					throw new VerificationException("未花费交易与私钥不匹配");
				}

				String txHash = object.getString("txHash");
				TransactionStore txStore = blockStoreProvider.getTransaction(Hex.decode(txHash));

				if(txStore == null) {
					throw new VerificationException("txHash：" + txHash + "未查询到相关交易");
				}

				Transaction tx = txStore.getTransaction();
				if(!tx.isPaymentTransaction()) {
					throw new VerificationException("txHash：" + txHash + "交易类型错误");
				}
				//如果交易不可用，则跳过
				if(tx.getLockTime() < 0l
						|| (tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > TimeService.currentTimeSeconds())
						|| (tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > network.getBestHeight())) {
					continue;
				}

				Integer index = object.getInt("outputIndex");
				try {
					TransactionOutput output = tx.getOutput(index);
					//判断交易是否已花费
					if(isSpent(tx, output, myAddress.getHash160())) {
						throw new VerificationException("txHash：" + txHash + "交易输出已花费或不可用index:" + index);
					}
					fromOutputs.add(output);
				}catch (ArrayIndexOutOfBoundsException ae) {
					throw new VerificationException("txHash：" + txHash + "未查询到相关交易输出index:" + index);
				}
			}
			//广播交易
			BroadcastResult br = accountKit.broadcastTransferTransaction(account, moneyCoin, feeCoin, fromOutputs, receiveAddress,remark);
			json.put("success", br.isSuccess());
			json.put("message", br.getMessage());

		}catch (Exception e) {
			json.put("success", false);
			json.put("message", e.getMessage());
		}
		return json;
	}

	//判断交易输出是否已花费
	private boolean isSpent(Transaction tx, TransactionOutput output, byte[]hash160) {
		Script script = output.getScript();
		if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
			byte[]txid = tx.getHash().getBytes();
			byte[] key = new byte[txid.length + 1];
			System.arraycopy(txid, 0, key, 0, key.length - 1);
			key[key.length - 1] = (byte) output.getIndex();
			byte[]status = chainstateStoreProvider.getBytes(key);
			if(status[0] == TransactionStore.STATUS_USED) {
				return true;
			}
			//本笔输出是否可用
			long lockTime = output.getLockTime();
			if(lockTime < 0l
					|| (lockTime > Definition.LOCKTIME_THRESHOLD && lockTime > TimeService.currentTimeSeconds())
					|| (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > network.getBestHeight()) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 广播交易 - 交易内容存放在文件里面
	 */
	@Override
	public JSONObject broadcastfromfile(String filepath) throws JSONException {

		JSONObject json = new JSONObject();

		File file = new File(filepath);
		if(!file.exists()) {
			json.put("success", false);
			json.put("message", "文件不存在");
			return json;
		}
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = br.readLine()) != null) {
				sb.append(line);
			}
			br.close();
		} catch (Exception e) {
			json.put("success", false);
			json.put("message", "读取文件错误");
			return json;
		}
		return broadcast(sb.toString());
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
	 * 获取共识节点数量
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getConsensusCount() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("count", consensusPool.getCurrentConsensus());
		json.put("success", true);
		return json;
	}

	/**
	 * 查看当前共识状态
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getConsensusStatus() throws JSONException {
		JSONObject json = new JSONObject();

		if(accountKit.checkConsensusing(null) ) {
			ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
			consensusMeeting.waitMeeting();
			MiningInfos miningInfo = consensusMeeting.getMineMiningInfos();
			String periodStartTime = DateUtil.convertDate(new Date(miningInfo.getPeriodStartTime()*1000), "HH:mm:ss");
			String beginTime = DateUtil.convertDate(new Date(miningInfo.getBeginTime()*1000), "HH:mm:ss");
			String endTime = DateUtil.convertDate(new Date(miningInfo.getEndTime()*1000), "HH:mm:ss");

			String msg = null;
			if(miningInfo.getBeginTime() == 0l) {
				if(consensusMeeting.getAccount() == null) {
					msg = "但钱包已加密不能打包，需要再次申请共识";
				} else {
					msg = "正在等待当前轮结束：预计" + (miningInfo.getPeriodEndTime() - TimeService.currentTimeSeconds()) + "秒";
				}
			} else if(TimeService.currentTimeSeconds() < miningInfo.getBeginTime()) {
				msg = "正在排队共识,预计" + (miningInfo.getBeginTime() - TimeService.currentTimeSeconds()) + "秒,当前轮开始时间：" + periodStartTime + ",我的共识时间：" + beginTime + " - " + endTime;
			} else if(TimeService.currentTimeSeconds() > miningInfo.getEndTime()) {
				msg = "正在等待进入下一轮共识队列：预计" + (miningInfo.getPeriodEndTime() - TimeService.currentTimeSeconds()) + "秒";
			} else {
				msg = "正在打包中: 倒计时 " + (miningInfo.getEndTime() - TimeService.currentTimeSeconds()) + "秒";
			}

			json.put("message", "当前已在共识中。" + msg);
		} else {
			json.put("message", "未参与共识");
		}

		json.put("success", true);
		return json;
	}

	/**
	 * 注册共识
	 * @param password
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject regConsensus(String password, String consensusAddress) throws JSONException {

		JSONObject json = new JSONObject();

		//判断信用是否足够
		long cert = getAccountCredit(null);

		BlockHeader bestBlockHeader = network.getBestBlockHeader();
		long consensusCert = ConsensusCalculationUtil.getConsensusCredit(bestBlockHeader.getHeight());

		if(cert < consensusCert) {
			json.put("success", false);
			json.put("message", "信用值不够,不能参加共识,当前"+cert+",共识所需"+consensusCert+",还差"+(consensusCert - cert));
			return json;
		}

		//判断账户是否加密
		if(accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR) && password == null) {
			json.put("needInput", true);
			json.put("inputType", 1);	//输入密码
			json.put("inputTip", "输入钱包密码参与共识");
			return json;
		}
		byte[] hash160 = null;
		if(StringUtil.isEmpty(consensusAddress)) {
			hash160 = accountKit.getDefaultAccount().getAddress().getHash160();
		} else {
			try {
				hash160 = Address.fromBase58(network, consensusAddress).getHash160();
			} catch (Exception e) {
				throw new VerificationException("错误的共识地址");
			}
		}
		//当前是否已经在共识了
		//延迟重置账户
		ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
		if(accountKit.checkConsensusing(hash160) && consensusMeeting.getAccount() != null) {
			json.put("success", false);
			json.put("message", "当前已经在共识状态中了");
			return json;
		}

		//解密钱包
		if(accountKit.isWalletEncrypted()) {
			Result result = accountKit.decryptAccount(password, null, Definition.TX_VERIFY_TR);
			if(!result.isSuccess()) {
				json.put("success", false);
				json.put("message", result.getMessage());
				return json;
			}
		}

		if(accountKit.checkConsensusing(hash160) && consensusMeeting.getAccount() == null) {
			consensusMeeting.waitMining();
			accountKit.resetKeys();
			json.put("success", true);
			json.put("message", "成功加入共识");
			return json;
		}

		try {
			Result result = accountKit.registerConsensus(consensusAddress);
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


	public JSONObject regconsensusFee() throws JSONException {
		JSONObject json = new JSONObject();
		try {
			BlockHeader bestBlockHeader = network.getBestBlockHeader();
			int currentConsensusSize = bestBlockHeader.getPeriodCount();
			//共识保证金
			Coin recognizance = ConsensusCalculationUtil.calculatRecognizance(currentConsensusSize, bestBlockHeader.getHeight());
			json.put("success", true);
			json.put("recognizance", new BigDecimal(recognizance.value).movePointLeft(8));
		}catch (Exception e) {
			log.error("共识请求出错", e);
			json.put("sucess", false);
			json.put("message", "共识查询出错");
		}
		return json;
	}
	/**
	 * 退出共识
	 * @param password
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject remConsensus(String address,String password) throws JSONException {

		JSONObject json = new JSONObject();

		//判断信用是否足够

		//当前是否已经在共识了
		if(!accountKit.checkConsensusing(Address.fromBase58(network,address).getHash160())) {
			json.put("success", false);
			json.put("message", "当前没有在共识中");
			return json;
		}

		//判断账户是否加密
		if(accountKit.accountIsEncrypted(address,Definition.TX_VERIFY_TR) && password == null) {
			json.put("needInput", true);
			json.put("inputType", 1);	//输入密码
			json.put("inputTip", "输入钱包密码退出共识");
			return json;
		}

		//解密钱包
		if(accountKit.accountIsEncrypted(address,1)) {
			Result result = accountKit.decryptAccount(password,address,Definition.TX_VERIFY_TR);
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


	/**
	 * 获取连接节点数量
	 * @return JSONObject
	 */
	public JSONObject getPeerCount() throws JSONException {

		JSONObject json = new JSONObject();

		int totalCount = peerKit.getConnectedCount();
		int availableCount = peerKit.getAvailablePeersCount();

		json.put("totalCount", totalCount).put("availableCount", availableCount);

		return json;
	}

	/**
	 * 通过公钥得到地址
	 * @param pubkey
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getAddressByPubKey(String pubkey) throws JSONException {

		JSONObject json = new JSONObject();

		if(StringUtil.isEmpty(pubkey)) {
			return json.put("success", false).put("message", "公钥为空");
		}

		try {
			json.put("address", AccountTool.newAddress(network, ECKey.fromPublicOnly(Hex.decode(pubkey))).getBase58());
			json.put("success", true).put("message", "ok");
		} catch (Exception e) {
			json.put("success", false).put("message", e.getMessage());
		}
		return json;
	}

	/**
	 * 获取私钥
	 * @param address
	 * @param password
	 * @return JSONObject
	 */
	public JSONObject getPrivatekey(String address, String password) throws JSONException {
		JSONObject json = new JSONObject();

		Account account = null;
		if(StringUtil.isNotEmpty(address)) {
			account = accountKit.getAccount(address);
		} else {
			account = accountKit.getDefaultAccount();
		}

		if(account == null) {
			json.put("success", false).put("message", "账户不存在");
			return json;
		}

		if(account.getAccountType() == network.getCertAccountVersion()) {
			json.put("success", false).put("message", "该方法不支持认证账号");
			return json;
		}

		if(account.isEncrypted() && StringUtil.isEmpty(password)) {
			json.put("success", false).put("message", "账户已加密");
			return json;
		}

		if(account.isEncrypted()) {
			//普通账户的解密
			account.resetKey(password);
			ECKey eckey = account.getEcKey();
			try {
				account.setEcKey(eckey.decrypt(password));
			} catch (Exception e) {
				log.error("解密失败, "+e.getMessage(), e);
				account.setEcKey(eckey);
				json.put("success", false).put("message", "密码错误");
				return json;
			}
		}

		String privateKey = account.getEcKey().getPrivateKeyAsHex();

		account.resetKey();

		json.put("privateKey", privateKey);
		json.put("success", true).put("message", "ok");
		return json;
	}

	/**
	 * 重建数据
	 * @return JSONObject
	 */
	public JSONObject resetData() throws JSONException {
		JSONObject json = new JSONObject();

		new Thread("reset data thread") {
			public void run() {
				blockStoreProvider.resetData();
			};
		}.start();

		json.put("success", true).put("message", "数据重建已启动，异步执行中，查看logo关注进度和结果");
		return json;
	}

	public JSONObject validateAddress(String address) throws JSONException {
		JSONObject json = new JSONObject();
		Account account = accountKit.getAccount(address);
		if(account == null) {
			json.put("ismine", false);
            json.put("isscript", false);
		}else {
			if(account.getAddress().getVersion() == network.getCertAccountVersion()) {
				json.put("ismine", false);
                json.put("isscript", false);
			}else {
				json.put("ismine", true);
				if(account.isEncrypted()) {
                    json.put("isscript", true);
                }else {
                    json.put("isscript", false);
                }
			}
		}
		json.put("success", true);
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
		json.put("lockTime", tx.getLockTime());

		json.put("height", txs.getHeight());
		json.put("confirmation", bestHeight - txs.getHeight());
		json.put("remark",tx.getRemark()==null?"":new String(tx.getRemark(),Utils.UTF_8));

		if(tx instanceof BaseCommonlyTransaction) {
			BaseCommonlyTransaction bctx = (BaseCommonlyTransaction) tx;
			try {
				json.put("address", bctx.getScriptSig().getAccountBase58(network));
			} catch (Exception e) {
				log.error("出错 {}", bctx, e);
			}
			json.put("scriptSig", bctx.getScriptSig());
		}

		if(tx.isPaymentTransaction()) {

			//是否是转出
			boolean isSendout = false;

			JSONArray inputArray = new JSONArray();
			JSONArray outputArray = new JSONArray();

			Coin inputFee = Coin.ZERO;
			Coin outputFee = Coin.ZERO;


			List<TransactionInput> inputs = tx.getInputs();
			if(tx.getType() != Definition.TYPE_COINBASE && inputs != null && inputs.size() > 0) {
				for (TransactionInput input : inputs) {

					if(input.getFroms() == null || input.getFroms().size() == 0) {
						continue;
					}

					for (TransactionOutput from : input.getFroms()) {
						JSONObject inputJson = new JSONObject();

						inputJson.put("fromTx", from.getParent().getHash());
						inputJson.put("fromIndex", from.getIndex());

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
			}

			List<TransactionOutput> outputs = tx.getOutputs();

			for (TransactionOutput output : outputs) {
				Script script = output.getScript();

				Coin value = Coin.valueOf(output.getValue());
				outputFee = outputFee.add(value);

				JSONObject outputJson = new JSONObject();
				outputJson.put("value", value.value);
				outputJson.put("lockTime", output.getLockTime());

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
			//如果是资产注册交易，这里需做特别转换
			if(tx.getType() == Definition.TYPE_ASSETS_REGISTER) {
				AssetsRegisterTransaction assetsRegisterTx = (AssetsRegisterTransaction)tx;
				json.put("name", new String(assetsRegisterTx.getName(), Utils.UTF_8));
				json.put("description", new String(assetsRegisterTx.getDescription(), Utils.UTF_8));
				json.put("code", new String(assetsRegisterTx.getCode(), Utils.UTF_8));
				json.put("logo", new String(assetsRegisterTx.getLogo(), Utils.UTF_8));
				json.put("remark", new String(assetsRegisterTx.getRemark(), Utils.UTF_8));
			}
			else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				AntifakeCodeMakeTransaction atx = (AntifakeCodeMakeTransaction) tx;
				TransactionStore ptx = null;
				if(atx.getHasProduct()==0) {
					ptx = blockStoreProvider.getTransaction(atx.getProductTx().getBytes());
				}
				//必要的NPT验证
				if(ptx != null) {
					Product product = ((ProductTransaction)ptx.getTransaction()).getProduct();
					json.put("productName", product.getName());
					json.put("productTx", ptx.getTransaction().getHash());
				}
				if(atx.getRewardCoin() != null) {
					json.put("rewardCoin", atx.getRewardCoin().toText());
				} else {
					json.put("rewardCoin", 0);
				}
				try {
					json.put("antifakeCode", Base58.encode(atx.getAntifakeCode()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {

				AntifakeCodeVerifyTransaction atx = (AntifakeCodeVerifyTransaction) tx;

				byte[] makebind = accountKit.getChainstate(atx.getAntifakeCode());
				byte[] makebyte = new byte[Sha256Hash.LENGTH];
				byte[] bindbyte = new byte[Sha256Hash.LENGTH];
				System.arraycopy(makebind,0,makebyte,0,Sha256Hash.LENGTH);
				if(makebind.length == 2*Sha256Hash.LENGTH){
					System.arraycopy(makebind,Sha256Hash.LENGTH,bindbyte,0,Sha256Hash.LENGTH);
				}

				//必要的NPT验证
				if(makebyte == null) {
					return json;
				}
				TransactionStore makeCodeTxStore = null;
				AntifakeCodeMakeTransaction makeCodeTx = null;
				AntifakeCodeBindTransaction bindCodeTx = null;
				TransactionStore productTxStore = null;
				if(makebind.length == 2*Sha256Hash.LENGTH){
					makeCodeTxStore = accountKit.getTransaction(Sha256Hash.wrap(bindbyte));
					bindCodeTx = (AntifakeCodeBindTransaction) makeCodeTxStore.getTransaction();
					productTxStore = accountKit.getTransaction(bindCodeTx.getProductTx());
				}else {
					makeCodeTxStore = accountKit.getTransaction(Sha256Hash.wrap(makebyte));
					makeCodeTx = (AntifakeCodeMakeTransaction)makeCodeTxStore.getTransaction();
					productTxStore =  accountKit.getTransaction(makeCodeTx.getProductTx());
				}
				if(makeCodeTxStore == null) {
					return json;
				}

				if(productTxStore == null) {
					return json;
				}
				try {
					if(makebind.length == 2*Sha256Hash.LENGTH) {
						json.put("antifakeCode", Base58.encode(bindCodeTx.getAntifakeCode()));
					}else{
						json.put("antifakeCode", Base58.encode(makeCodeTx.getAntifakeCode()));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				json.put("antifakeMakeTx", makeCodeTx!=null?makeCodeTx.getHash():bindCodeTx.getHash());
				json.put("antifakeVerifyTx", atx.getHash());
				json.put("productTx", productTxStore.getTransaction().getHash());

			}
			else if(tx.getType() == Definition.TYPE_VIOLATION) {
				ViolationTransaction vtx = (ViolationTransaction) tx;

				ViolationEvidence evidence = vtx.getViolationEvidence();
				int violationType = evidence.getViolationType();
				String reason = "";
				long credit = 0;
				if(violationType == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
					NotBroadcastBlockViolationEvidence nbve = (NotBroadcastBlockViolationEvidence) evidence;
					reason = String.format("共识过程中，开始时间为%s的轮次及后面一轮次超时未出块", DateUtil.convertDate(new Date(nbve.getCurrentPeriodStartTime() * 1000)));
					credit = Configure.CERT_CHANGE_TIME_OUT;
				} else if(violationType == ViolationEvidence.VIOLATION_TYPE_REPEAT_BROADCAST_BLOCK) {
					RepeatBlockViolationEvidence nbve = (RepeatBlockViolationEvidence) evidence;
					reason = String.format("共识过程中,开始时间为%s的轮次重复出块,没收保证金%s", DateUtil.convertDate(new Date(nbve.getBlockHeaders().get(0).getPeriodStartTime() * 1000)), Coin.valueOf(vtx.getOutput(0).getValue()).toText());
					credit = Configure.CERT_CHANGE_SERIOUS_VIOLATION;
				}
				reason = "信用 " + credit + " 原因：" + reason;

				json.put("credit", credit);
				json.put("reason", reason);
			}
		} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER ||
				tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
			//认证账户注册
			CertAccountRegisterTransaction crt = (CertAccountRegisterTransaction) tx;

			JSONObject infos = new JSONObject();

			List<AccountKeyValue> bodyContents = crt.getBody().getContents();
			for (AccountKeyValue keyValuePair : bodyContents) {
				if(AccountKeyValue.LOGO.getCode().equals(keyValuePair.getCode())) {
					//图标
					infos.put(keyValuePair.getCode(), Base64.getEncoder().encodeToString(keyValuePair.getValue()));
				} else {
					infos.put(keyValuePair.getCode(), keyValuePair.getValueToString());
				}
			}
			json.put("infos", infos);

		} else if(tx.getType() == Definition.TYPE_CREATE_PRODUCT) {

			ProductTransaction ptx = (ProductTransaction) tx;

			List<ProductKeyValue> bodyContents = ptx.getProduct().getContents();

			JSONObject product = new JSONObject();
			for (ProductKeyValue keyValuePair : bodyContents) {
				if(ProductKeyValue.CREATE_TIME.getCode().equals(keyValuePair.getCode())) {
					//时间
					product.put(keyValuePair.getCode(), DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0))));
				} else {
					product.put(keyValuePair.getCode(), keyValuePair.getValueToString());
				}
			}
			json.put("product", product);

		} else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {

			GeneralAntifakeTransaction gtx = (GeneralAntifakeTransaction) tx;

			if(gtx.getProduct() != null) {
				JSONObject product = new JSONObject();
				for (ProductKeyValue keyValuePair : gtx.getProduct().getContents()) {
					if(ProductKeyValue.CREATE_TIME.getCode().equals(keyValuePair.getCode())) {
						//时间
						product.put(keyValuePair.getName(), DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0))));
					} else {
						product.put(keyValuePair.getName(), keyValuePair.getValueToString());
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

		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CIRCULATION) {
			//防伪码流转记录
			CirculationTransaction ctx = (CirculationTransaction) tx;
			json.put("antifakeCode", Base58.encode(ctx.getAntifakeCode()));
			json.put("time", ctx.getTime());
			try {
				json.put("tag", new String(ctx.getTag(), "utf-8"));
				json.put("content", new String(ctx.getContent(), "utf-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else if(tx.getType() == Definition.TYPE_ANTIFAKE_TRANSFER) {
			//防伪码流转记录
			AntifakeTransferTransaction attx = (AntifakeTransferTransaction) tx;
			json.put("antifakeCode", Base58.encode(attx.getAntifakeCode()));
			json.put("from", attx.getOperator());
			json.put("to", attx.getReceiveAddress());
			json.put("time", attx.getTime());
		} else if(tx.getType() == Definition.TYPE_CREDIT) {
			CreditTransaction ctx = (CreditTransaction) tx;

			String reason = "初始化";
			if(ctx.getReasonType() == Definition.CREDIT_TYPE_PAY) {
				reason = String.format("%s小时内第一笔转账", Configure.CERT_CHANGE_PAY_INTERVAL/3600000l);
			}
			reason = "信用 +" + ctx.getCredit() + " 原因：" + reason;

			json.put("credit", ctx.getCredit());
			json.put("reason", reason);

		} else if(tx.getType() == Definition.TYPE_ASSETS_ISSUED) {
            AssetsIssuedTransaction aitx = (AssetsIssuedTransaction) tx;
            AccountStore accountStore =chainstateStoreProvider.getAccountInfo(aitx.getReceiver());
            Address address;
            if(accountStore == null) {
                address = new Address(network, aitx.getReceiver());
            }else {
                address = new Address(network,accountStore.getType(), aitx.getReceiver());
            }
            json.put("amount", aitx.getAmount());
            json.put("receiver", address.getBase58());
		} else if(tx.getType() == Definition.TYPE_REG_ALIAS || tx.getType() == Definition.TYPE_UPDATE_ALIAS) {
			RegAliasTransaction rtx = (RegAliasTransaction)tx;
			try {
				json.put("alias", new String(rtx.getAlias(),"utf-8"));
			} catch (UnsupportedEncodingException e) {

			}
		}

		return json;
	}

	/**
	 * 通过地址获取账户
	 * @param address
	 * @param password
	 * @param verifyTr 1账户管理私钥 ，2交易私钥
	 * @return
	 */
	private Account checkAndGetAccount(String address, String password, Integer verifyTr) {
		Account account = null;
		if(address == null) {
			account = accountKit.getDefaultAccount();
		} else {
			account = accountKit.getAccount(address);
		}
		if(account == null) {
			throw new VerificationException("账户不存在");
		}
		if(account.isEncrypted()) {
			if(StringUtil.isEmpty(password)) {
				throw new VerificationException("账户已加密，请解密或者传入密码");
			}
			//解密钱包
			Result pwdResult = accountKit.decryptAccount(password, address ,verifyTr);
			if(!pwdResult.isSuccess()) {
				throw new VerificationException("密码错误");
			}
		}
		return account;
	}

	/**
	 * 解锁钱包
	 * @param password
	 * @param timeSec
	 * @return JSONObject
	 */
	public JSONObject unlockWallet(String passwd,int timeSec) throws JSONException{
		Result rs = accountKit.unlockWallet(passwd,timeSec);
		return new JSONObject().put("success",rs.isSuccess()).put("message",rs.getMessage());
	}

	/**
	 * 立即锁定钱包
	 * @return JSONObject
	 */
	public JSONObject lockWallet() throws JSONException {
		accountKit.lockWallet();
		return new JSONObject().put("success",true).put("message","锁定成功");
	}
}
