package org.inchain.kit;

import java.io.IOException;

import org.inchain.kits.AppKit;
import org.inchain.listener.Listener;
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

	private AppKit appKit;
	
	private InchainInstance() {
		// 不允许外部创建实例
	}

	public static InchainInstance getInstance() {
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
	public void startup(int netType, Listener initListener) {
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
		
//		try {
//			Thread.sleep(1000l);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		
//		TestNetworkParams network = springContext.getBean(TestNetworkParams.class);
//		network.getSeedManager().getSeedList(100);
//		network.getSeedManager().add(new Seed(new InetSocketAddress("192.168.1.100", 6888)));
		
		//启动核心
		appKit = springContext.getBean(AppKit.class);
		if(initListener != null) {
			appKit.setInitListener(initListener);
		}
		appKit.startSyn();

		log.info("Server启动成功。");

	}

	
	public void shutdown() throws BeansException, IOException {
		springContext.getBean(AppKit.class).stop();
		springContext.stop();
		springContext.close();
		
		log.info("shutdown success");
	}
	
	public AppKit getAppKit() {
		return appKit;
	}
}
