package org.inchain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.inchain.core.Coin;
import org.inchain.crypto.Sha256Hash;
import org.inchain.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置
 * @author ln
 *
 */
public final class Configure {

	private static Logger log = LoggerFactory.getLogger(Configure.class);

	private final static Properties property = new Properties();
	
	public static String SERVER_HOME = System.getProperty("serverHome");
	
	static {
		try {
			if(StringUtil.isEmpty(SERVER_HOME)) {
				SERVER_HOME = System.getProperty("user.dir");
				System.setProperty("serverHome", SERVER_HOME);
			}
			oloadProperties();
		} catch (IOException e) {
			log.error("加载配置文件出错", e);
		}
	}

    private static void oloadProperties() throws IOException {
    	InputStream in = Configure.class.getResourceAsStream("/config.properties");
		if(in != null) {
			property.load(in);
			in.close();
		}
    }
    
	/**
	 * 运行模式，1主网， 2 测试网络，3 单元测试
	 */
	public final static int RUN_MODE = getProperty("run.mode", 2);
	
	/**
	 * 网络中默认的p2p端口,一般是指没有自已修改过端口的服务节点，使用的是该类端口
	 */
	public final static int DEFAULT_PORT = 11888;

	/**
	 * p2p 端口
	 */
	public final static int PORT = DEFAULT_PORT;//getProperty("port", DEFAULT_PORT);
	
	/**
	 * 是否挖矿
	 */
	public final static boolean MINING = getProperty("mining", false);
	
	/**
	 * 最小节点连接数，只要达到这个数量之后，节点才开始同步与监听数据，并提供网络服务
	 */
	public final static int MIN_CONNECT_COUNT = getProperty("min.connect.count", 1);


	/**
	 * 超级节点之间最大连接数
	 */
	public final static int MAX_SUPER_CONNECT_COUNT = getProperty("max.super.connect.count", 100);

	/**
	 * 普通节点链接的超级节点数
	 */
	public final static int MAX_NORMAL_SUPER_CONNECT_COUNT = getProperty("max.normal.super.connect.count", 2);

	/**
	 * 本节点是否是超级节点 0：no   1：yes
	 */
	public final static int IS_SUPER_NODE = getProperty("is.super.node", 0);

	/**
	 *
	 */
	public final static int MAX_ANTICODE_COUNT =  getProperty("max.anticode_count", 2000);

	/**
	 * 最大允许节点连接数
	 */
	public final static int MAX_CONNECT_COUNT = getProperty("max.connect.count", 10);
	
	/**
	 * 区块生成间隔时间，单位秒
	 */
	public final static int BLOCK_GEN_TIME = 10;
	/**
	 * 区块生成间隔时间，单位豪秒
	 */
	public final static long BLOCK_GEN__MILLISECOND_TIME = BLOCK_GEN_TIME * 1000;
	/**
	 * 挖矿奖励冻结区块数
	 */
	public final static int MINING_MATURE_COUNT = 1000;	// getProperty("block.gen.time", 1000);
	
	/**
	 * 数据存储目录
	 */
	public static String DATA_DIR = getProperty("data.dir", SERVER_HOME + File.separator +"data");
	/**
	 * 账户存储目录
	 */
	public static String DATA_ACCOUNT = DATA_DIR + File.separator + "account";
	/**
	 * 区块存储目录
	 */
	public static String DATA_BLOCK = DATA_DIR + File.separator + "block";
	/**
	 * 区块状态存储目录
	 */
	public static String DATA_CHAINSTATE = DATA_DIR + File.separator + "chainstate";
	/**
	 * 与帐户有关的交易存储目录
	 */
	public static String DATA_TRANSACTION = DATA_DIR + File.separator + "transaction";
	
	/*************  RPC 相关配置  begin  *****************/
	
	/**
	 * RPC服务默认地址
	 */
	public static String RPC_SERVER_HOST = "localhost";

	/**
	 * RPC 默认端口
	 */
	public static int RPC_SERVER_PORT = 8632;

	/**
	 * RPC 默认用户名
	 */
	public static String RPC_SERVER_USER = "user";
	/**
	 * PRC 消息体前缀固定8位记录信息长度
	 */
	public static int RPC_HEAD_LENGTH = 8;
	
	/*************  RPC 相关配置   end  *****************/
	

	/*************  账户相关配置  begin  *****************/
	
	/**
	 * 账户不存在时，是否自动创建
	 */
	public static boolean ACCOUNT_AUTO_INIT = getProperty("account.auto.init", false);
	
	/*************  账户相关配置   end  *****************/

	//TODO 临时设置为0
	/**
	 * 账户注册别名所需信用值
	 */
	public final static long REG_ALIAS_CREDIT = 1;
	/**
	 * 修改账户别名需信用达到值
	 */
	public final static long UPDATE_ALIAS_CREDIT = 1;
	/**
	 * 修改账户别名消耗信用值
	 */
	public final static long UPDATE_ALIAS_SUB_CREDIT = -1;
	/**
	 * 转让防伪码所需信用值
	 */
	public final static long TRANSFER_ANTIFAKECODE_CREDIT = 1;
	/**
	 * 转让防伪码消耗信用值
	 */
	public final static long TRANSFER_ANTIFAKECODE_SUB_CREDIT = -1;

	/**
	 * 认证账户level最大值
	 */
	public final static int MAX_CERT_LEVEL = 4;

	/**
	 * 被吊销的认证账户
	 */
	public final static int REVOKED_CERT_LEVEL = 0;

	/**
	 * 参与共识所需的信用点
	 */
	public final static long CONSENSUS_CREDIT = 1;
	
	/**
	 * 转账获得信用点数
	 */
	public final static long CERT_CHANGE_PAY = 1;
	
	/**
	 * 转账获得信用点数 - 间隔时间 ， 毫秒数
	 */
	public final static long CERT_CHANGE_PAY_INTERVAL = 24 * 60 * 60 * 1000l;
	/**
	 * 转账获得信用点数 - 间隔时间 ， 秒数
	 */
	public final static long CERT_CHANGE_PAY_INTERVAL_SECOND = CERT_CHANGE_PAY_INTERVAL / 1000l;

	/**
	 * 超时未出块惩罚信用点数
	 */
	public final static long CERT_CHANGE_TIME_OUT = -2;
	/**
	 * 严重违规的惩罚信用点数
	 */
	public final static long CERT_CHANGE_SERIOUS_VIOLATION = -9999999999l;

	/** 参与共识所需最低保证金 -- 1 W ins **/
	public static final Coin CONSENSUS_MIN_RECOGNIZANCE = Coin.COIN.multiply(10000);
	/** 参与共识所需最高保证金 **/
	public static final Coin CONSENSUS_MAX_RECOGNIZANCE = Coin.COIN.multiply(1000000);
	/** 资产发行注册手续费 --  1 W ins **/
	public static final Coin ASSETS_REG_FEE = Coin.COIN.multiply(10000);


	/*************  交易相关配置   begin  *****************/

	/**
	 * 转账时为花费交易选择机制
	 * 1 以交易数据小优先，该种机制尽量选择一笔输入
	 * 2 优先使用零钱
	 */
	public final static int TRANSFER_PREFERRED = getProperty("transfer.preferred", 1);

	//交易备注最大字符长度
	public final static int MAX_REMARK_LEN = 128;

	/*************  交易相关配置   end  *****************/

	/*************  系统级配置   begin  *****************/

	/**
	 * 认证账户列表存储键
	 */
	public final static byte[] CERT_ACCOUNT_KEYS = Sha256Hash.hashTwice("cert_account_keys".getBytes());


	/**
	 * 共识账户列表存储键
	 */
	public final static byte[] REVOKED_CERT_ACCOUNT_KEYS = Sha256Hash.hashTwice("revoked_cert_account_keys".getBytes());
	/**
	 * 共识账户列表存储键
	 */
	public final static byte[] CONSENSUS_ACCOUNT_KEYS = Sha256Hash.hashTwice("consensus_account_keys".getBytes());

	/**
	 * 资产注册列表存储键
	 */
	public final static byte[] ASSETS_REG_LIST_KEYS = Sha256Hash.hashTwice("assets_reg_list_keys".getBytes());


	/**
	 *  资产发行列表存储键首两位
	 */

	public final static byte[] ASSETS_ISSUE_FIRST_KEYS = new byte[]{1,1};


	/*************  系统级配置   begin  *****************/
	
    /**
     * 获取配置信息
     * @param name 配置名称
     * @param defaultValue  默认值
     * @return T
     */
	@SuppressWarnings("unchecked")
	public static <T> T getProperty(String name, T defaultValue) {
		T result = defaultValue;
		String value = property.getProperty(name);
		if(value == null) {
			return result;
		}
		try {
			if(defaultValue == null || defaultValue instanceof String) {
				return (T) value;
			} else if(defaultValue instanceof Integer) {
				return (T)((Integer) Integer.parseInt(value));
			} else if(defaultValue instanceof Long) {
				return (T)((Long) Long.parseLong(value));
			} else if(defaultValue instanceof Float) {
				return (T)((Float) Float.parseFloat(value));
			} else if(defaultValue instanceof Double) {
				return (T)((Double) Double.parseDouble(value));
			} else if(defaultValue instanceof Boolean) {
				return (T)((Boolean) Boolean.parseBoolean(value));
			} else {
				return result;
			}
		} catch (Exception e) {
			log.warn("配置{}出错，错误值{}，将使用默认值{}，错误信息：" + e, name, value, defaultValue);
			return result;
		}
    }
}
