package org.inchain.core.exception;

/**
 * 支付时余额不足抛出的异常
 * @author ln
 *
 */
public class MoneyNotEnoughException extends RuntimeException {
	private static final long serialVersionUID = 966570708446699600L;
	
	public MoneyNotEnoughException(String message) {
		super(message);
	}
}
