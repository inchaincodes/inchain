package org.inchain.consensus;

import java.util.List;
import java.util.Set;

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
	void broadcastMessage(ConsensusMessage message);
	
	/**
	 * 接收来自网络的其它节点的发言，并做相应的回复
	 * @param msid
	 * @param message
	 */
	void receiveMeetingMessage(Sha256Hash msid, ConsensusMessage message);
	
	/**
	 * 获取一个已接收的共识消息，如果没有则会返回null
	 * @param msid
	 * @return ConsensusMessage
	 */
	ConsensusMessage getMeetingMessage(Sha256Hash msid);
	
	/**
	 * 消息是否接收
	 * @param msid
	 * @return boolean
	 */
	boolean messageHasReceived(Sha256Hash msid);
	
	/**
	 * 获取区块高度对应的打包人信息，只适用于新区快的验证
	 * @param height 区块高度
	 * @return ConsensusInfos
	 */
	ConsensusInfos getCurrentConsensusInfos(long height);

	/**
	 * 设置运行账户
	 * @param account 
	 */
	void setAccount(Account account);
	
	/**
	 * 获取本轮共识的开始点，也就是开始高度
	 * @return long
	 */
	long getPeriodStartPoint();
	
	/**
	 * 获取本轮和上一轮超时的账号
	 */
	Set<ConsensusAccount> getTimeoutList();
	
	/**
	 * 异步启动
	 */
	void startSyn();
	
	/**
	 * 停止
	 */
	void stop();


}
