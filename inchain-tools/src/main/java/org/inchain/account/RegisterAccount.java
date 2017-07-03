package org.inchain.account;

import java.io.File;

import org.inchain.Configure;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.NetworkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RegisterAccount {
	
	private static Logger log = LoggerFactory.getLogger(RegisterAccount.class);

	public static void main(String[] args) throws Exception {
		
		String[] xmls = null;
		if(Configure.RUN_MODE == 1) {
			xmls = new String[] { "classpath:/applicationContext-mainnet.xml", "classpath:/applicationContext.xml" };
		} else if(Configure.RUN_MODE == 2) {
			xmls = new String[] { "classpath:/applicationContext-testnet.xml", "classpath:/applicationContext.xml" };
		} else {
			xmls = new String[] { "classpath:/applicationContext-unit.xml", "classpath:/applicationContext.xml" };
		}

		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(xmls);
		
		springContext.start();
		
		//测试前先清空帐户目录
		File dir = new File(Configure.DATA_ACCOUNT);
		if(dir.listFiles() != null) {
			for (File file : dir.listFiles()) {
				file.delete();
			}
		}
		
		PeerKit peerKit = springContext.getBean(PeerKit.class);
		peerKit.startSyn();
		
		AccountKit accountKit = springContext.getBean(AccountKit.class);
		NetworkParams networkParams = springContext.getBean(NetworkParams.class);
		try {
			Thread.sleep(1000l);
			if(accountKit.getAccountList().isEmpty()) {
				Account account = accountKit.createNewCertAccount("inchain123456", "inchain123", AccountBody.empty(), "");
				log.info("new address is : "+account.getAddress().getBase58());
			}
		} finally {
			accountKit.close();
			peerKit.stop();
			springContext.stop();
			springContext.close();
		}
	}
	
}
