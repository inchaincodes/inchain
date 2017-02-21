package org.inchain.network;

import java.io.Serializable;
import java.net.InetAddress;

public class PeerAddressStore implements Serializable {
	private static final long serialVersionUID = 8821950401288230577L;

	private InetAddress addr;
    private String hostname;
    private int port;
    private long services;
    private long time;
    private int status;	//状态，0待验证，1验证通过，2验证失败
    private int verifySuccessCount;	//验证通过的次数
    private int verifyFailCount;	//验证失败的次数，一般验证失败的次数过多，则可能从缓存之中移除
    private long lastVerifyTime;	//最后一次验证时间，一般加入到列表中的节点，如果没验证通过，则每隔10分钟验证一次，验证通过的节点，也每3小时验证一次
    
	public InetAddress getAddr() {
		return addr;
	}
	public void setAddr(InetAddress addr) {
		this.addr = addr;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public long getServices() {
		return services;
	}
	public void setServices(long services) {
		this.services = services;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getVerifySuccessCount() {
		return verifySuccessCount;
	}
	public void setVerifySuccessCount(int verifySuccessCount) {
		this.verifySuccessCount = verifySuccessCount;
	}
	public int getVerifyFailCount() {
		return verifyFailCount;
	}
	public void setVerifyFailCount(int verifyFailCount) {
		this.verifyFailCount = verifyFailCount;
	}
	public long getLastVerifyTime() {
		return lastVerifyTime;
	}
	public void setLastVerifyTime(long lastVerifyTime) {
		this.lastVerifyTime = lastVerifyTime;
	}
}
