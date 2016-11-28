package org.inchain.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RPCTest {
	 
    public static void main(String[] args) throws IOException {
        new Thread(new Runnable() {
            public void run() {
                try {
                    RPCServer serviceServer = new RPCServer( );
//                    serviceServer.register(RPCService.class, RPCService.class);
                    serviceServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        RPCService service = RPCClient.getRemoteProxyObj(RPCService.class, new InetSocketAddress("localhost", 8036));
//        System.out.println(service.sayHi("test"));
    }
}