package org.inchain.wallet.entity;

import java.util.ArrayList;
import java.util.List;

import org.inchain.account.AccountBody.KeyValuePair;

/**
 * 商家（认证账户）
 * @author ln
 *
 */
public class BusinessEntity {

	private int status;
	private byte[] logo;
	private String name;
	private List<KeyValuePair> details;
	private long time;
	
	public BusinessEntity(int status, byte[] logo, String name, List<KeyValuePair> details, long time) {
		super();
		this.status = status;
		this.logo = logo;
		this.name = name;
		this.details = details;
		this.time = time;
	}
	public BusinessEntity() {
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public byte[] getLogo() {
		return logo;
	}
	public void setLogo(byte[] logo) {
		this.logo = logo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<KeyValuePair> getDetails() {
		return details;
	}
	public void setDetails(List<KeyValuePair> details) {
		this.details = details;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public void addDetail(KeyValuePair keyValuePair) {
		if(details == null) {
			details = new ArrayList<KeyValuePair>();
		}
		details.add(keyValuePair);
	}
}
