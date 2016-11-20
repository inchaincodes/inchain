package org.inchain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.loadconfig.configuration.ConfigurableProcessor;
import com.loadconfig.configuration.Property;

/**
 * 配置
 * @author ln
 *
 */
public final class Configure {

	private static Logger log = LoggerFactory.getLogger(Configure.class);
	
	private static final String CONFIG_FILE = "config.conf";

	/**
	 * 是否挖矿
	 */
	@Property(key="mining", defaultValue="false")
	public static boolean MINING;
	
	/**
	 * 最大允许节点连接数
	 */
	@Property(key="max.connect.count", defaultValue="10")
	public static int MAX_CONNECT_COUNT;
	
	/**
	 * 区块生成间隔时间
	 */
	@Property(key="block.gen.time", defaultValue="10")
	public static int BLOCK_GEN_TIME;
	/**
	 * 挖矿奖励冻结区块数
	 */
	@Property(key="mining.mature.count", defaultValue="10")
	public static int MINING_MATURE_COUNT;
	/**
	 * 数据存储目录
	 */
	@Property(key="data.dir",defaultValue="./data")
	public static String DATA_DIR;
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
	@Property(key="rpc.server.port", defaultValue="8632")
	public static int RPC_SERVER_PORT;
	
	/*************  RPC 相关配置   end  *****************/
	
	
	static {
		load();
		DATA_ACCOUNT = DATA_DIR+File.separator+"account";
		DATA_BLOCK = DATA_DIR+File.separator+"block";
		DATA_CHAINSTATE = DATA_DIR+File.separator+"chainstate";
		DATA_TRANSACTION = DATA_DIR+File.separator+"transaction";
	}
	
	public static void load() {

		Properties properties = new Properties();
		InputStream stream = null;

		try {

			ClassLoader loader = Configure.class.getClassLoader();

			stream = loader.getResourceAsStream(CONFIG_FILE);

			properties.load(stream);

			ConfigurableProcessor.process(Configure.class, properties);

			if (log.isDebugEnabled()) {
				log.debug("加载配置文件完毕！");
			}

		} catch (IOException e) {
			log.error("加载配置文件出错，程序将退出！", e);
			System.exit(-1);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
