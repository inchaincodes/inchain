package org.inchain.core;

import java.util.Arrays;

import org.inchain.account.Address;
import org.inchain.crypto.Sha256Hash;
import org.inchain.utils.ByteArrayTool;

/**
 * 违规证据
 * @author ln
 *
 */
public abstract class ViolationEvidence {
	
	/** 违规类型，1超时不出块 **/
	public final static int VIOLATION_TYPE_NOT_BROADCAST_BLOCK = 1;
	/** 违规类型，2严重违规，重复出块 **/
	public final static int VIOLATION_TYPE_REPEAT_BROADCAST_BLOCK = 2;

	/** 违规类型 **/
	protected int violationType;
	/** 被处理人 **/
	protected byte[] audienceHash160;
	/** 违规证据内容 **/
	protected byte[] evidence;
	
	private int length;
	
	public ViolationEvidence(byte[] content) {
		this(content, 0);
	}
	
	public ViolationEvidence(byte[] content, int offset) {
		parse(content, offset);
	}

	public ViolationEvidence(int violationType, byte[] audienceHash160) {
		this.violationType = violationType;
		this.audienceHash160 = audienceHash160;
	}

	/**
	 * 根据数据流创建证据对象
	 * @param payload
	 * @param offset
	 * @return ViolationEvidence
	 */
	public static ViolationEvidence fromBytes(byte[] payload, int offset) {
		int type = payload[offset];
		if(type == VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
			return new NotBroadcastBlockViolationEvidence(payload, offset);
		}
		return null;
	}
	
	/**
	 * 序列化
	 * @return byte[]
	 */
	public byte[] serialize() {
		ByteArrayTool byteArray = new ByteArrayTool();
		
		byteArray.append(violationType);
		byteArray.append(audienceHash160);
		
		byteArray.append(new VarInt(evidence.length).encode());
		byteArray.append(evidence);
		
		return byteArray.toArray();
    }
	
	/**
	 * 反序列化
	 * @param content
	 * @param offset 
	 */
	public void parse(byte[] content, int offset) {
		int cursor = offset;
		violationType = content[cursor];
		cursor ++;
		audienceHash160 = Arrays.copyOfRange(content, cursor, cursor + Address.LENGTH);
		cursor += Address.LENGTH;
		
		VarInt evidenceLength = new VarInt(content, cursor);
		cursor += evidenceLength.getOriginalSizeInBytes();
		
		evidence = Arrays.copyOfRange(content, cursor, cursor + (int)evidenceLength.value);
		cursor += evidence.length;
		
		length = cursor - offset;
	}
	
	/**
	 * 获取证据的hash，用户快速判断是否已经被处理过了
	 * @return Sha256Hash
	 */
	public Sha256Hash getEvidenceHash() {
		return Sha256Hash.twiceOf(serialize());
	}
	
	public int getViolationType() {
		return violationType;
	}
	public void setViolationType(int violationType) {
		this.violationType = violationType;
	}
	public byte[] getAudienceHash160() {
		return audienceHash160;
	}
	public void setAudienceHash160(byte[] audienceHash160) {
		this.audienceHash160 = audienceHash160;
	}
	public byte[] getEvidence() {
		return evidence;
	}
	public void setEvidence(byte[] evidence) {
		this.evidence = evidence;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
}
