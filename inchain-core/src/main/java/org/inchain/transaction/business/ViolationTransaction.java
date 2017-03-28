package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Definition;
import org.inchain.core.ViolationEvidence;
import org.inchain.core.exception.ProtocolException;
import org.inchain.network.NetworkParams;

/**
 * 违规处理
 * 因为是系统规则，违背系统规则的共识节点甚至普通节点会被乖的（诚信）共识节点处理
 * 对别人的处罚，不能随意，所以任何类型的处罚，都必须提供密码学证据
 * 1、节点超时不出块，这种情况并不代表共识节点是坏节点，有可能是网络原因，或者down机，对该类型的处罚也是最轻的。
 * 	   每一轮共识，都编号排序好的，任何时候都可以追踪验证，所以要判断共识节点没有出块，是比较容易的事，只需要提供对应的该节点的前后时段块即可证明。
 * 2、节点恶意出块，分为多种情况，典型的就是不该出块的时候出块，时段对不上，也包括该节点所属时段出多个不同的块，这些都能留下密码学证据，提供即可对其进行处理。
 * 3、节点尝试双花。
 * 4、节点尝试分叉区块。
 * 
 * 以上2-4种情况，都有密码学证据，即该节点对区块头信息的签名。
 * 
 * @author ln
 *
 */
public class ViolationTransaction extends BaseCommonlyTransaction {
	
	/** 证据  **/
	private ViolationEvidence violationEvidence;

	public ViolationTransaction(NetworkParams params, ViolationEvidence violationEvidence) throws ProtocolException {
		super(params);
		
		type = Definition.TYPE_VIOLATION;
		this.violationEvidence = violationEvidence;
	}
	
	public ViolationTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	public ViolationEvidence getViolationEvidence() {
		return violationEvidence;
	}

	public void setViolationEvidence(ViolationEvidence violationEvidence) {
		this.violationEvidence = violationEvidence;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(violationEvidence.serialize());
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		violationEvidence = ViolationEvidence.fromBytes(payload, cursor);
		length += violationEvidence.getLength();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ViolationTransaction [version=");
		builder.append(version);
		builder.append(", hash=");
		builder.append(hash);
		builder.append(", time=");
		builder.append(time);
		builder.append(", violationEvidence=");
		builder.append(violationEvidence);
		builder.append("]");
		return builder.toString();
	}
}
