package org.inchain.kit;

import java.io.IOException;

import org.inchain.kits.AppKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 印链核心实例
 * @author ln
 *
 */
public final class InchainInstance {
	
	private static final Logger log = LoggerFactory.getLogger(InchainInstance.class);

	private static InchainInstance INSTANCE;
	
	private ClassPathXmlApplicationContext springContext;
	
	private InchainInstance() {
		// 不允许外部创建实例
	}

	public static InchainInstance newInstance() {
		if (INSTANCE == null) {
			INSTANCE = new InchainInstance();
		}
		return INSTANCE;
	}
	
	/**
	 * 启动核心
	 * @param netType 网络类型，1正式网络，2测试网络
	 * @throws IOException
	 */
	public void startup(int netType) {
		// 通过Spring启动服务器
		String[] xmls = null;
		if(netType == 1) {
			xmls = new String[] { "classpath:/applicationContext-mainnet.xml", "classpath:/applicationContext.xml" };
		} else if(netType == 2) {
			xmls = new String[] { "classpath:/applicationContext-testnet.xml", "classpath:/applicationContext.xml" };
		} else {
			throw new RuntimeException("netType error");
		}

		springContext = new ClassPathXmlApplicationContext(xmls);
		
		springContext.start();

		log.info("Server启动成功。");

	}

	
	public void shutdown() throws BeansException, IOException {
		springContext.getBean(AppKit.class).stop();
		springContext.stop();
		springContext.close();
	}
	
}
