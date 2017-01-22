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
import org.inchain.transaction.RegConsensusTransaction;
import org.inchain.transaction.Transaction;

public class DefaultMessageProcessFactory implements MessageProcessFactory {

	private static final MessageProcessFactory INSTANCE = new DefaultMessageProcessFactory();
	
//	private static final Map<Class<? extends Message>, Class<? extends MessageProcess>> FACTORYS = new HashMap<Class<? extends Message>, Class<? extends MessageProcess>>();
	private static final Map<Class<? extends Message>, String> FACTORYS = new HashMap<Class<? extends Message>, String>();

    static {
//    	FACTORYS.put(PingMessage.class, PingMessageProcess");
//    	FACTORYS.put(PongMessage.class, PongMessageProcess");
//    	FACTORYS.put(VersionMessage.class, VersionMessageProcess");
//    	FACTORYS.put(VerackMessage.class, VerackMessageProcess");
////    	FACTORYS.put(RegisterTransaction.class, RegisterTransaction);
//    	FACTORYS.put(BlockMessage.class, BlockMessageProcess");
//    	FACTORYS.put(GetBlocksMessage.class, GetBlocksMessageProcess");
//    	FACTORYS.put(NewBlockMessage.class, NewBlockMessageProcess");
//    	FACTORYS.put(RegConsensusTransaction.class, RegConsensusMessageProcess");
//    	FACTORYS.put(ConsensusMessage.class, ConsensusMessageProcess");
//    	FACTORYS.put(InventoryMessage.class, InventoryMessageProcess");
//    	FACTORYS.put(GetDatasMessage.class, GetDatasMessageProcess");
    	
    	FACTORYS.put(PingMessage.class, "pingMessageProcess");
    	FACTORYS.put(PongMessage.class, "pongMessageProcess");
    	FACTORYS.put(VersionMessage.class, "versionMessageProcess");
    	FACTORYS.put(VerackMessage.class, "verackMessageProcess");
//    	FACTORYS.put(RegisterTransaction.class, RegisterTransaction);
    	FACTORYS.put(Block.class, "blockMessageProcess");
    	FACTORYS.put(GetBlocksMessage.class, "getBlocksMessageProcess");
    	FACTORYS.put(NewBlockMessage.class, "newBlockMessageProcess");
    	FACTORYS.put(RegConsensusTransaction.class, "regConsensusMessageProcess");
    	FACTORYS.put(ConsensusMessage.class, "consensusMessageProcess");
    	FACTORYS.put(InventoryMessage.class, "inventoryMessageProcess");
    	FACTORYS.put(GetDatasMessage.class, "getDatasMessageProcess");
    	FACTORYS.put(Transaction.class, "transactionMessageProcess");
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
		
//		if(message instanceof PingMessage) {
//			return new PingMessageProcess();
//		} else if(message instanceof PongMessage) {
//			return new PongMessageProcess();
//		} else if(message instanceof VerackMessage) {
//			return new VerackMessageProcess();
//		} else if(message instanceof VersionMessage) {
//			return new VersionMessageProcess();
//		} else if(message instanceof RegConsensusTransaction) {
//			//注册成为共识节点消息
//			return new RegConsensusMessageProcess();
//		}  else if(message instanceof Transaction) {
//			return new TransactionMessageProcess();
//		} else if(message instanceof NewBlockMessage) {
//			return new NewBlockMessageProcess();
//		} else if(message instanceof BlockMessage) {
//			return new BlockMessageProcess();
//		} else if(message instanceof GetBlocksMessage) {
//			return new GetBlocksMessageProcess();
//		} else if(message instanceof ConsensusMessage) {
//			return new ConsensusMessageProcess();
//		} else if(message instanceof GetDatasMessage) {
//			return new GetDatasMessageProcess();
//		} else if(message instanceof InventoryMessage) {
//			return new InventoryMessageProcess();
//		} else {
//			return new UnknownMessageProcess();
//		}
	}
}
