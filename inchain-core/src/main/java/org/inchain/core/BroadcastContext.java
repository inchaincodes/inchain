package org.inchain.core;

import java.util.Hashtable;
import java.util.Map;

import org.inchain.crypto.Sha256Hash;

/**
 * 消息广播上下文，存放广播结果，以获取回应
 * @author ln
 *
 */
public final class BroadcastContext {

	private static final BroadcastContext INSTALL = new BroadcastContext();

	private Map<Sha256Hash, BroadcastResult> context = new Hashtable<Sha256Hash, BroadcastResult>();
	
	private BroadcastContext() {
	}
	
	public synchronized static BroadcastContext get() {
		return INSTALL;
	}
	
	public void add(Sha256Hash hash, BroadcastResult result) {
		context.put(hash, result);
	}
	
	public boolean exist(Sha256Hash hash) {
		return context.containsKey(hash);
	}
	
	public BroadcastResult get(Sha256Hash hash) {
		return context.get(hash);
	}
	
	public BroadcastResult remove(Sha256Hash hash) {
		return context.remove(hash);
	}
	
}
