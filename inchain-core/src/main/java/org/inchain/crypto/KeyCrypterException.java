
package org.inchain.crypto;

public class KeyCrypterException extends RuntimeException {
	private static final long serialVersionUID = -1209846722929172652L;

	public KeyCrypterException(String s) {
        super(s);
    }

    public KeyCrypterException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
