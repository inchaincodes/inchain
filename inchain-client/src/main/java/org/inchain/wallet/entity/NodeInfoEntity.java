package org.inchain.wallet.entity;

public class NodeInfoEntity {

	private String ip;
	private String version;
	private String sort;
	private String time;
	private String offsetTime;
	private String duration;
	private String types;
	
	public NodeInfoEntity(String ip,String version,String sort,String time,String offsetTime,String duration,String types) {
		this.ip = ip;
		this.version = version;
		this.sort = sort;
		this.time = time;
		this.offsetTime = offsetTime;
		this.duration = duration;
		this.types = types;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getSort() {
		return sort;
	}
	public void setSort(String sort) {
		this.sort = sort;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getOffsetTime() {
		return offsetTime;
	}
	public void setOffsetTime(String offsetTime) {
		this.offsetTime = offsetTime;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public String getTypes() {
		return types;
	}
	public void setTypes(String types) {
		this.types = types;
	}
	
}
