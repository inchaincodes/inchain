package org.inchain.validator;

import org.inchain.core.Coin;

/**
 * 交易验证结果
 * @author ln
 *
 */
public class TransactionValidatorResult {

	private boolean success;			//验证结果
	private String message;				//验证结果信息
	private Coin fee;					//交易手续费
	
	public void setResult(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Coin getFee() {
		return fee;
	}
	public void setFee(Coin fee) {
		this.fee = fee;
	}
	@Override
	public String toString() {
		return "TransactionValidatorResult [success=" + success + ", message=" + message + ", fee=" + fee + "]";
	}
	
}
