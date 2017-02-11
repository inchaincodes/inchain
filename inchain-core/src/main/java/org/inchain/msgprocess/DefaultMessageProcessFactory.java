package org.inchain.msgprocess;

import java.util.HashMap;
import java.util.Map;

import org.inchain.SpringContextUtils;
import org.inchain.message.Block;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.GetBlocksMessage;
import org.inchain.message.GetDatasMessage;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.message.NewBlockMessage;
import org.inchain.message.PingMessage;
import org.inchain.message.PongMessage;
import org.inchain.message.VerackMessage;
import org.inchain.message.VersionMessage;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.CertAccountUpdateTransaction;
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;

public class DefaultMessageProcessFactory implements MessageProcessFactory {

	private static final MessageProcessFactory INSTANCE = new DefaultMessageProcessFactory();
	
	private static final Map<Class<? extends Message>, String> FACTORYS = new HashMap<Class<? extends Message>, String>();

    static {
    	FACTORYS.put(PingMessage.class, "pingMessageProcess");
    	FACTORYS.put(PongMessage.class, "pongMessageProcess");
    	FACTORYS.put(VersionMessage.class, "versionMessageProcess");
    	FACTORYS.put(VerackMessage.class, "verackMessageProcess");
    	FACTORYS.put(Block.class, "blockMessageProcess");
    	FACTORYS.put(GetBlocksMessage.class, "getBlocksMessageProcess");
    	FACTORYS.put(NewBlockMessage.class, "newBlockMessageProcess");
    	FACTORYS.put(RegConsensusTransaction.class, "regConsensusMessageProcess");
    	FACTORYS.put(ConsensusMessage.class, "consensusMessageProcess");
    	FACTORYS.put(InventoryMessage.class, "inventoryMessageProcess");
    	FACTORYS.put(GetDatasMessage.class, "getDatasMessageProcess");
    	FACTORYS.put(Transaction.class, "transactionMessageProcess");
    	FACTORYS.put(CertAccountRegisterTransaction.class, "transactionMessageProcess");
    	FACTORYS.put(CertAccountUpdateTransaction.class, "transactionMessageProcess");
    }

	private DefaultMessageProcessFactory() {
	}
	
	public static MessageProcessFactory getInstance() {
		return INSTANCE;
	}
	
	@Override
	public MessageProcess getFactory(Message message) {
		
		String processId = FACTORYS.get(message.getClass());
		MessageProcess messageProcess = SpringContextUtils.getBean(processId);
		if(messageProcess == null) {
			messageProcess = SpringContextUtils.getBean(UnknownMessageProcess.class);
		}
		return messageProcess;
	}
}
