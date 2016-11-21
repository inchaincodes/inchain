package org.inchain.rpc;

import java.net.ServerSocket;
import java.net.Socket;

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
public class RPCServer {

	/**
	 * RPC服务启动方法，启动之后监听本地端口 {@link org.inchain.Configure.RPC_SERVER_PORT}提供服务
	 * 
	 */
    public static final int PORT = org.inchain.Configure.RPC_SERVER_PORT;//监听的端口号     

	public void start() {
		  System.out.println("服务器启动...\n");    
		  RPCServer server = new RPCServer();    
	        server.init();  
		
	}
     public void init() {    
	        try {    
	            ServerSocket serverSocket = new ServerSocket(PORT);    
	            while (true) {    
	                // 一旦有堵塞, 则表示服务器与客户端获得了连接    
	                Socket client = serverSocket.accept();    
	                // 处理这次连接    
	                new RPCHanlder(client);    
	            }    
	       } catch (Exception e) {    
	            System.out.println("服务器异常: " + e.getMessage());    
	       }    
	  }    
	
	
}
