package org.inchain.rpc;

/**
 * rpc客户端，通过命令调用远程客户端功能
 * 用法示例 java org.inchain.rpc.RPCClient [-options] <command> [params]
 * [-options] 包括：
 *  -help     客户端帮助，显示命令列表
 * 	-server   远程服务端地址
 *  -user	      远程服务端访问用户名
 *  -password 远程服务端访问密码
 *  -config	  配置文件地址，以上配置都可直接配置到文件里
 *  
 * <command> 包括：
 *  help	显示服务端命令列表
 *  其它参考 {@link org.inchain.rpc.RPCServer}
 *  
 *  [params] 也参考  {@link org.inchain.rpc.RPCServer}
 *  
 * @author ln
 *
 */
public class RPCClient {

	public static void main(String[] args) {
		//TODO
		
	}
}
