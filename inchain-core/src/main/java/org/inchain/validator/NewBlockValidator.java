package org.inchain.validator;

import java.util.Arrays;

import org.inchain.Configure;
import org.inchain.consensus.ConsensusInfos;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.core.Result;
import org.inchain.message.Block;
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
public class NewBlockValidator {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ConsensusMeeting consensusMeeting;

	public Result doVal(Block block) {
		try {
			//验证新区块打包的人是否合法
			ConsensusInfos currentInfos = consensusMeeting.getCurrentConsensusInfos(block.getTimePeriod());

			if(currentInfos == null) {
				log.error("验证新区块时段出错", block);
				return new Result(false, "error");
			}
			
			log.info("新区块验证信息：{}", currentInfos);
			//如果返回的是不确定，则通过
			if(currentInfos.getResult() == ConsensusInfos.RESULT_UNCERTAIN) {
				return new Result(true, "uncertain");
			}
			
			if(!Arrays.equals(currentInfos.getHash160(), block.getHash160())) {
				log.error("new block error 新区块验证错误 : {} {}", block.getHeight(), block.getHash());
				return new Result(false, "error");
			}
			
			//如果时间不同，则应该放入分叉里
			if(currentInfos.getBeginTime() - Configure.BLOCK_GEN__MILLISECOND_TIME > block.getTime() || 
					currentInfos.getEndTime() + Configure.BLOCK_GEN__MILLISECOND_TIME < block.getTime()) {
				log.error("new block error 新区块验时间戳验证出错 : {} {}", block.getHeight(), block.getHash());
				return new Result(false, "error");
			}
			//TODO
		} catch (Exception e) {
			log.error("验证新区块时段出错", e);
		}
		return new Result(true, "ok");
	}
}
