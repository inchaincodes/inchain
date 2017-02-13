package org.inchain.wallet.entity;

/**
 * 共识节点信息
 * @author ln
 *
 */
public class ConensusEntity {

	private int status;
	private String address;
	private long cert;
	private long time;

	public ConensusEntity() {
	}
	
	public ConensusEntity(int status, String address, long cert, long time) {
		super();
		this.status = status;
		this.address = address;
		this.cert = cert;
		this.time = time;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public long getCert() {
		return cert;
	}
	public void setCert(long cert) {
		this.cert = cert;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
}
