package org.inchain.rpc;

import java.io.IOException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.Product;
import org.inchain.core.exception.AddressFormatException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.service.impl.VersionService;
import org.inchain.utils.Base58;
import org.inchain.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RPC命令分发处理
/**
 * 
 * 核心客户端RPC服务，RPC服务随核心启动，端口配置参考 {@link org.inchain.Configure.RPC_SERVER_PORT }
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
@Service
public class RPCHanlder {

	private final static Logger log = LoggerFactory.getLogger(org.inchain.rpc.RPCHanlder.class);

	@Autowired
	private NetworkParams network;
	@Autowired
	private RPCService rpcService;
	@Autowired
	private VersionService versionService;

	/**
	 * 处理命令
	 * @param commandInfos
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject hanlder(JSONObject commandInfos) throws JSONException {
		try {
			return hanlder(commandInfos, null);
		} catch (JSONException e) {
			return new JSONObject().put("success", false).put("message", "缺少参数");
		} catch (AddressFormatException ae) {
			return new JSONObject().put("success", false).put("message", "地址不正确");
		}
	}

	public JSONObject hanlder(JSONObject commandInfos, JSONObject inputInfos) throws JSONException {
		String command = commandInfos.getString("command");

		JSONArray params = commandInfos.getJSONArray("params");

		String password = null;
		String newPassword = null;
		if(inputInfos != null) {
			if(inputInfos.has("password")) {
				password = inputInfos.getString("password");
				if(inputInfos.has("newPassword")) {
					newPassword = inputInfos.getString("newPassword");
				}
			} else if(inputInfos.has("newPassword")) {
				password = inputInfos.getString("newPassword");
			}
		}

		JSONObject result = new JSONObject();
		switch (command) {

			//获取本地区块数量
			case "help":  {
				result.put("success", true);
				result.put("commands", getHelpCommands());

				return result;
			}

			//获取当前版本信息
			case "getversion":  {
				result.put("success", true);
				result.put("version", Definition.LIBRARY_SUBVER);

				String newestVersion = versionService.getNewestVersion();

				result.put("newestversion", newestVersion);

				return result;
			}

			//更新版本
			case "updateversion":  {
				result.put("success", true);

				JSONObject json = versionService.check();

				if(json.getBoolean("success") && json.getBoolean("newVersion")) {

					versionService.update(null);
					result.put("message", "更新成功，请重启客户端");

				} else {
					result.put("message", "无需更新");
				}

				return result;
			}

			//获取本地区块数量
			case "getblockcount":  {
				result.put("success", true);
				result.put("blockcount", rpcService.getBlockCount());

				return result;
			}

			//获取最新区块高度
			case "getbestblockheight": {
				result.put("success", true);
				result.put("bestblockheight", rpcService.getBestBlockHeight());

				return result;
			}

			//获取最新区块hash
			case "getbestblockhash": {
				result.put("success", true);
				result.put("bestblockhash", rpcService.getBestBlockHash());

				return result;
			}

			//通过高度获取区块hash
			case "getblockhash": {
				result.put("success", true);
				result.put("blockhash", rpcService.getBlockHashByHeight(Long.parseLong(params.getString(0))));

				return result;
			}

			//通过高度或者hash获取区块头信息
			case "getblockheader": {
				result.put("success", true);
				result.put("blockheader", rpcService.getBlockHeader(params.getString(0)));

				return result;
			}

			//通过hash或者高度获取一个完整的区块信息
			case "getblock": {
				result.put("success", true);
				result.put("block", rpcService.getBlock(params.getString(0)));

				return result;
			}

			//通过hash获取一个分叉快
			case "getforkblock": {
				result.put("success", true);
				result.put("blockheader", rpcService.getForkBlock(params.getString(0)));

				return result;
			}

			//获取账户列表
			case "getaccounts": {
				JSONArray array = rpcService.getAccounts();

				result.put("success", true);
				result.put("accountList", array);

				return result;
			}

			//新建普通账户
			case "newaccount": {

				try {
					result = rpcService.newAccount();
				} catch (IOException e) {
					result.put("success", false);
					result.put("message", "创建时出错：" + e.getMessage());
				}

				return result;
			}

			//新建认证账户
			case "newcertaccount": {

				try {
					String mggpw = params.getString(0);
					String trpw = params.getString(1);
					String bodyHexStr = params.getString(2);
					AccountBody body = new AccountBody(Base58.decode(bodyHexStr));
					String certpw = params.getString(3);
					String managerAddress = null;
					if(params.length()==5) {
						managerAddress = params.getString(4);
					}

					result = rpcService.newCertAccount(mggpw, trpw, body,certpw,managerAddress);
				} catch (JSONException e) {
					if(e instanceof JSONException) {
						result.put("success", false);
						result.put("message", "缺少参数，命令用法：newcertaccount [mgpw] [trpw] [body hex]");
						return result;
					}
					result.put("success", false);
					result.put("message", "创建时出错：" + e.getMessage());
				}

				return result;
			}

			//修改认证账户信息
			case "updatecertaccount": {

				try {
					String bodyHexStr = params.getString(0);
					AccountBody body = new AccountBody(Base58.decode(bodyHexStr));
					String pw = params.getString(1);
					String address = null;
					if(params.length() > 2) {
						address = params.getString(2);
					}

					result = rpcService.updateCertAccount(body, pw, address);
				} catch (JSONException e) {
					if(e instanceof JSONException) {
						result.put("success", false);
						result.put("message", "缺少参数，命令用法：updatecertaccount [body hex] [pw]");
						return result;
					}
					result.put("success", false);
					result.put("message", "创建时出错：" + e.getMessage());
				}

				return result;
			}

			//吊销认证账户信息
			case "revokecertaccount": {

				try {
					String revokeAddress = params.getString(0);
					String pw = params.getString(1);
					String address = null;
					if(params.length() > 2) {
						address = params.getString(2);
					}

					result = rpcService.revokeCertAccount(revokeAddress, pw, address);
				} catch (JSONException e) {
					if(e instanceof JSONException) {
						result.put("success", false);
						result.put("message", "缺少参数，命令用法：revokecertaccount [revokeaddress] [trpw] [address]");
						return result;
					}
					result.put("success", false);
					result.put("message", "创建时出错：" + e.getMessage());
				}

				return result;
			}

			//修改认证账户密码
			case "certaccounteditpassword": {

				try {
					String oldMgpw = params.getString(0);
					String newMgpw = params.getString(1);
					String newTrpw = params.getString(2);
					String address = null;
					if(params.length() > 3) {
						address = params.getString(3);
					}

					result = rpcService.certAccountEditPassword(oldMgpw, newMgpw, newTrpw, address);
				} catch (JSONException e) {
					if(e instanceof JSONException) {
						result.put("success", false);
						result.put("message", "缺少参数，命令用法：newcertaccount [mgpw] [trpw] [body hex]");
						return result;
					}
					result.put("success", false);
					result.put("message", "创建时出错：" + e.getMessage());
				}

				return result;
			}

			//获取余额
			case "getbalance": {

				String address = null;

				if(params.length() > 0) {
					address = params.getString(0);
				}

				Coin[] blanaces = rpcService.getAccountBalance(address);

				result.put("success", true);
				result.put("blanace", blanaces[0].add(blanaces[1]).value);
				result.put("canUseBlanace", blanaces[0].value);
				result.put("cannotUseBlanace", blanaces[1].value);

				return result;
			}

			//获取账户信用
			case "getcredit": {
				try {
					String address = null;

					if(params.length() > 0) {
						address = params.getString(0);
					}

					long credit = rpcService.getAccountCredit(address);

					result.put("success", true);
					result.put("credit", credit);
				} catch (VerificationException e) {
					result.put("success", false);
					result.put("message", e.getMessage());
				}
				return result;
			}

			//获取账户信息
			case "getaccountinfo": {
				try {
					String address = null;

					if(params.length() > 0) {
						address = params.getString(0);
					}

					result = rpcService.getAccountInfo(address);

					result.put("success", true);
				} catch (VerificationException e) {
					result.put("success", false);
					result.put("message", e.getMessage());
				}
				return result;
			}

			//加密钱包
			case "encryptwallet": {
				if(password == null && params.length() >= 1) {
					password = params.getString(0);
				}
				result = rpcService.encryptWallet(password);
				return result;
			}

			//修改密码
			case "password": {
				if(password == null && newPassword == null && params.length() >= 2) {
					password = params.getString(0);
					newPassword = params.getString(1);
				}
				result = rpcService.changePassword(password, newPassword);
				return result;
			}

			//通过hash获取一笔交易详情
			case "gettx": {
				result = rpcService.getTx(params.getString(0));

				result.put("success", true);

				return result;
			}

			//获取账户交易
			case "gettransaction": {
				String address = null;

				if(params.length() > 0) {
					address = params.getString(0);
				}

				JSONArray txs = rpcService.getTransaction(address);

				result.put("success", true);
				result.put("txs", txs);

				return result;
			}

			//转账
			case "send": {

				if(params.length() < 2) {
					return new JSONObject().put("success", false).put("message", "缺少参数");
				}

				String toAddress = params.getString(0);
				String amount = params.getString(1);
				String address = null;
				String remark = null;
				String passwordOrRemark = null;

				if(params.length() == 3) {
					address = params.getString(2);
					try {
						Address.fromBase58(network, address);
					} catch (Exception e) {
						password = address;
						address = null;
					}
				} else if(params.length() == 4) {
					address = params.getString(2);
					try {
						Address ar = Address.fromBase58(network, address);
						passwordOrRemark = params.getString(3);
					} catch (Exception e) {
						password = address;
						address = null;
						remark = params.getString(3);
					}
				} else if(params.length() == 5) {
					address = params.getString(2);
					password = params.getString(3);
					remark = params.getString(4);
				}

				return rpcService.sendMoney(toAddress, amount, address, password, remark, passwordOrRemark);
			}

			//认证账户创建商品
			case "createproduct": {
				try {
					String productHexStr = params.getString(0);

					String certpw = params.getString(1);

					String address = null;
					if(params.length() > 2) {
						address = params.getString(2);
					}
					Product product = new Product(Base58.decode(productHexStr));

				result = rpcService.createProduct(product, certpw, address);
			} catch (JSONException e) {
				if(e instanceof JSONException) {
					result.put("success", false);
					result.put("message", "缺少参数，命令用法：createproduct [product hex] [trpw] [address]");
					return result;
				}
				result.put("success", false);
				result.put("message", "创建时出错：" + e.getMessage());
			}
			
			return result;
		}

		//认证账户创建防伪码
		case "createantifake": {
			try {
				String productHash = params.getString(0);

				JSONArray sources = null;
				int count = 0;

				String params1 = params.getString(1);

				try {
					System.out.println(params1);

					JSONObject params1Json = new JSONObject(params1);

					count = params1Json.getInt("count");
					if(count <= 0) {
						result.put("success", false);
						result.put("message", "防伪码数量不正确，应大于0");
						return result;
					}
					if(params1Json.has("sources")) {
						sources = params1Json.getJSONArray("sources");
						if(count != sources.length()) {
							result.put("success", false);
							result.put("message", "来源个数和防伪码个数不匹配");
							return result;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					result.put("success", false);
					result.put("message", "参数格式不正确");
					return result;
				}

				String trpw = params.getString(2);

				if(StringUtil.isEmpty(trpw)) {
					result.put("success", false);
					result.put("message", "缺少密码");
					return result;
				}

				//奖励
				Coin reward = Coin.ZERO;
				//账户
				String address = null;
				if(params.length() > 3) {
					try {
						reward = Coin.parseCoin(params.getString(3));
					} catch (Exception e) {
						address = params.getString(3);
					}
					if(params.length() > 4) {
						address = params.getString(4);
					}
				}

				result = rpcService.createAntifake(productHash, count, sources, reward, trpw, address);
			} catch (JSONException e) {
				if(e instanceof JSONException) {
					result.put("success", false);
					result.put("message", "缺少参数，命令用法：createantifake [product hash] [count] [trpassword] ([reward]) [address]");
					return result;
				}
				result.put("success", false);
				result.put("message", "创建时出错：" + e.getMessage());
			}

			return result;
		}

		//通过防伪码查询商家和商品
		case "queryantifake": {
			if(params.length()  < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			return rpcService.queryAntifake(params.getString(0));
		}

		//防伪码验证
		case "verifyantifake": {
			return rpcService.verifyAntifake(params);
		}

		//添加防伪码流转信息
		case "addcirculation": {
			if(params.length()  < 3) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			//防伪码
			String antifakeCode = params.getString(0);
			//标签
			String tag = params.getString(1);
			//内容
			String content = params.getString(2);

			String address = null;
			String privateKeyOrPassword = null;

			if(params.length() == 4) {
				//判断是否是地址
				try {
					Address ar = Address.fromBase58(network, params.getString(3));
					address = ar.getBase58();
				} catch (Exception e) {
					privateKeyOrPassword = params.getString(3);
				}
			} else if(params.length() == 5) {
				address = params.getString(3);
				privateKeyOrPassword = params.getString(4);
			}

			return rpcService.addCirculation(antifakeCode, tag, content, address, privateKeyOrPassword);
		}

		//查询防伪码流转信息
		case "querycirculations": {
			if(params.length()  < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			//防伪码
			String antifakeCode = params.getString(0);
			return rpcService.queryCirculations(antifakeCode);
		}

		//查询防伪码流转次数
		case "querycirculationcount": {
			if(params.length()  < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			//防伪码
			String antifakeCode = params.getString(0);
			String address = null;

			if(params.length() > 1) {
				address = params.getString(1);
			}

			return rpcService.queryCirculationCount(antifakeCode, address);
		}

		//防伪码转让
		case "transferantifake": {
			if(params.length()  < 3) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			//防伪码
			String antifakeCode = params.getString(0);
			//接收者
			String receiver = params.getString(1);
			//备注
			String remark = params.getString(2);

			String address = null;
			String privateKeyOrPassword = null;

			if(params.length() == 4) {
				//判断是否是地址
				try {
					Address ar = Address.fromBase58(network, params.getString(3));
					address = ar.getBase58();
				} catch (Exception e) {
					privateKeyOrPassword = params.getString(3);
				}
			} else if(params.length() == 5) {
				address = params.getString(3);
				privateKeyOrPassword = params.getString(4);
			}

			return rpcService.transferAntifake(antifakeCode, receiver, remark, address, privateKeyOrPassword);
		}

		//查询防伪码转让信息
		case "querytransfers": {
			if(params.length()  < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			//防伪码
			String antifakeCode = params.getString(0);
			return rpcService.queryTransfers(antifakeCode);
		}

		//查询防伪码转让次数
		case "querytransfercount": {
			if(params.length()  < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			//防伪码
			String antifakeCode = params.getString(0);
			return rpcService.queryTransferCount(antifakeCode);
		}

		//资产注册
		case "regassets": {
			if(params.length()  < 5) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			String name = params.getString(0);
			String description = params.getString(1);
			String code = params.getString(2);
			String logo = params.getString(3);
			String remark = params.getString(4);
			String address = null;                  //账户地址
			String pwd = null;                      //账户密码
			//如果参数只有6个的时候 ，考虑用户只传了密码未传账户地址的情况
			if(params.length() == 6) {
				try {
					Address ar = Address.fromBase58(network, params.getString(5));
					address = ar.getBase58();
				} catch (Exception e) {
					pwd = params.getString(5);
				}
			}else if(params.length() == 7) {
				address = params.getString(5);
				pwd = params.getString(6);
			}
			return rpcService.regAssets(name, description, code, logo, remark, address, pwd);
		}
		//获取资产注册列表
		case "getassetslist": {
			return rpcService.getAssetsRegList();
		}

		//资产签发参数格式：   资产代码  接收人地址 资产发行数量  资产注册人地址(选填)  密码(选填)
		case "assetsissue" : {
			if(params.length()  < 3) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}

			String code = params.getString(0);
			String receiver = params.getString(1);
			Long amount =  params.getLong(2);
			String address = null;
			String pwd = null;
			if(params.length() == 4) {
				try {
					Address ar = Address.fromBase58(network, params.getString(3));
					address = ar.getBase58();
				} catch (Exception e) {
					pwd = params.getString(3);
				}

			}else {
				address = params.getString(3);
				pwd = params.getString(4);
			}

			return rpcService.assetsIssue(code, receiver, amount, address, password);
		}
		
		//查询防伪码所属权
		case "queryantifakeowner": {
			if(params.length()  < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			//防伪码
			String antifakeCode = params.getString(0);
			return rpcService.queryAntifakeOwner(antifakeCode);
		}
		
		//认证商家关联子账户
		case "relevancesubaccount": {
			
			if(params.length()  < 3) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			//关联账户
			String relevancer = params.getString(0);
			//别名
			String alias = params.getString(1);
			//描述
			String content = params.getString(2);
			
			String trpw = params.getString(3);
			String address = null;
			
			if(params.length() > 4) {
				address = params.getString(4); 
			}
			
			return rpcService.relevanceSubAccount(relevancer, alias, content, trpw, address);
		}
		
		//认证商家解除子账户关联
		case "removesubaccount": {
			if(params.length()  < 3) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			//关联账户
			String relevancer = params.getString(0);
			//交易id
			String hashId = params.getString(1);
			
			String trpw = params.getString(2);
			String address = null;
			
			if(params.length() > 3) {
				address = params.getString(3); 
			}
			
			return rpcService.removeSubAccount(relevancer, hashId, trpw, address);
		}
		
		//获取认证商家子账户列表
		case "getsubaccounts": {
			if(params.length() == 0) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			String address = params.getString(0);
			return rpcService.getSubAccounts(address);
		}
		
		//获取认证商家子账户个数
		case "getsubaccountcount": {
			if(params.length() == 0) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			String address = params.getString(0);
			return rpcService.getSubAccountCount(address);
		}
		
		//获取认证商家子账户个数
		case "checksssubaccount": {
			if(params.length() < 2) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			String certAddress = params.getString(0);
			String address = params.getString(1);
			return rpcService.checkIsSubAccount(certAddress, address);
		}
		
		//通过别名获取账户
		case "getaccountbyalias": {
			if(params.length() < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			return rpcService.getAccountByAlias(params.getString(0));
		}
		
		//通过账户获取别名
		case "getaliasbyaccount": {
			if(params.length() < 1) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			return rpcService.getAliasByAccount(params.getString(0));
		}
		
		//广播
		case "broadcast": {
			if(params.length() == 0) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			return rpcService.broadcast(params.getString(0));
		}
		
		//广播交易，交易存于文件里
		case "broadcastfromfile": {
			if(params.length() == 0) {
				result.put("success", false);
				result.put("message", "缺少参数");
				return result;
			}
			
			return rpcService.broadcastfromfile(params.getString(0));
		}
		
		//获取共识列表
		case "getconsensus": {
			JSONArray consensus = rpcService.getConsensus();
			
			result.put("success", true);
			result.put("consensus", consensus);
			
			return result;
		}
		
		//获取当前共识节点数量
		case "getconsensuscount": {
			return rpcService.getConsensusCount();
		}
		
		//查看当前共识状态
		case "getconsensusstatus": {
			return rpcService.getConsensusStatus();
		}
		
		//注册共识
		case "regconsensus": {
			String consensusAddress = null;
			if(params.length() == 1) {
				String param1 = params.getString(0);
				try {
					Address.fromBase58(network, param1);
					consensusAddress = param1;
				} catch (Exception e) {
					password = params.getString(0);
				}
			}
			if(params.length() == 2) {
				consensusAddress = params.getString(0);
				password = params.getString(1);
			}
			result = rpcService.regConsensus(password, consensusAddress);
			return result;
		}
		
		//退出共识
		case "remconsensus": {
			if(params.length() > 0 && password == null) {
				password = params.getString(0);
			}
			result = rpcService.remConsensus(password);
			return result;
		}
		
		//获取连接节点信息
		case "getpeers": {
			result = rpcService.getPeers();
			
			result.put("success", true);
			return result;
		}
		
		//获取连接节点数量
		case "getpeercount": {
			result = rpcService.getPeerCount();
			
			result.put("success", true);
			return result;
		}
		
		//通过公钥得到地址
		case "getaddressbypubkey": {
			String pubkey = params.getString(0);
			return rpcService.getAddressByPubKey(pubkey);
		}
		
		//查看账户的私钥
		case "getprivatekey": {
			
			String pw = null;
			String address = null;
			if(params.length() > 0) {
				pw = params.getString(0);
			}
			if(params.length() > 1) {
				address = params.getString(1);
			}
			if(params.length() == 1) {
				//当参数只有一个时，判断是密码还是地址
				try {
					Address.fromBase58(network, address);
					address = params.getString(0);
					pw = null;
				} catch (Exception e) {
				}
			}
			return rpcService.getPrivatekey(address, pw);
		}
		
		//查看账户的私钥
		case "resetdata": {
			return rpcService.resetData();
		}
		
		default:
			result.put("success", false).put("message", "没有找到的命令" + command);
			return result;
		}
	}

	/*
	 * 获取帮助命令
	 */
	public static String getHelpCommands() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("命令列表\n");
		sb.append("\n");
		sb.append(" --- 区块相关 --- \n");
		sb.append("  getblockcount                   获取区块的数量\n");
		sb.append("  getbestblockheight              获取最新区块的高度\n");
		sb.append("  getbestblockhash                获取最新区块的hash\n");
		sb.append("  getblockhash                    通过高度获取区块hash\n");
		sb.append("  getblockheader [param] (block hash or height)   通过区块的hash或者高度获取区块的头信息\n");
		sb.append("  getblock [param] (block hash or height)         通过区块的hash或者高度获取区块的完整信息\n");
		sb.append("\n");
		sb.append(" --- 帐户相关 --- \n");
		sb.append("  getbalance                      获取账户的余额\n");
		sb.append("  getcredit                       获取账户的信用\n");
		sb.append("  getaccountinfo                  获取账户的详细信息\n");
		sb.append("  gettransaction                  获取帐户的交易记录\n");
		sb.append("  encryptwallet                   加密钱包\n");
		sb.append("  password                        修改钱包密码\n");

		sb.append("\n");
		sb.append(" --- 交易相关 --- \n");
		sb.append("  gettx [param] (tx hash)             通过交易hash获取一条交易详情\n");
		sb.append("  send [to address] [money] [fee]     转账\n");
		sb.append("  broadcast [txcontent]               广播交易\n");
		sb.append("\n");
		sb.append(" --- 共识相关 --- \n");
		sb.append("  getconsensus                    获取共识节点列表\n");
		sb.append("  getconsensuscount               获取共识节点数量\n");
		sb.append("  getconsensusstatus              获取当前共识状态\n");
		sb.append("  regconsensus                    注册共识\n");
		sb.append("  remconsensus                    退出共识\n");
		sb.append("\n");
		sb.append(" --- 节点相关 --- \n");
		sb.append("  getpeers                        获取连接节点列表\n");
		sb.append("  getpeercount                    获取连接节点数量\n");
		sb.append("\n");
		sb.append(" --- 业务相关 --- \n");
		sb.append("  createproduct [productinfo] [password]                               认证账户创建商品[仅适用于认证账户]\n");
		sb.append("  makegeneralantifakecode [productinfo|producttxid] [password]         创建普通防伪码[仅适用于认证账户]\n");
		sb.append("  makeantifakecode [productinfo] [password]                            创建链上防伪码[仅适用于认证账户]\n");
		sb.append("  verifygeneralantifakecode [antifakecode] [password]                  验证普通防伪码[仅适用于普通账户]\n");
		sb.append("  verifyantifakecode [antifakecode] [password]                         验证链上防伪码[仅适用于普通账户]\n");
		sb.append("\n");
		sb.append("  queryantifake [antifakecode]                                                                            查询防伪码的信息,包括防伪码所属商家、商品、溯源信息、流转信息、验证信息、转让信息\n");
		sb.append("  addcirculation [antifakecode] [subject] [description] ([address] [privateKeyOrPassword])                添加防伪码流转信息\n");
		sb.append("  querycirculations [antifakecode]                                                                        查询防伪码流转信息\n");
		sb.append("  querycirculationcount [antifakecode] [address]                                                          查询防伪码流转次数\n");
		sb.append("  transferantifake [antifakecode] [receiverAddress] [description]  ([address] [privateKeyOrPassword])     防伪码转让\n");
		sb.append("  querytransfers [antifakecode]                                                                           查询防伪码转让记录\n");
		sb.append("  querytransfercount [antifakecode]                                                                       查询防伪码转让次数\n");
		sb.append("  queryantifakeowner [antifakecode]                                                                       查询防伪码拥有者\n");
		sb.append("\n");
		sb.append("  relevancesubaccount [address] [alias] [description] [trpw] ([certAddress])                              认证商家关联子账户\n");
		sb.append("  removeSubAccount [address] [txId] [trpw] ([certAddress])                                                解除子账户的关联\n");
		sb.append("  getsubaccounts [certAddress]                                                                            获取认证商家子账户列表\n");
		sb.append("  getsubaccountcount [certAddress]                                                                        获取认证商家子账户数量\n");
		sb.append("  checkissubaccount [certAddress] [address]                                                               检查是否是商家的子账户\n");
		sb.append("\n");
		sb.append(" --- 系统相关 --- \n");
		sb.append("  getversion                                                               获取系统版本信息\n");
		sb.append("  updateversion                                                            更新版本\n");

		return sb.toString();
	}
}    
   
