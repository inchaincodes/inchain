package org.inchain.validator;

import org.inchain.core.Coin;

/**
 * 交易验证结果
 * @author ln
 *
 */
public class TransactionValidatorResult {

	/** 错误代码 -- 已使用 **/
	public final static int ERROR_CODE_USED = 1;
	
	private boolean success;			//验证结果
	private String message;				//验证结果信息
	private int errorCode;				//错误代码
	private Coin fee;					//交易手续费
	
	public void setResult(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	
	public void setResult(boolean success, int errorCode, String message) {
		this.success = success;
		this.message = message;
		this.errorCode = errorCode;
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
	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	public String toString() {
		return "TransactionValidatorResult [success=" + success + ", message=" + message + ", errorCode=" + errorCode
				+ ", fee=" + fee + "]";
	}
	
}
