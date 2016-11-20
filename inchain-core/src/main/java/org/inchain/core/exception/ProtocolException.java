package org.inchain.core.exception;

public class ProtocolException extends VerificationException {
	
	private static final long serialVersionUID = 6960479550098711952L;

	public ProtocolException(String msg) {
        super(msg);
    }

    public ProtocolException(Exception e) {
        super(e);
    }

    public ProtocolException(String msg, Exception e) {
        super(msg, e);
    }
}
