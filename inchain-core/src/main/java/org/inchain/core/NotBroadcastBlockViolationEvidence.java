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
	
	/** 时段开始点 **/
	private long periodStartPoint;
	
	public NotBroadcastBlockViolationEvidence(byte[] content) {
		super(content);
	}
	
	public NotBroadcastBlockViolationEvidence(byte[] content, int offset) {
		super(content, offset);
	}
	
	public NotBroadcastBlockViolationEvidence(byte[] audienceHash160, long periodStartPoint) {
		super(VIOLATION_TYPE_NOT_BROADCAST_BLOCK, audienceHash160);
		
		this.periodStartPoint = periodStartPoint;
	}
	
	@Override
	public byte[] serialize() {
		
		ByteArrayTool byteArray = new ByteArrayTool();
		byteArray.append(periodStartPoint);
		evidence = byteArray.toArray();
		
		return super.serialize();
	}
	
	@Override
	public void parse(byte[] content, int offset) {
		super.parse(content, offset);
		periodStartPoint = Utils.readUint32(evidence, 0);
	}

	public long getPeriodStartPoint() {
		return periodStartPoint;
	}

	public void setPeriodStartPoint(long periodStartPoint) {
		this.periodStartPoint = periodStartPoint;
	}
	
}
