package org.inchain.consensus;

import org.inchain.account.Account;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.ConsensusMessage;

/**
 * 共识过程，这里理解为共识会议，所有共识人一起开一个永不结束的会议
 * 会议对于所有节点都是透明的，但是只有共识节点才能发言，如果非共识节点违规发言，则有可能面临信用惩罚
 * @author ln
 *
 */
public interface ConsensusMeeting {

	/**
	 * 会议发言，向全网广播当前的想法，仅允许共识节点
	 * @param message
	 */
	void sendMeetingMessage(ConsensusMessage message);
	
	/**
	 * 接收来自网络的其它节点的发言，并做相应的回复
	 * @param msid
	 * @param message
	 */
	void receiveMeetingMessage(Sha256Hash msid, ConsensusMessage message);
	
	/**
	 * 消息是否接收
	 * @param msid
	 * @return boolean
	 */
	boolean messageHasReceive(Sha256Hash msid);
	
	/**
	 * 下一个是否轮到我
	 * @return boolean
	 */
	boolean nextIsMy();
	
	/**
	 * 新块产生，进行广播
	 */
	void newBlockBroadcast();
	/**
	 * 设置运行账户
	 * @param account 
	 */
	void setAccount(Account account);
	
	/**
	 * 异步启动
	 * @param account 
	 */
	void startSyn();
	
	/**
	 * 停止
	 */
	void stop();

}
