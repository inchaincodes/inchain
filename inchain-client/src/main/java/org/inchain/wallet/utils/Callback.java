package org.inchain.wallet.utils;

public abstract class Callback {
	public abstract void ok(Object param);
	public void cancel(Object param) {}
}
