package org.inchain.rpc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.inchain.kits.AccountKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RPCClient<T> {
    public static <T> T getRemoteProxyObj(final Class<?> serviceInterface, final InetSocketAddress addr) {
        // 1.将本地的接口调用转换成JDK的动态代理，在动态代理中实现接口的远程调用
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[]{serviceInterface},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Socket socket = null;
                        ObjectOutputStream output = null;
                        ObjectInputStream input = null;
                        try {
                            // 2.创建Socket客户端，根据指定地址连接远程服务提供者
                            socket = new Socket();
                            socket.connect(addr);
 
                            // 3.将远程服务调用所需的接口类、方法名、参数列表等编码后发送给服务提供者
                            output = new ObjectOutputStream(socket.getOutputStream());
                            output.writeUTF(serviceInterface.getName());
                            output.writeUTF(method.getName());
                            output.writeObject(method.getParameterTypes());
                            output.writeObject(args);
 
                            // 4.同步阻塞等待服务器返回应答，获取应答后返回
                            input = new ObjectInputStream(socket.getInputStream());
                            return input.readObject();
                        } finally {
                            if (socket != null) socket.close();
                            if (output != null) output.close();
                            if (input != null) input.close();
                        }
                    }
                });
    }
}
