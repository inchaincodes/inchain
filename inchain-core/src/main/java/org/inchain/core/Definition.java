package org.inchain.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.inchain.message.AddressMessage;
import org.inchain.message.Block;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.GetAddressMessage;
import org.inchain.message.GetBlocksMessage;
import org.inchain.message.GetDatasMessage;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.message.NewBlockMessage;
import org.inchain.message.PingMessage;
import org.inchain.message.PongMessage;
import org.inchain.message.VerackMessage;
import org.inchain.message.VersionMessage;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.CertAccountUpdateTransaction;
import org.inchain.transaction.business.CreditTransaction;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.transaction.business.RegConsensusTransaction;
import org.inchain.transaction.business.RemConsensusTransaction;

/**
 * 协议定义
 * @author ln
 *
 */
public final class Definition {


	public static final long VERSION = 1;
	
	//lockTime 小于该值的代表区块高度，大于该值的代表时间戳（毫秒）
	public static final long LOCKTIME_THRESHOLD = 50000000000l;
	
	public static final int TYPE_COINBASE = 1;					//coinbase交易
	public static final int TYPE_PAY = 2;						//普通支付交易
	public static final int TYPE_REG_CONSENSUS = 3;				//注册成为共识节点
	public static final int TYPE_REM_CONSENSUS = 4;				//注销共识节点
	public static final int TYPE_CERT_ACCOUNT_REGISTER = 11;	//认证账户注册
	public static final int TYPE_CERT_ACCOUNT_UPDATE = 12;		//认证账户修改信息
	
	//业务交易
	public static final int TYPE_CREATE_PRODUCT = 20;			//创建产品
	public static final int TYPE_GENERAL_ANTIFAKE = 21;			//普通类型的防伪验证交易
	
	public static final int TYPE_INIT_CREDIT = 99;				//初始化信用，只在创世块里有用
	
	
	public static final int TX_VERIFY_MG = 1;				//脚本认证，账户管理类
	public static final int TX_VERIFY_TR = 2;				//脚本认证，交易类
	
	//交易关联
	public static final Map<Integer, Class<? extends Message>> TRANSACTION_RELATION = new HashMap<Integer, Class<? extends Message>>();
	//消息命令关联
	public static final Map<Class<? extends Message>, String> MESSAGE_COMMANDS = new HashMap<Class<? extends Message>, String>();
	//命令消息关联
	public static final Map<String, Class<? extends Message>> COMMANDS_MESSAGE = new HashMap<String, Class<? extends Message>>();
	//消息对应处理器
    public static final Map<Class<? extends Message>, String> PROCESS_FACTORYS = new HashMap<Class<? extends Message>, String>();
    
	static {
    	//===========================-分割线=============================//
		
		PROCESS_FACTORYS.put(PingMessage.class, "pingMessageProcess");
    	PROCESS_FACTORYS.put(PongMessage.class, "pongMessageProcess");
    	PROCESS_FACTORYS.put(VersionMessage.class, "versionMessageProcess");
    	PROCESS_FACTORYS.put(VerackMessage.class, "verackMessageProcess");
    	PROCESS_FACTORYS.put(Block.class, "blockMessageProcess");
    	PROCESS_FACTORYS.put(GetBlocksMessage.class, "getBlocksMessageProcess");
    	PROCESS_FACTORYS.put(NewBlockMessage.class, "newBlockMessageProcess");
    	PROCESS_FACTORYS.put(ConsensusMessage.class, "consensusMessageProcess");
    	PROCESS_FACTORYS.put(InventoryMessage.class, "inventoryMessageProcess");
    	PROCESS_FACTORYS.put(GetDatasMessage.class, "getDatasMessageProcess");

    	PROCESS_FACTORYS.put(AddressMessage.class, "addressMessageProcess");
    	PROCESS_FACTORYS.put(GetAddressMessage.class, "addressMessageProcess");
    	
    	PROCESS_FACTORYS.put(Transaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(CertAccountRegisterTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(CertAccountUpdateTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RegConsensusTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RemConsensusTransaction.class, "transactionMessageProcess");
    	
    	//业务消息处理器 
    	PROCESS_FACTORYS.put(ProductTransaction.class, "productTransactionProcess");
    	
    	//===========================-分割线=============================//
    	
    	MESSAGE_COMMANDS.put(PingMessage.class, "ping");
    	MESSAGE_COMMANDS.put(PongMessage.class, "pong");
    	MESSAGE_COMMANDS.put(VersionMessage.class, "version");
    	MESSAGE_COMMANDS.put(VerackMessage.class, "verack");
    	MESSAGE_COMMANDS.put(AddressMessage.class, "addr");
    	MESSAGE_COMMANDS.put(GetAddressMessage.class, "getaddr");
    	MESSAGE_COMMANDS.put(Block.class, "block");
    	MESSAGE_COMMANDS.put(GetBlocksMessage.class, "getblock");
    	MESSAGE_COMMANDS.put(NewBlockMessage.class, "newblock");
    	MESSAGE_COMMANDS.put(ConsensusMessage.class, "consensus");
    	MESSAGE_COMMANDS.put(InventoryMessage.class, "inv");
    	MESSAGE_COMMANDS.put(GetDatasMessage.class, "getdatas");
    	
    	MESSAGE_COMMANDS.put(Transaction.class, "tx");
    	MESSAGE_COMMANDS.put(CertAccountRegisterTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(CertAccountUpdateTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(RegConsensusTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(RemConsensusTransaction.class, "tx");

    	MESSAGE_COMMANDS.put(ProductTransaction.class, "tx");
    	
    	//===========================-分割线=============================//
    	
    	TRANSACTION_RELATION.put(TYPE_COINBASE, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_PAY, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_REG_CONSENSUS, RegConsensusTransaction.class);
		TRANSACTION_RELATION.put(TYPE_REM_CONSENSUS, RemConsensusTransaction.class);
		TRANSACTION_RELATION.put(TYPE_CERT_ACCOUNT_REGISTER, CertAccountRegisterTransaction.class);
		TRANSACTION_RELATION.put(TYPE_CERT_ACCOUNT_UPDATE, CertAccountUpdateTransaction.class);
		TRANSACTION_RELATION.put(TYPE_INIT_CREDIT, CreditTransaction.class);
		
		//业务交易
		TRANSACTION_RELATION.put(TYPE_CREATE_PRODUCT, ProductTransaction.class);
		TRANSACTION_RELATION.put(TYPE_GENERAL_ANTIFAKE, GeneralAntifakeTransaction.class);
		
    	//===========================-分割线=============================//
    	
    	for (Entry<Class<? extends Message>, String> entry : MESSAGE_COMMANDS.entrySet()) {
			COMMANDS_MESSAGE.put(entry.getValue(), entry.getKey());
		}
    }
	
}
