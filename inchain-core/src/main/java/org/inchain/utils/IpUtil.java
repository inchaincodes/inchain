package org.inchain.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class IpUtil {

	/**
	 * 判断ip地址是否是内网ip
	 * @param addr
	 * @return boolean
	 */
	public static boolean internalIp(byte[] addr) {
		
	    final byte b0 = addr[0];
	    final byte b1 = addr[1];
	    //10.x.x.x/8
	    final byte SECTION_1 = 0x0A;
	    //172.16.x.x/12
	    final byte SECTION_2 = (byte) 0xAC;
	    final byte SECTION_3 = (byte) 0x10;
	    final byte SECTION_4 = (byte) 0x1F;
	    //192.168.x.x/16
	    final byte SECTION_5 = (byte) 0xC0;
	    final byte SECTION_6 = (byte) 0xA8;
	    switch (b0) {
	        case SECTION_1:
	            return true;
	        case SECTION_2:
	            if (b1 >= SECTION_3 && b1 <= SECTION_4) {
	                return true;
	            }
	        case SECTION_5:
	            switch (b1) {
	                case SECTION_6:
	                    return true;
	            }
	        default:
	            return false;
	    }
	}

	/** 
     * 多IP处理，可以得到最终ip 
     * @return Set<String>
     */  
    public static Set<String> getIps() {  
    	//返回本机所有的IP地址 ，包括了内网和外网的IP4地址
    	Set<String> ips = new HashSet<String>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();  
            InetAddress ip = null;
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();  
                Enumeration<InetAddress> address = ni.getInetAddresses();  
                while (address.hasMoreElements()) {  
                    ip = address.nextElement();  
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress()  
                            && ip.getHostAddress().indexOf(":") == -1) {// 外网IP  
                    	ips.add(ip.getHostAddress());  
                        break;  
                    } else if (ip.isSiteLocalAddress()  
                            && !ip.isLoopbackAddress()  
                            && ip.getHostAddress().indexOf(":") == -1) {// 内网IP  
                    	ips.add(ip.getHostAddress());  
                    }  
                }  
            }  
        } catch (SocketException e) {  
            e.printStackTrace();
        }
        return ips;
    }
    
}
