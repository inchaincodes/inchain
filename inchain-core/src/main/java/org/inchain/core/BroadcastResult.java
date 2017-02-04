package org.inchain.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.inchain.crypto.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.SettableListenableFuture;

public class BroadcastResult {

    private final static Logger log = LoggerFactory.getLogger(BroadcastResult.class);
    
	private final SettableListenableFuture<BroadcastResult> future =  new SettableListenableFuture<BroadcastResult>();

	//广播结果
	private boolean success;
	//结果信息
	private String result;
	//等待对等体数量
	private int numWaitingFor;
	//消息的hash
	private Sha256Hash hash;
	//广播该消息的对等体
	private List<Peer> broadcastPeers;
	//广播该消息的对等体的响应
	private Set<Peer> broadcastPeerReplys = new HashSet<Peer>();
	//非广播对等体的响应
	private Set<Peer> peerReplys = new HashSet<Peer>();
	
	public BroadcastResult get() throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(10, TimeUnit.SECONDS);
	}
	
	/**
	 * 添加响应对等体
	 * @param peer
	 */
	public void addReply(Peer peer) {
		if(broadcastPeers.contains(peer)) {
			broadcastPeerReplys.add(peer);
		} else {
			peerReplys.add(peer);
		}
		if(broadcastPeerReplys.size() + peerReplys.size() >= numWaitingFor) {
            log.info("broadcast: {} complete, {} , {} peer reply", hash, broadcastPeerReplys.size(), peerReplys.size());
            success = true;
			result = "成功";
			future.set(BroadcastContext.get().remove(hash));
		}
	}
	
	public SettableListenableFuture<BroadcastResult> getFuture() {
		return future;
	}
	
	public void setHash(Sha256Hash hash) {
		this.hash = hash;
	}
	
	public Sha256Hash getHash() {
		return hash;
	}
	
	public void setBroadcastPeers(List<Peer> broadcastPeers) {
		this.broadcastPeers = broadcastPeers;
	}
	
	public boolean needWait() {
		return hash != null;
	}

	public void setNumWaitingFor(int numWaitingFor) {
		this.numWaitingFor = numWaitingFor;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
