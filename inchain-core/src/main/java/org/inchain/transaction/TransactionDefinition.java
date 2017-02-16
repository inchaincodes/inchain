package org.inchain.transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * 交易定义
 * @author ln
 *
 */
public class TransactionDefinition {


	public static final long VERSION = 1;
	
	//lockTime 小于该值的代表区块高度，大于该值的代表时间戳（毫秒）
	public static final long LOCKTIME_THRESHOLD = 50000000000l;
	
	public static void main(String[] args) {
		System.out.println(System.currentTimeMillis());
	}
	
	public static final int TYPE_COINBASE = 1;					//coinbase交易
	public static final int TYPE_PAY = 2;						//普通支付交易
	public static final int TYPE_REG_CONSENSUS = 3;				//注册成为共识节点
	public static final int TYPE_REM_CONSENSUS = 4;				//注销共识节点
	public static final int TYPE_CERT_ACCOUNT_REGISTER = 11;	//认证账户注册
	public static final int TYPE_CERT_ACCOUNT_UPDATE = 12;		//认证账户修改信息
	
	public static final int TYPE_INIT_CREDIT = 99;				//初始化信用，只在创世块里有用
	
	
	public static final int TX_VERIFY_MG = 1;				//脚本认证，账户管理类
	public static final int TX_VERIFY_TR = 2;				//脚本认证，交易类
	
	
	public static final Map<Integer, String> TRANSACTION_RELATION = new HashMap<Integer, String>();
	
	public static final Map<Integer, String> PROCESS_RELATION = new HashMap<Integer, String>();
	
	static {
		TRANSACTION_RELATION.put(TYPE_COINBASE, "org.inchain.transaction.Transaction");
		TRANSACTION_RELATION.put(TYPE_PAY, "org.inchain.transaction.Transaction");
		TRANSACTION_RELATION.put(TYPE_REG_CONSENSUS, "org.inchain.transaction.RegConsensusTransaction");
		TRANSACTION_RELATION.put(TYPE_REM_CONSENSUS, "org.inchain.transaction.RemConsensusTransaction");
		TRANSACTION_RELATION.put(TYPE_CERT_ACCOUNT_REGISTER, "org.inchain.transaction.CertAccountRegisterTransaction");
		TRANSACTION_RELATION.put(TYPE_CERT_ACCOUNT_UPDATE, "org.inchain.transaction.CertAccountUpdateTransaction");
		TRANSACTION_RELATION.put(TYPE_INIT_CREDIT, "org.inchain.transaction.CreditTransaction");
	}
	
}
