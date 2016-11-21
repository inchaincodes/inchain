package org.inchain.rpc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;

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
    private Socket socket;    

    public RPCHanlder(Socket client) {    
        socket = client;    
        new Thread(this).start();    
    }    

    public void run() {    
        try {    
        	RPCService service = new RPCServiceImpl();  
            // 读取客户端数据    
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());  
            String clientInputStr = input.readUTF();//这里要注意和客户端输出流的写方法对应,否则会抛 EOFException  
            // 处理客户端数据    
            System.out.println("客户端发过来的内容:" + clientInputStr);    
            try {  
                String methodName = input.readUTF();  
                Class<?>[] parameterTypes = (Class<?>[])input.readObject();  
                Object[] arguments = (Object[])input.readObject();  
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());  
                try {  
                    Method method = service.getClass().getMethod(methodName, parameterTypes);  
                    Object result = method.invoke(service, arguments);  
                    // 向客户端回复信息    
                    output.writeObject(result); 
                    System.out.print("请输入:\t"); 
                } catch (Throwable t) {  
                    output.writeObject(t);  
                } finally {  
                    output.close();  
                }  
            } finally {  
                input.close();  
            }  
        } catch (Exception e) {    
            System.out.println("服务器 run 异常: " + e.getMessage());    
        } finally {    
            if (socket != null) {    
                try {    
                    socket.close();    
                } catch (Exception e) {    
                    socket = null;    
                    System.out.println("服务端 finally 异常:" + e.getMessage());    
                }    
            }    
        }   
    }    


}    
   
