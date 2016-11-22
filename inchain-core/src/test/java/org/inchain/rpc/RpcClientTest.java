package org.inchain.rpc;

public class RpcClientTest {
	
    public static final String IP_ADDR = "localhost";//服务器地址   
    public static final int PORT = org.inchain.Configure.RPC_SERVER_PORT;//服务器端口号   
    
	public static void main(String [] ar){
	    System.out.println("单元测试开始。");
	    RPCClient rpcClient = new RPCClient() ;
	    rpcClient.RPCClient(IP_ADDR, PORT);
	}

}
