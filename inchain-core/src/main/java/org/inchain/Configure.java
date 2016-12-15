package org.inchain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
	
	static {
		try {
			oloadProperties();
		} catch (IOException e) {
			log.error("加载配置文件出错", e);
		}
	}

    private static void oloadProperties() throws IOException {
    	InputStream in = Configure.class.getResourceAsStream("/config.properties");
    	property.load(in);
    	in.close();
    }
    
	/**
	 * 运行模式，1主网， 2 测试网络，3 单元测试
	 */
	public static int RUN_MODE = getProperty("run.mode", 2);

	/**
	 * p2p 端口
	 */
	public static int PORT = getProperty("port", 8631);
	
	/**
	 * 是否挖矿
	 */
	public static boolean MINING = getProperty("mining", false);
	
	/**
	 * 最大允许节点连接数
	 */
	public static int MAX_CONNECT_COUNT = getProperty("max.connect.count", 10);
	
	/**
	 * 区块生成间隔时间，单位秒
	 */
	public static int BLOCK_GEN_TIME = getProperty("block.gen.time", 10);
	/**
	 * 挖矿奖励冻结区块数
	 */
	public static int MINING_MATURE_COUNT = getProperty("block.gen.time", 10);
	/**
	 * 数据存储目录
	 */
	public static String DATA_DIR = getProperty("data.dir", "./data");
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
	 * RPC 端口
	 */
	public static int RPC_SERVER_PORT = getProperty("rpc.server.port", 8632);
	
	/*************  RPC 相关配置   end  *****************/
	
	/*************  账户相关配置  begin  *****************/
	
	/**
	 * 账户不存在时，是否自动创建
	 */
	public static boolean ACCOUNT_AUTO_INIT = getProperty("account.auto.init", false);
	
	/*************  账户相关配置   end  *****************/
	

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
