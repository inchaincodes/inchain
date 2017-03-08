package org.inchain.core;

import org.inchain.utils.ByteArrayTool;
import org.inchain.utils.Utils;

/**
 * 没有打包块的证据
 * 只需要给出前后2个块的高度即可，其它所有节点均可验证
 * 注意这两个块需要相连
 * @author ln
 *
 */
public class NotBroadcastBlockViolationEvidence extends ViolationEvidence {
	
	/** 上一块 **/
	private long preBlockHeight;
	/** 下一块 **/
	private long nextBlockHeight;
	
	public NotBroadcastBlockViolationEvidence(byte[] content) {
		super(content);
	}
	
	public NotBroadcastBlockViolationEvidence(byte[] content, int offset) {
		super(content, offset);
	}
	
	public NotBroadcastBlockViolationEvidence(byte[] audienceHash160, long preBlockHeight, long nextBlockHeight) {
		super(VIOLATION_TYPE_NOT_BROADCAST_BLOCK, audienceHash160);
		
		this.preBlockHeight = preBlockHeight;
		this.nextBlockHeight = nextBlockHeight;
	}
	
	@Override
	public byte[] serialize() {
		
		ByteArrayTool byteArray = new ByteArrayTool();
		byteArray.append(preBlockHeight);
		byteArray.append(nextBlockHeight);
		evidence = byteArray.toArray();
		
		return super.serialize();
	}
	
	@Override
	public void parse(byte[] content, int offset) {
		super.parse(content, offset);
		preBlockHeight = Utils.readUint32(evidence, 0);
		nextBlockHeight = Utils.readUint32(evidence, 4);
	}

	public long getPreBlockHeight() {
		return preBlockHeight;
	}

	public void setPreBlockHeight(long preBlockHeight) {
		this.preBlockHeight = preBlockHeight;
	}

	public long getNextBlockHeight() {
		return nextBlockHeight;
	}

	public void setNextBlockHeight(long nextBlockHeight) {
		this.nextBlockHeight = nextBlockHeight;
	}
	
}
