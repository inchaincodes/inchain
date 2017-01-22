package org.inchain.wallet.entity;

public class TransactionEntity {

	private long status;
	private String type;  
    private String detail;
    private String amount;
    private String time;
    
	public TransactionEntity(long status, String type, String detail, String amount, String time) {
		this.status = status;
		this.type = type;
		this.detail = detail;
		this.amount = amount;
		this.time = time;
	}
	
	public long getStatus() {
		return status;
	}

	public void setStatus(long status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}

}
