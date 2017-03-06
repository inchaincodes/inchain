package org.inchain.validator;

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
			ConsensusInfos currentInfos = consensusMeeting.getCurrentConsensusInfos(block.getHeight());
			
			log.info("新区块验证信息：{}", currentInfos);
			
			if(currentInfos == null || currentInfos.equals(ConsensusInfos.UNCERTAIN)) {
				//不确定的
				log.warn("不确定的新区块 : {} {}", block.getHeight(), block.getHash());
			} else if(currentInfos.getHeight() != block.getHeight() ) {
				log.error("new block error 新区块验证错误 : {} {}", block.getHeight(), block.getHash());
			}
		} catch (Exception e) {
			log.error("========= error ========== {}", e);
		}
		return new Result(true, "ok");
	}
}
