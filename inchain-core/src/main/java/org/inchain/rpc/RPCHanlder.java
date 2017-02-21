package org.inchain.rpc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RPCHanlder implements Runnable {
	private final static Logger log = LoggerFactory.getLogger(RPCHanlder.class);

	Socket clent = null;

	public RPCHanlder(Socket client) {
		this.clent = client;
	}

	public void run() {
		ObjectInputStream input = null;
		ObjectOutputStream output = null;
		try {
			// 2.将客户端发送的码流反序列化成对象，反射调用服务实现者，获取执行结果
			input = new ObjectInputStream(clent.getInputStream());
			String serviceName = input.readUTF();
			String methodName = input.readUTF();
			Class<?>[] parameterTypes = (Class<?>[]) input.readObject();
			Object[] arguments = (Object[]) input.readObject();
			Class serviceClass = RPCService.class;
			if (serviceClass == null) {
				throw new ClassNotFoundException(serviceName + " not found");
			}
			Method method = serviceClass.getMethod(methodName, parameterTypes);
			Object result = method.invoke(serviceClass.newInstance(), arguments);

			// 3.将执行结果反序列化，通过socket发送给客户端
			output = new ObjectOutputStream(clent.getOutputStream());
			output.writeObject(result);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (clent != null) {
				try {
					clent.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}    
   
