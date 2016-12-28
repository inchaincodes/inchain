package org.inchain.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.inchain.Configure;
import org.inchain.core.TimeHelper;
import org.inchain.utils.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远程种子节点管理
 * @author ln
 *
 */
public class RemoteSeedManager implements SeedManager {
	
	private final static Logger log = LoggerFactory.getLogger(RemoteSeedManager.class);
	
	private final static Set<String> SEED_DOMAINS = new HashSet<String>(); 
	
	private final List<Seed> list = new ArrayList<Seed>();
	

	public RemoteSeedManager() {
		SEED_DOMAINS.add("test1.seed.inchain.org");
		SEED_DOMAINS.add("test2.seed.inchain.org");
		
		new Thread() {
			public void run() {
				startInit();
			};
		}.start();
	}

	@Override
	public List<Seed> getSeedList(int maxConnectionCount) {
		List<Seed> newList = new ArrayList<Seed>();
		List<Seed> removeList = new ArrayList<Seed>();
		//排除连接失败且不重试的
		for (Seed seed : list) {
			if(seed.getStaus() == Seed.SEED_CONNECT_WAIT ||
				(seed.getStaus() == Seed.SEED_CONNECT_FAIL && seed.isRetry() &&
					(TimeHelper.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()))) {
				newList.add(seed);
				seed.setStaus(Seed.SEED_CONNECT_ING);
			} else if(seed.getStaus() == Seed.SEED_CONNECT_FAIL && !seed.isRetry()) {
				removeList.add(seed);
			}
		}
		list.removeAll(removeList);
		return newList;
	}

	@Override
	public boolean hasMore() {
		for (Seed seed : list) {
			if(seed.getStaus() == Seed.SEED_CONNECT_WAIT ||
				(seed.getStaus() == Seed.SEED_CONNECT_FAIL && seed.isRetry() &&
					(System.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()))) {
				return true;
			}
		}
		return false;
	}

	public boolean add(Seed node) {
		return list.add(node);
	}
	
	/**
	 * 初始化种子节点
	 */
	protected void startInit() {
		try {
			Thread.sleep(1000);
			
			if(log.isDebugEnabled()) {
				log.debug("初始化种子节点");
			}
			Set<String> myIps = IpUtil.getIps();
			for (String seedDomain : SEED_DOMAINS) {
				InetAddress[] response = InetAddress.getAllByName(seedDomain);
				for (InetAddress inetAddress : response) {
					//排除自己
					if(myIps.contains(inetAddress.getHostAddress())) {
						continue;
					}
					//若连接失败，5分钟后重试
					Seed seed = new Seed(new InetSocketAddress(inetAddress, Configure.PORT), false, 5 * 60000);
					add(seed);
				}
			}
			
			if(log.isDebugEnabled()) {
				log.debug("种子节点初始化完成");
			}
		} catch (Exception e) {
			log.debug("种子节点获取出错", e);
		}
	}
}
