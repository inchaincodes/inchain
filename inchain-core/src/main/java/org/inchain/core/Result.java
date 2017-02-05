package org.inchain.core;

/**
 * 通用的结果封装
 * @author ln
 *
 */
public class Result {

	protected boolean success;
	protected String message;
	
	public Result() {
		super();
	}
	public Result(boolean success, String message) {
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
	@Override
	public String toString() {
		return "Result [success=" + success + ", message=" + message + "]";
	}
}
