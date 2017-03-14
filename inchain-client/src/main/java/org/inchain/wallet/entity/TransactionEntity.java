package org.inchain.wallet.entity;

import org.inchain.crypto.Sha256Hash;

public class TransactionEntity {

	private Sha256Hash txHash;
	private long status;
	private String type;  
    private DetailValue detail;
    private String amount;
    private String time;
    
	public TransactionEntity(Sha256Hash txHash, long status, String type, DetailValue detail, String amount, String time) {
		this.txHash = txHash;
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
	public DetailValue getDetail() {
		return detail;
	}
	public void setDetail(DetailValue detail) {
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
	public Sha256Hash getTxHash() {
		return txHash;
	}
	@Override
	public String toString() {
		return "TransactionEntity [status=" + status + ", type=" + type + ", detail=" + detail + ", amount=" + amount
				+ ", time=" + time + "]";
	}

}
