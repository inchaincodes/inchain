package org.inchain.msgprocess;

import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.core.Peer;
import org.inchain.mempool.MempoolContainer;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.message.Message;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 共识注册处理器
 * @author ln
 *
 */
@Service
public class RegConsensusMessageProcess implements MessageProcess {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	//内存池
	private MempoolContainer mempool = MempoolContainerMap.getInstace();
	
	public RegConsensusMessageProcess() {
		
	}
	
	/**
	 * 接收到共识注册，验证合法性，验证是否达到共识条件，如验证通过，则转发该消息，并放进内存池
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive RegConsensusMessage : {}", message);
		}
		//共识申请消息
		RegConsensusTransaction regConsensusMessage = (RegConsensusTransaction) message;

		//申请人
		byte[] hash160 = regConsensusMessage.getHash160();
		
		//得到区块状态提供器
		ChainstateStoreProvider chainstateStore = SpringContextUtils.getBean("chainstateStoreProvider", ChainstateStoreProvider.class);
		
		//获取申请人信息，包括信用和可用余额
		byte[] infos = chainstateStore.getBytes(hash160);
		if(infos == null) {
			//信用不存在
			return null;
		}
		
		//判断是否达到共识条件
		long credit = Utils.readUint32BE(infos, 0);
		if(credit < Configure.CONSENSUS_CREDIT) {
			//信用不够
			return null;
		}
		
		//判断是否已经是共识节点
		if(infos.length > 8 && infos[8] == 1) {
			//已经是共识节点了
			return null;
		}
		
		regConsensusMessage.verfify();
		regConsensusMessage.verfifyScript();
		
		//加入内存池
		mempool.add(regConsensusMessage);
		
		//验证通过，转发该消息
		//TODO
		
		return null;
	}
}
