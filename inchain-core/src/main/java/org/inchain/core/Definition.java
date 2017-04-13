package org.inchain.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.inchain.message.AddressMessage;
import org.inchain.message.Block;
import org.inchain.message.ConsensusMessage;
import org.inchain.message.DataNotFoundMessage;
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
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.AntifakeTransferTransaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.CertAccountUpdateTransaction;
import org.inchain.transaction.business.CirculationTransaction;
import org.inchain.transaction.business.CreditTransaction;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.transaction.business.RegAliasTransaction;
import org.inchain.transaction.business.RegConsensusTransaction;
import org.inchain.transaction.business.RelevanceSubAccountTransaction;
import org.inchain.transaction.business.RemConsensusTransaction;
import org.inchain.transaction.business.RemoveSubAccountTransaction;
import org.inchain.transaction.business.UpdateAliasTransaction;
import org.inchain.transaction.business.ViolationTransaction;

/**
 * 协议定义
 * @author ln
 *
 */
public final class Definition {


	/**
     * Inchain 核心程序版本
     */
    public static final String INCHAIN_VERSION = "0.0.5";

    /**
     * 版本完整信息
     */
    public static final String LIBRARY_SUBVER = "inchain core preview version v" + INCHAIN_VERSION + "";
    
	public static final long VERSION = 1;
	
	/** lockTime 小于该值的代表区块高度，大于该值的代表时间戳（毫秒） **/
	public static final long LOCKTIME_THRESHOLD = 50000000000l;
	
	public static final int TYPE_COINBASE = 1;					//coinbase交易
	public static final int TYPE_PAY = 2;						//普通支付交易
	public static final int TYPE_REG_CONSENSUS = 3;				//注册成为共识节点
	public static final int TYPE_REM_CONSENSUS = 4;				//注销共识节点
	public static final int TYPE_VIOLATION = 5;					// 违规事件处理
	/** 信用累积 **/
	public static final int TYPE_CREDIT = 6;
	/** 注册别名 **/
	public static final int TYPE_REG_ALIAS = 7;
	/** 修改别名 **/
	public static final int TYPE_UPDATE_ALIAS = 8;
	
	/** 认证账户注册 **/
	public static final int TYPE_CERT_ACCOUNT_REGISTER = 11;
	/** 认证账户修改信息 **/
	public static final int TYPE_CERT_ACCOUNT_UPDATE = 12;
	/** 商家关联子账户 **/
	public static final int TYPE_RELEVANCE_SUBACCOUNT = 13;
	/** 商家解除子账户的关联 **/
	public static final int TYPE_REMOVE_SUBACCOUNT = 14;
	
	//业务交易
	/** 创建产品 **/
	public static final int TYPE_CREATE_PRODUCT = 20;
	/** 普通类型的防伪验证交易 **/
	public static final int TYPE_GENERAL_ANTIFAKE = 21;
	/** 防伪码生产交易 **/
	public static final int TYPE_ANTIFAKE_CODE_MAKE = 22;
	/** 防伪码流转记录，防伪码状态变动 **/
	public static final int TYPE_ANTIFAKE_STATE_CHANGE = 23;
	/** 防伪码验证 **/
	public static final int TYPE_ANTIFAKE_CODE_VERIFY = 24;
	/** 防伪码流转 **/
	public static final int TYPE_ANTIFAKE_CIRCULATION = 25;
	/** 防伪码转让 **/
	public static final int TYPE_ANTIFAKE_TRANSFER = 26;
	
	
	public static final int TX_VERIFY_MG = 1;				//脚本认证，账户管理类
	public static final int TX_VERIFY_TR = 2;				//脚本认证，交易类
	
	/**
	 * 违规类型， 重复打包
	 */
	public final static int PENALIZE_REPEAT_BLOCK = 1;
	/**
	 * 违规类型， 垃圾块攻击
	 */
	public final static int PENALIZE_RUBBISH_BLOCK = 2;
	/**
	 * 违规类型， 打包不合法交易
	 */
	public final static int PENALIZE_ILLEGAL_TX = 3;
	

	/** 转账获得信用值 **/
	public static final int CREDIT_TYPE_PAY = 1;
	/** 持续在线获得信用值 **/
	public static final int CREDIT_TYPE_ONLINE = 2;
	
	/**
	 * 判断传入的交易是否跟代币有关
	 * @param type
	 * @return boolean
	 */
	public static boolean isPaymentTransaction(int type) {
		return type == TYPE_COINBASE || type == TYPE_PAY || type == TYPE_ANTIFAKE_CODE_MAKE
				|| type == TYPE_ANTIFAKE_CODE_VERIFY || type == TYPE_REG_CONSENSUS
				|| type == TYPE_REM_CONSENSUS || type == TYPE_VIOLATION; 
	}
	
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
    	PROCESS_FACTORYS.put(DataNotFoundMessage.class, "dataNotFoundMessageProcess");

    	PROCESS_FACTORYS.put(AddressMessage.class, "addressMessageProcess");
    	PROCESS_FACTORYS.put(GetAddressMessage.class, "addressMessageProcess");
    	
    	PROCESS_FACTORYS.put(Transaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RegAliasTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(UpdateAliasTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(CertAccountRegisterTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(CertAccountUpdateTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RelevanceSubAccountTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RemoveSubAccountTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RegConsensusTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(RemConsensusTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(ViolationTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(CreditTransaction.class, "transactionMessageProcess");
    	
    	//业务消息处理器 
    	PROCESS_FACTORYS.put(ProductTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(GeneralAntifakeTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(AntifakeCodeMakeTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(AntifakeCodeVerifyTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(CirculationTransaction.class, "transactionMessageProcess");
    	PROCESS_FACTORYS.put(AntifakeTransferTransaction.class, "transactionMessageProcess");
    	
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
    	MESSAGE_COMMANDS.put(DataNotFoundMessage.class, "notfound");
    	
    	MESSAGE_COMMANDS.put(Transaction.class, "tx");
    	MESSAGE_COMMANDS.put(RegAliasTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(UpdateAliasTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(CertAccountRegisterTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(CertAccountUpdateTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(RegConsensusTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(RemConsensusTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(RelevanceSubAccountTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(RemoveSubAccountTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(ViolationTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(CreditTransaction.class, "tx");

    	MESSAGE_COMMANDS.put(ProductTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(GeneralAntifakeTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(AntifakeCodeMakeTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(AntifakeCodeVerifyTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(CirculationTransaction.class, "tx");
    	MESSAGE_COMMANDS.put(AntifakeTransferTransaction.class, "tx");
    	
    	//===========================-分割线=============================//
    	
    	TRANSACTION_RELATION.put(TYPE_COINBASE, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_PAY, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_REG_ALIAS, RegAliasTransaction.class);
		TRANSACTION_RELATION.put(TYPE_UPDATE_ALIAS, UpdateAliasTransaction.class);
		TRANSACTION_RELATION.put(TYPE_REG_CONSENSUS, RegConsensusTransaction.class);
		TRANSACTION_RELATION.put(TYPE_REM_CONSENSUS, RemConsensusTransaction.class);
		TRANSACTION_RELATION.put(TYPE_CERT_ACCOUNT_REGISTER, CertAccountRegisterTransaction.class);
		TRANSACTION_RELATION.put(TYPE_CERT_ACCOUNT_UPDATE, CertAccountUpdateTransaction.class);
		TRANSACTION_RELATION.put(TYPE_RELEVANCE_SUBACCOUNT, RelevanceSubAccountTransaction.class);
		TRANSACTION_RELATION.put(TYPE_REMOVE_SUBACCOUNT, RemoveSubAccountTransaction.class);
		TRANSACTION_RELATION.put(TYPE_VIOLATION, ViolationTransaction.class);
		TRANSACTION_RELATION.put(TYPE_CREDIT, CreditTransaction.class);
		
		//业务交易
		TRANSACTION_RELATION.put(TYPE_CREATE_PRODUCT, ProductTransaction.class);
		TRANSACTION_RELATION.put(TYPE_GENERAL_ANTIFAKE, GeneralAntifakeTransaction.class);
		TRANSACTION_RELATION.put(TYPE_ANTIFAKE_CODE_MAKE, AntifakeCodeMakeTransaction.class);
		TRANSACTION_RELATION.put(TYPE_ANTIFAKE_CODE_VERIFY, AntifakeCodeVerifyTransaction.class);
		TRANSACTION_RELATION.put(TYPE_ANTIFAKE_CIRCULATION, CirculationTransaction.class);
		TRANSACTION_RELATION.put(TYPE_ANTIFAKE_TRANSFER, AntifakeTransferTransaction.class);
		
    	//===========================-分割线=============================//
    	
    	for (Entry<Class<? extends Message>, String> entry : MESSAGE_COMMANDS.entrySet()) {
			COMMANDS_MESSAGE.put(entry.getValue(), entry.getKey());
		}
    }
}
