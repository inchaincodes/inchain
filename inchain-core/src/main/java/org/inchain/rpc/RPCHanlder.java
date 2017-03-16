package org.inchain.rpc;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.inchain.core.Coin;
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
	
	private final static Logger log = LoggerFactory.getLogger(RPCHanlder.class);

	@Autowired
	private RPCService rpcService;
	
	/**
	 * 处理命令
	 * @param commandInfos
	 * @return JSONObject
	 * @throws JSONException 
	 */
	public JSONObject hanlder(JSONObject commandInfos) throws JSONException {
		return hanlder(commandInfos, null);
	}

	public JSONObject hanlder(JSONObject commandInfos, JSONObject inputInfos) throws JSONException {
		String command = commandInfos.getString("command");
		
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
			result.put("blockhash", rpcService.getBlockHashByHeight(Long.parseLong(commandInfos.getJSONArray("params").getString(0))));
			
			return result;
		}
		
		//通过高度或者hash获取区块头信息
		case "getblockheader": {
			result.put("success", true);
			result.put("blockheader", rpcService.getBlockHeader(commandInfos.getJSONArray("params").getString(0)));
			
			return result;
		}
		
		//通过hash或者高度获取一个完整的区块信息
		case "getblock": {
			result.put("success", true);
			result.put("blockheader", rpcService.getBlock(commandInfos.getJSONArray("params").getString(0)));
			
			return result;
		}
		
		//获取余额
		case "getbalance": {
			Coin[] blanaces = rpcService.getAccountBalance();
			
			result.put("success", true);
			result.put("blanace", blanaces[0].add(blanaces[1]).value);
			result.put("canUseBlanace", blanaces[0].value);
			result.put("cannotUseBlanace", blanaces[1].value);
			
			return result;
		}
		
		//获取账户信用
		case "getcredit": {
			long credit = rpcService.getAccountCredit();
			
			result.put("success", true);
			result.put("credit", credit);
			
			return result;
		}
		
		//获取账户信息
		case "getaccountinfo": {
			result = rpcService.getAccountInfo();
			
			result.put("success", true);
			
			return result;
		}
		
		//加密钱包
		case "encryptwallet": {
			result = rpcService.encryptWallet(password);
			return result;
		}
		
		//修改密码
		case "password": {
			result = rpcService.changePassword(password, newPassword);
			return result;
		}
		
		//通过hash获取一笔交易详情
		case "gettx": {
			result = rpcService.getTx(commandInfos.getJSONArray("params").getString(0));
			
			result.put("success", true);
			
			return result;
		}
		
		//获取账户交易
		case "gettransaction": {
			JSONArray txs = rpcService.getTransaction();
			
			result.put("success", true);
			result.put("txs", txs);
			
			return result;
		}
		
		//转账
		case "send": {
			if(commandInfos.getJSONArray("params").length() > 3 && password == null) {
				password = commandInfos.getJSONArray("params").getString(0);
			}
			return rpcService.sendMoney(commandInfos.getJSONArray("params").getString(0), commandInfos.getJSONArray("params").getString(1), commandInfos.getJSONArray("params").getString(2), password);
		}
		
		//获取共识列表
		case "getconsensus": {
			JSONArray consensus = rpcService.getConsensus();
			
			result.put("success", true);
			result.put("consensus", consensus);
			
			return result;
		}
		
		//注册共识
		case "regconsensus": {
			if(commandInfos.getJSONArray("params").length() > 0 && password == null) {
				password = commandInfos.getJSONArray("params").getString(0);
			}
			result = rpcService.regConsensus(password);
			return result;
		}
		
		//退出共识
		case "remconsensus": {
			if(commandInfos.getJSONArray("params").length() > 0 && password == null) {
				password = commandInfos.getJSONArray("params").getString(0);
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
		
		
		
		default:
			result.put("success", false).put("message", "没有找到的命令" + command);
			return result;
		}
	}

	/*
	 * 获取帮助命令
	 */
	private String getHelpCommands() {
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
		sb.append("\n");
		sb.append(" --- 共识相关 --- \n");
		sb.append("  getconsensus                    获取共识节点列表\n");
		sb.append("  regconsensus                    注册共识\n");
		sb.append("  remconsensus                    退出共识\n");
		sb.append("\n");
		sb.append(" --- 节点相关 --- \n");
		sb.append("  getpeers                        获取连接节点信息\n");
		
		sb.append("\n");
		
		return sb.toString();
	}
}    
   
