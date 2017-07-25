package org.inchain.rpc;

import org.codehaus.jettison.json.JSONException;
import org.inchain.core.Coin;
import java.io.IOException;

public class RpcClientTest {

	public static final String IP_ADDR = "localhost";//服务器地址
	public static final int PORT = org.inchain.Configure.RPC_SERVER_PORT;//服务器端口号
	public static String cmd_head = "D:/src/inchain/inchain-client/inchain_cli.bat ";
	static String cmd_makeanti= "createantifakewithoutproduct {count:1} inchain123 0 cnxF3RwBfnzVwksoU7h6DuV9VQEj6VNWvm";
	static String[] paras = cmd_makeanti.split(" ");
	public static void main(String [] ar){
		for(int j =0;j<1;j ++) {
			new Thread() {
				public void run() {
					for (int i = 0; i < 1; i++) {
						try {
							RPCClient.main(paras);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
		}
	}

/*
		Runtime runtime=Runtime.getRuntime();
		try{
			runtime.exec("cmd "+cmd_head + cmd_makeanti);
		}catch(Exception e){
			System.out.println("Error!");
		}
	}
*/
}
