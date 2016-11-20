package org.inchain.core.exception;

public class ScriptException extends VerificationException {

	private static final long serialVersionUID = 9178812267693577317L;

	public ScriptException(String msg) {
        super(msg);
    }

    public ScriptException(String msg, Exception e) {
        super(msg, e);
    }
}
