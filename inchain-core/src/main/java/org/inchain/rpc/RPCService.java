package org.inchain.rpc;

import java.io.IOException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.account.AccountBody;
import org.inchain.core.Coin;
import org.inchain.core.Product;
import org.inchain.core.exception.VerificationException;

/**
 * RPCService
/**
 * 
 * 核心客户端RPC服务，RP
 * 命令列表：
 * help    帮助命令，列表出所有命令
 * 
 * --- 区块相关
 * getblockcount 				获取区块的数量
 * getnewestblockheight 		获取最新区块的高度 
 * getnewestblockhash			获取最新区块的hash
 * getblockheader [param] (block hash or height)	通过区块的hash或者高度获取区块的头信息
 * getblock		  [param] (block hash or height)	通过区块的hash或者高度获取区块的完整信息
 * 
 * --- 内存池
 * getmempoolinfo [count] 		获取内存里的count条交易
 * 
 * --- 帐户
 * newaccount [mgpw trpw]		创建帐户，同时必需指定帐户管理密码和交易密码
 * getaccountaddress			获取帐户的地址
 * getaccountpubkeys			获取帐户的公钥
 * dumpprivateseed 				备份私钥种子，同时显示帐户的hash160
 * 
 * getblanace					获取帐户的余额
 * gettransaction				获取帐户的交易记录
 * 
 * ---交易相关
 * TODO ···
 * 
 * @author ln
 *
 */
public interface RPCService {
	
	/**
	 * 获取区块的数量
	 * @return String
	 */
	long getBlockCount();
	
	/**
	 * 获取最新区块的高度 
	 * @return String
	 */
	long getBestBlockHeight();
	
	/**
	 * 获取最新区块的hash
	 * @return String
	 */
	String getBestBlockHash();
	
	/**
	 * 通过高度获取区块hash
	 * @param height
	 * @return String
	 */
	String getBlockHashByHeight(long height);
	
	/**
	 * 通过区块的hash或者高度获取区块的头信息
	 * @param hashOrHeight
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject getBlockHeader(String hashOrHeight) throws JSONException;
	
	/**
	 * 通过区块的hash或者高度获取区块的完整信息
	 * @param hashOrHeight
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject getBlock(String hashOrHeight) throws JSONException;

	/**
	 * 通过hash获取一个分叉块
	 * @param hash
	 * @return JSONObject
	 * @throws JSONException
	 */
	JSONObject getForkBlock(String hash) throws JSONException;
	
	// --- 内存池
	String getmempoolinfo (String count);		//获取内存里的count条交易
	 // --- 帐户
	
	String getaccountpubkeys();			//获取帐户的公钥
	String dumpprivateseed();				//备份私钥种子，同时显示帐户的hash160

	/**
	 * 创建一个普通账户
	 * @return JSONObject
	 * @throws JSONException
	 * @throws IOException 
	 */
	JSONObject newAccount() throws JSONException, IOException;

	/**
	 * 创建一个认证账户
	 * @param mgpw
	 * @param trpw
	 * @param body
	 * @param certpw 
	 * @return JSONObject
	 * @throws JSONException
	 */
	JSONObject newCertAccount(String mgpw, String trpw, AccountBody body, String certpw) throws JSONException;
	
	/**
	 * 获取帐户列表
	 * @return JSONArray
	 * @throws JSONException
	 */
	JSONArray getAccounts() throws JSONException;
	
	/**
	 * 获取账户的余额
	 * @param address 
	 * @return Coin[]
	 */
	Coin[] getAccountBalance(String address);
	

	/**
	 * 获取账户的信用
	 * @param address 
	 * @return long
	 * @throws VerificationException 
	 */
	long getAccountCredit(String address) throws VerificationException;
	
	/**
	 * 获取账户的详细信息
	 * @param address 
	 * @return long
	 * @throws JSONException  VerificationException
	 */
	JSONObject getAccountInfo(String address) throws JSONException, VerificationException;
	
	/**
	 * 获取帐户的交易记录
	 * @param address 
	 * @return JSONArray
	 * @throws JSONException 
	 */
	JSONArray getTransaction(String address) throws JSONException;

	/**
	 * 通过交易hash获取条交易详情
	 * @param txid
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject getTx(String txid) throws JSONException;
	
	/**
	 * 认证账户创建商品
	 * @param address
	 * @param product
	 * @param certpw
	 * @return JSONObject
	 * @throws JSONException
	 */
	JSONObject createProduct(Product product, String certpw, String address) throws JSONException;
	
	/**
	 * 认证账户创建防伪码
	 * @param productTx 商品id
	 * @param count 数量
	 * @param reward 奖励
	 * @param trpw 账户交易密码
	 * @param address	账户地址
	 * @return JSONObject
	 * @throws JSONException
	 */
	JSONObject createAntifake(String productTx, int count, Coin reward, String trpw, String address) throws JSONException;

	/**
	 * 获取共识节点列表
	 * @return JSONArray
	 * @throws JSONException 
	 */
	JSONArray getConsensus() throws JSONException;

	/**
	 * 注册共识
	 * @param password
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject regConsensus(String password) throws JSONException;

	/**
	 * 退出共识
	 * @param password
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject remConsensus(String password) throws JSONException;

	/**
	 * 获取连接节点信息
	 * @return JSONObject
	 */
	JSONObject getPeers() throws JSONException;

	/**
	 * 加密钱包
	 * @param password 
	 * @return JSONObject
	 */
	JSONObject encryptWallet(String password) throws JSONException;

	/**
	 * 修改密码
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject changePassword(String oldPassword, String newPassword) throws JSONException;

	/**
	 * 发送交易
	 * @param toAddress
	 * @param money
	 * @param fee
	 * @param password 
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject sendMoney(String toAddress, String money, String fee, String password) throws JSONException;

	/**
	 * 广播交易
	 * @param txContent
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject broadcast(String txContent) throws JSONException;

	/**
	 * 广播交易 - 交易内容存放在文件里面
	 * @param filepath
	 * @return JSONObject
	 * @throws JSONException 
	 */
	JSONObject broadcastfromfile(String filepath) throws JSONException;

	/**
	 * 通过防伪码查询商家和商品
	 * @param antifakeCode
	 * @return JSONObject
	 * @throws JSONException
	 */
	JSONObject queryAntifake(String antifakeCode) throws JSONException;
	
	/**
	 * 防伪码验证
	 * @param params
	 * @return JSONObject
	 * @throws JSONException
	 */
	JSONObject verifyAntifake(JSONArray params) throws JSONException;
}