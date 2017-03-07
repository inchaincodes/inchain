package org.inchain.validator;

import java.util.Arrays;
import java.util.Date;

import org.inchain.Configure;
import org.inchain.consensus.ConsensusInfos;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.core.Result;
import org.inchain.message.Block;
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
				return new Result(false, "验证新区块时段出错");
			}
			
			log.info("新区块验证信息：{}", currentInfos);
			//如果返回的是不确定，则通过
			if(currentInfos.getResult() == ConsensusInfos.RESULT_UNCERTAIN) {
				return new Result(true, "uncertain");
			}
			
			if(!Arrays.equals(currentInfos.getHash160(), block.getHash160())) {
				log.error("新区块打包人验证错误 : {} {}", block.getHeight(), block.getHash());
				return new Result(false, "新区块打包人验证错误");
			}
			
			//如果时间不同，则应该放入分叉里
			if(currentInfos.getBeginTime() - Configure.BLOCK_GEN__MILLISECOND_TIME > block.getTime() || 
					currentInfos.getEndTime() + Configure.BLOCK_GEN__MILLISECOND_TIME < block.getTime()) {
				log.error("新区块时间戳验证出错 : 高度 {} , hash {} , 块时间 {}, 时段 {} - {}", block.getHeight(), block.getHash(),
						DateUtil.convertDate(new Date(block.getTime())), DateUtil.convertDate(new Date(currentInfos.getBeginTime())), DateUtil.convertDate(new Date(currentInfos.getEndTime())));
				return new Result(false, "新区块时间戳验证出错");
			}
			//TODO
		} catch (Exception e) {
			log.error("验证新区块时段出错", e);
		}
		return new Result(true, "ok");
	}
}
