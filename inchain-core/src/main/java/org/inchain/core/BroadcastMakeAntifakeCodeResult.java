package org.inchain.core;

/**
 * 广播生成防伪码交易结果
 * @author ln
 *
 */
public class BroadcastMakeAntifakeCodeResult extends BroadcastResult {
	
	private AntifakeCode antifakeCode;

	public BroadcastMakeAntifakeCodeResult() {
		super();
	}
	
	public BroadcastMakeAntifakeCodeResult(boolean success, String message) {
		super(success, message);
	}

	public AntifakeCode getAntifakeCode() {
		return antifakeCode;
	}

	public void setAntifakeCode(AntifakeCode antifakeCode) {
		this.antifakeCode = antifakeCode;
	}

	@Override
	public String toString() {
		return "BroadcastMakeAntifakeCodeResult [success=" + success + ", message=" + message + ", antifakeCode="
				+ antifakeCode + "]";
	}
	
}
