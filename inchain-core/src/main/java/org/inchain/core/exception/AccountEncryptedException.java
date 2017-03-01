package org.inchain.core.exception;

/**
 * 钱包已加密异常，在尝试签名时，遇到加密的钱包没有解密，则会抛出该异常
 * @author ln
 *
 */
public class AccountEncryptedException extends RuntimeException {

	private static final long serialVersionUID = 278074072341884063L;

	public AccountEncryptedException() {
		super("钱包已加密,不能签名,请先解密钱包");
	}
	
	public AccountEncryptedException(String msg) {
		super(msg);
	}
}
