package org.inchain.consensus;

import java.util.ArrayList;
import java.util.List;

import org.inchain.message.ConsensusMessage;

/**
 * 一轮完整的会议纪要，从就绪，到顺序共识，到结束，都有数据记录
 * @author ln
 *
 */
public class MeetingItemService {

	//本轮的所有就绪消息队列
	private final List<ConsensusMessage> readysList = new ArrayList<ConsensusMessage>();
	//本轮的所有共识消息队列
	private final List<ConsensusMessage> sequenceList = new ArrayList<ConsensusMessage>();
	
	//本轮会议代表的高度，通常和开始高度一致
	private long height;
	//本轮对应的开始高度
	private long startHeight;
	//本轮对应的结束高度
	private long endHeight;
	//本轮的共识状态
	private int status;
	
	
	
}
