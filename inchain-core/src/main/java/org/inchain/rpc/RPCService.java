package org.inchain.rpc;

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
	  //--- 区块相关
	String   getblockcount(); 				//获取区块的数量
	String   getnewestblockheight();  		//获取最新区块的高度 
	String   getnewestblockhash(); 			//获取最新区块的hash
	String   getblockheader  (String  height);	//通过区块的hash或者高度获取区块的头信息
	String   getblock	(String  height);	//通过区块的hash或者高度获取区块的完整信息
	// --- 内存池
	String   getmempoolinfo (String count);		//获取内存里的count条交易
	 // --- 帐户
	String   newaccount (String mgpw, String trpw);	//，同时必需指定帐户管理密码和交易密码
	String   getaccountaddress();		//获取帐户的地址
	String   getaccountpubkeys();			//获取帐户的公钥
	String   dumpprivateseed();				//备份私钥种子，同时显示帐户的hash160
	String   getblanace();					//获取帐户的余额
	String   gettransaction();				//获取帐户的交易记录
	  
	  //---交易相关
	 // TODO ···
   
  
}