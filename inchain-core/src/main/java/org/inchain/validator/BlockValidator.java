package org.inchain.validator;

import java.util.Arrays;
import java.util.Date;

import org.inchain.Configure;
import org.inchain.consensus.ConsensusInfos;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.core.Result;
import org.inchain.core.TimeService;
import org.inchain.message.Block;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 新区块验证器
 * @author ln
 *
 */
@Service
public class BlockValidator {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ConsensusMeeting consensusMeeting;
	@Autowired
	private NetworkParams networkParams;

	public Result doVal(Block block) {
		try {
			//验证新区块打包的人是否合法
			ConsensusInfos currentInfos = consensusMeeting.getCurrentConsensusInfos(block.getPeriodStartPoint(), block.getTimePeriod());

			if(currentInfos == null) {
				log.error("验证新区块时段出错", block);
				return new Result(false, "验证新区块时段出错");
			}
			
			if(log.isDebugEnabled()) {
				log.debug("新区块验证信息：{}", currentInfos);
			}
			//如果返回的是不确定，则通过
			if(currentInfos.getResult() == ConsensusInfos.RESULT_UNCERTAIN) {
				log.warn("不确定的时段，做通过处理", block);
				return new Result(true, "uncertain");
			}
			
			if(!Arrays.equals(currentInfos.getHash160(), block.getHash160())) {
				log.error("新区块打包人验证错误 : {} {}", block.getHeight(), block.getHash());
				return new Result(false, "新区块打包人验证错误");
			}
			
			//如果时间不同，则应该放入分叉里
			if(currentInfos.getBeginTime() > block.getTime() || currentInfos.getEndTime() < block.getTime()) {
				log.error("新区块时间戳验证出错 : 高度 {} , hash {} , 块时间 {}, 时段 {} - {}", block.getHeight(), block.getHash(),
						DateUtil.convertDate(new Date(block.getTime())), DateUtil.convertDate(new Date(currentInfos.getBeginTime())), DateUtil.convertDate(new Date(currentInfos.getEndTime())));
				return new Result(false, "新区块时间戳验证出错");
			}
			
			//允许块的时间和当前时间不超过1个时段的误差，否则验证不通过
			//这个条件的判断，会导致时间不准的节点出错，但是不判断则会给系统带来极大风险，是否放宽条件？  TODO
			long timeDiff = TimeService.currentTimeMillis() - block.getTime();
			if(Math.abs(timeDiff) > Configure.BLOCK_GEN__MILLISECOND_TIME) {
				return new Result(false, "新区块时间误差过大，拒绝接收");
			}
			
			BlockHeader bestBlock = networkParams.getBestBlockHeader().getBlockHeader();
			if(bestBlock.getPeriodStartPoint() == block.getPeriodStartPoint() && bestBlock.getTimePeriod() >= block.getTime()) {
				return new Result(false, "新区块时段比老区块小，禁止接收");
			}
			
		} catch (Exception e) {
			log.error("验证新区块时段出错", e);
		}
		return new Result(true, "ok");
	}
}
