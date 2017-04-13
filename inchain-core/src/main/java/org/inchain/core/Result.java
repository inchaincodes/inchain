package org.inchain.core;

/**
 * 通用的结果封装
 * @author ln
 *
 */
public class Result {

	protected boolean success;
	protected int errorCode;
	protected String message;
	
	public Result() {
		super();
	}
	public Result(boolean success) {
		this.success = success;
	}
	public Result(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
	public Result(boolean success, int errorCode, String message) {
		this.success = success;
		this.errorCode = errorCode;
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
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Result [success=");
		builder.append(success);
		builder.append(", errorCode=");
		builder.append(errorCode);
		builder.append(", message=");
		builder.append(message);
		builder.append("]");
		return builder.toString();
	}
}
