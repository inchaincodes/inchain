package org.inchain.core;

import java.util.ArrayList;
import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.utils.ByteArrayTool;
import org.inchain.utils.Utils;

/**
 * 重复出块证据
 * 给出两个以上不同的块头信息，由同一个人打包签名，且在同一时段
 * @author ln
 *
 */
public class RepeatBlockViolationEvidence extends ViolationEvidence {
	
	/** 重复块的头信息 **/
	private List<BlockHeader> blockHeaders;
	
	public RepeatBlockViolationEvidence(byte[] content) {
		this(content, 0);
	}
	
	public RepeatBlockViolationEvidence(byte[] content, int offset) {
		super(content, offset);
	}
	
	public RepeatBlockViolationEvidence(byte[] audienceHash160, List<BlockHeader> blockHeaders) {
		super(VIOLATION_TYPE_REPEAT_BROADCAST_BLOCK, audienceHash160);
		
		this.blockHeaders = blockHeaders;
	}
	
	@Override
	public byte[] serialize() {
		
		ByteArrayTool byteArray = new ByteArrayTool();
		
		Utils.checkNotNull(blockHeaders);
		
		byteArray.append(blockHeaders.size());
		
		for (BlockHeader blockHeader : blockHeaders) {
			byteArray.append(blockHeader.baseSerialize());
		}
		evidence = byteArray.toArray();
		
		return super.serialize();
	}
	
	@Override
	public void parse(byte[] content, int offset) {
		super.parse(content, offset);
		
		blockHeaders = new ArrayList<BlockHeader>();
		
		int cursor = 0;
		int countSize = evidence[cursor];
		cursor++;
        
        NetworkParams network = SpringContextUtils.getNetwork();
        for (int i = 0; i < countSize; i++) {
        	BlockHeader blockHeader = new BlockHeader(network, evidence, cursor);
        	blockHeaders.add(blockHeader);
        	cursor += blockHeader.getLength();
		}
	}

	public List<BlockHeader> getBlockHeaders() {
		return blockHeaders;
	}

	public void setBlockHeaders(List<BlockHeader> blockHeaders) {
		this.blockHeaders = blockHeaders;
	}
}
