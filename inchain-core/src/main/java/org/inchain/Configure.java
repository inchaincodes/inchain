package org.inchain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * 配置
 * @author ln
 *
 */
public final class Configure extends PropertyPlaceholderConfigurer {

	private static Logger log = LoggerFactory.getLogger(Configure.class);
	
	private static final String CONFIG_FILE = "config.conf";
	
	/**
	 * 运行模式，1主网， 2 测试网络，3 单元测试
	 */
	public static int RUN_MODE = 2;

	/**
	 * p2p 端口
	 */
	public static int PORT = 8631;
	
	/**
	 * 是否挖矿
	 */
	public static boolean MINING = false;
	
	/**
	 * 最大允许节点连接数
	 */
	public static int MAX_CONNECT_COUNT = 10;
	
	/**
	 * 区块生成间隔时间
	 */
	public static int BLOCK_GEN_TIME = 10;
	/**
	 * 挖矿奖励冻结区块数
	 */
	public static int MINING_MATURE_COUNT = 10;
	/**
	 * 数据存储目录
	 */
	public static String DATA_DIR = "./data";
	/**
	 * 账户存储目录
	 */
	public static String DATA_ACCOUNT;
	/**
	 * 区块存储目录
	 */
	public static String DATA_BLOCK;
	/**
	 * 区块状态存储目录
	 */
	public static String DATA_CHAINSTATE;
	/**
	 * 与帐户有关的交易存储目录
	 */
	public static String DATA_TRANSACTION;
	
	/*************  RPC 相关配置  begin  *****************/
	
	/**
	 * RPC 端口
	 */
	public static int RPC_SERVER_PORT = 8632;
	
	/*************  RPC 相关配置   end  *****************/
	
	
	static {
		DATA_ACCOUNT = DATA_DIR+File.separator+"account";
		DATA_BLOCK = DATA_DIR+File.separator+"block";
		DATA_CHAINSTATE = DATA_DIR+File.separator+"chainstate";
		DATA_TRANSACTION = DATA_DIR+File.separator+"transaction";
	}
	
	private static Map<String,String> propertyMap;

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {
        super.processProperties(beanFactoryToProcess, props);
        propertyMap = new HashMap<String, String>();
        for (Object key : props.keySet()) {
            String keyStr = key.toString();
            String value = props.getProperty(keyStr);
            propertyMap.put(keyStr, value);
        }
    }

	public static String getProperty(String name) {
        return propertyMap.get(name);
    }
}
