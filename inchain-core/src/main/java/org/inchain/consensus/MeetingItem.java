package org.inchain.consensus;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.core.TimeService;
import org.inchain.core.VarInt;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.BlockHeader;
import org.inchain.utils.ByteArrayTool;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 一轮完整的会议纪要，从就绪，到顺序共识，到结束，都有数据记录
 * @author ln
 *
 */
public class MeetingItem implements Cloneable {

	//本轮的所有就绪消息队列
	private final List<ConsensusAccount> consensusList;

	private ConsensusMeeting consensusMeeting;
	
	//上一轮的偏移
	private long diffCount;
	//本轮对应的开始时间点，单位秒
	private long periodStartTime;
	//当前轮结束时间，单位秒
	private long periodEndTime;
	//本轮的共识状态
	private int status;
	
	//我是否已经打过包了
	private boolean hasPackage;
	//我的hash160
	private byte[] myHash160;
	//我的打包时间
	private long myPackageTime;
	private long myPackageTimeEnd;
	private int index;
	
	//是否初始化完成
	private boolean init;
	
	public MeetingItem(ConsensusMeeting consensusMeeting, long startTime, List<ConsensusAccount> consensusList) {
		this.consensusMeeting = consensusMeeting;
		this.consensusList = consensusList;
		this.periodStartTime = startTime;
		this.periodEndTime = periodStartTime + consensusList.size() * Configure.BLOCK_GEN_TIME;
		
		//这里打乱共识的顺序，运用每个节点都统一的startHeight属性，来重新排序consensusList
		//排序
		consensusList.sort(new Comparator<ConsensusAccount>() {
			@Override
			public int compare(ConsensusAccount o1, ConsensusAccount o2) {
				if(o1.getSortValue() == null) {
					o1.setSortValue(Sha256Hash.twiceOf((startTime + o1.getHash160Hex()).getBytes()));
				}
				if(o2.getSortValue() == null) {
					o2.setSortValue(Sha256Hash.twiceOf((startTime + o2.getHash160Hex()).getBytes()));
				}
				return o1.getSortValue().compareTo(o2.getSortValue());
			}
		});
	}
	
	public byte[] serialize() {
		
		ByteArrayTool byteArray = new ByteArrayTool();
		
		byteArray.append(status);
		byteArray.append(periodStartTime);
		byteArray.append(periodStartTime);
		byteArray.append(diffCount);
		
		//当前共识列表快照
		if(consensusList != null) {
			byteArray.append(new VarInt(consensusList.size()).encode());
			for (ConsensusAccount consensusAccount : consensusList) {
				byteArray.append(consensusAccount.baseSerialize());
			}
		} else {
			byteArray.append(new VarInt(0).encode());
		}
		
		return byteArray.toArray();
	}


	public void parse(byte[] content) {
		parse(content, 0);
	}
	
	public void parse(byte[] content, int offset) {
		//当前共识状态
		status = content[offset];
		offset++;

		periodStartTime = Utils.readUint32(content, offset);
		offset += 4;
		
		periodStartTime = Utils.readUint32(content, offset);
		offset += 4;
		
		diffCount = Utils.readUint32(content, offset);
		offset += 4;
		
		
		//当共识列表
		VarInt varint = new VarInt(content, offset);
		offset += varint.getOriginalSizeInBytes();
        
		for (int i = 0; i < varint.value; i++) {
			ConsensusAccount consensusAccount = new ConsensusAccount(content, offset);
			offset += consensusAccount.getLength();
			
			boolean exist = false;
			for (ConsensusAccount ca : consensusList) {
				if(Arrays.equals(consensusAccount.getHash160(), ca.getHash160())) {
					exist = true;
					break;
				}
			}
			if(!exist) {
				consensusList.add(consensusAccount);
			}
		}
	}

	/**
	 * 是否能开始共识
	 * @return boolean
	 */
	public boolean canStartConsensus() {
		return consensusList.size() > 1;
	}

	/**
	 * 开始共识，这时根据队列数量决定结束的区块高度
	 */
	public void startConsensus() {
		
		init = true;
		Account myAccount = consensusMeeting.getAccount();
		if(myAccount == null) {
			index = -1;
			return;
		}
		byte[] myHash160 = myAccount.getAddress().getHash160();
		for (int i = 0; i < consensusList.size(); i++) {
			ConsensusAccount consensusAccount = consensusList.get(i);
			if(Arrays.equals(myHash160, consensusAccount.getHash160())) {
				this.myHash160 = myHash160;
				myPackageTime = periodStartTime + (i * Configure.BLOCK_GEN_TIME);
				myPackageTimeEnd = myPackageTime + Configure.BLOCK_GEN_TIME;
				
				index = i;
				break;
			}
		}
	}
	
	/**
	 * 当前是否轮到我打包了
	 * @return boolean
	 */
	public boolean canPackage() {
		if(myHash160 == null || hasPackage) {
			return false;
		}
		//优先验证时间，因为这个永远不会错
		long now = TimeService.currentTimeSeconds();
		
		if(now > myPackageTime && now < myPackageTimeEnd) {
			hasPackage = true;
			return true;
		}
		return false;
	}

	/**
	 * 获取我的共识时段
	 * @return int
	 */
	public int getTimePeriod() {
		Account myAccount = consensusMeeting.getAccount();
		if(myAccount == null) {
			return -1;
		}
		byte[] myHash160 = myAccount.getAddress().getHash160();
		for (int i = 0; i < consensusList.size(); i++) {
			ConsensusAccount consensusAccount = consensusList.get(i);
			if(Arrays.equals(myHash160, consensusAccount.getHash160())) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 获取当前轮的时段数量，也就是由多少人参与共识
	 * @return int
	 */
	public int getPeriodCount() {
		return consensusList.size();
	}
	
	/**
	 * 获取区块高度对应的打包人信息，只适用于新区快的验证
	 * @param timePeriod 区块时段
	 * @return ConsensusInfos
	 */
	public ConsensusInfos getCurrentConsensusInfos(int timePeriod) {
		//根据时间戳确定当前的受托人和高度
		
		if(timePeriod >= consensusList.size() || timePeriod < 0) {
			return null;
		}
		ConsensusAccount consensusAccount = consensusList.get(timePeriod);
		
		byte[] hash160 = consensusAccount.getHash160();
		
		//这里运行广播的时间段，是该节点的时间段中间值，往后延一个区块出块的时间，意味着只有在这个时间段出块，才会被接受
		//系统默认的是中间值出块，最大程度保证诚信节点的稳定运行
		long beginTime = periodStartTime + timePeriod * Configure.BLOCK_GEN_TIME;
		long endTime = beginTime + Configure.BLOCK_GEN_TIME;
		
		ConsensusInfos result = new ConsensusInfos(hash160, beginTime, endTime);
		
		result.setPeriodStartTime(periodStartTime);
		result.setIndex(index);
		
		return result;
	}

	/**
	 * 当前是否已满足本轮结束条件，二哥条件二选一满意即可
	 * 1、高度达到结束高度
	 * 2、本轮时间周期结束
	 * @return boolean
	 */
	public boolean canEnd() {
		if(TimeService.currentTimeSeconds() >= periodEndTime) {
			return true;
		}
		return false;
	}
	
	/**
	 * 获取某个账户的共识时段，返回-1代表不是共识账户
	 * @param hash160
	 * @return int
	 */
	public int getPeriod(byte[] hash160) {
		for (int i = 0; i < consensusList.size(); i++) {
			ConsensusAccount consensusAccount = consensusList.get(i);
			if(Arrays.equals(hash160, consensusAccount.getHash160())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 判断该节点是否在共识列表中
	 * @param consensusAccount
	 * @return boolean
	 */
	public boolean exist(ConsensusAccount consensusAccount) {
		for (ConsensusAccount ca : consensusList) {
			if(Arrays.equals(ca.getHash160(), consensusAccount.getHash160())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 该块是否是该轮共识的最后一个块
	 * @param blockHeader
	 * @return boolean
	 */
	public boolean isLastBlock(BlockHeader blockHeader) {
		if(blockHeader == null) {
			return false;
		}
		if(blockHeader.getPeriodStartTime() == periodStartTime && blockHeader.getPeriodCount() == consensusList.size() 
				&& blockHeader.getTimePeriod() == blockHeader.getPeriodCount() - 1) {
			return true;
		}
		return false;
	}
	
	@Override
	public MeetingItem clone() throws CloneNotSupportedException {
		return (MeetingItem) super.clone();
	}
	
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public List<ConsensusAccount> getConsensusList() {
		return consensusList;
	}
	public long getMyPackageTime() {
		return myPackageTime;
	}
	public void setMyPackageTime(long myPackageTime) {
		this.myPackageTime = myPackageTime;
	}
	public long getMyPackageTimeEnd() {
		return myPackageTimeEnd;
	}
	public void setMyPackageTimeEnd(long myPackageTimeEnd) {
		this.myPackageTimeEnd = myPackageTimeEnd;
	}
	public byte[] getMyHash160() {
		return myHash160;
	}
	public boolean isInit() {
		return init;
	}
	public long getPeriodStartTime() {
		return periodStartTime;
	}
	public void setPeriodStartTime(long periodStartTime) {
		this.periodStartTime = periodStartTime;
	}
	public long getPeriodEndTime() {
		return periodEndTime;
	}
	public void setPeriodEndTime(long periodEndTime) {
		this.periodEndTime = periodEndTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MeetingItem [");
		builder.append("periodStartTime=");
		builder.append(DateUtil.convertDate(new Date(periodStartTime * 1000)));
		builder.append(", periodEndTime=");
		builder.append(DateUtil.convertDate(new Date(periodEndTime * 1000)));
		builder.append(", myPackageTime=");
		builder.append(DateUtil.convertDate(new Date(myPackageTime * 1000)));
		builder.append(", myPackageTimeEnd=");
		builder.append(DateUtil.convertDate(new Date(myPackageTimeEnd * 1000)));
		builder.append(", hasPackage=");
		builder.append(hasPackage);
		builder.append(", myHash160=");
		if(myHash160 == null) {
			builder.append("null");
		} else {
			builder.append(Hex.encode(myHash160));
		}
		builder.append(", index=");
		builder.append(index);
		builder.append("]");
		return builder.toString();
	}
}
