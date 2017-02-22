package org.inchain.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.inchain.Configure;
import org.inchain.core.Peer;
import org.inchain.core.PeerAddress;
import org.inchain.core.TimeService;
import org.inchain.kits.PeerKit;
import org.inchain.message.AddressMessage;
import org.inchain.message.GetAddressMessage;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 节点发现服务
 * @author ln
 *
 */
@Service
public class PeerDiscoveryService implements PeerDiscovery , Serializable {
	
	private static final long serialVersionUID = -3380517531452438967L;

	private static final Logger log = LoggerFactory.getLogger(PeerDiscoveryService.class);
	
	private static final String PEER_INFO_SAVE_FILE = Configure.DATA_DIR + "/.peers";
	
	/*
	 * 验证失败节点重试时间间隔（毫秒）
	 */
	private static final long VERIFY_FAIL_RETRY_TIME = 10 * 60 * 1000l;	//10分钟
	/*
	 * 验证成功节点重新验证时间间隔（毫秒）
	 */
	private static final long VERIFY_SUCCESS_RETRY_TIME = 3 * 60 * 60 * 1000l; //3小时
	/*
	 * 广播getaddr消息获取对等节点拥有的地址间隔时间
	 */
	private static final long GET_ADDR_INTERVAL_TIME = 10 * 60 * 1000l; //10分钟
	/*
	 * 保存节点信息到磁盘的间隔时间
	 */
	private static final long STORAGE_INTERVAL_TIME = 10 * 60 * 1000l; //10分钟
	
	/*
	 * 节点状态，待验证
	 */
	private static final int PEER_STATUS_NEED_VERIFY = 1;
	/*
	 * 节点状态，验证通过
	 */
	private static final int PEER_STATUS_VERIFY_SUCCESS = 2;
	/*
	 * 节点状态，验证失败
	 */
	private static final int PEER_STATUS_VERIFY_FAIL = 3;
	
	//服务锁
	private Lock locker = new ReentrantLock();
	
	//节点验证服务运行状态
	private volatile boolean runing;
	//网络地址列表，整个网络上报的地址都会在里面
	private volatile List<PeerAddressStore> netaddressMaps;
	//已认证可用的节点，由验证服务自动维护，系统启动时会加载之前的
	private volatile List<PeerAddressStore> verifyedMaps = new CopyOnWriteArrayList<PeerAddressStore>();
	//可连接列表
	private volatile List<PeerAddressStore> canuseMaps = new CopyOnWriteArrayList<PeerAddressStore>();
	//状态表，1未连接，2已连接
	private volatile Map<PeerAddressStore, Seed> connectedStatusMaps = new HashMap<PeerAddressStore, Seed>();
	//是否已经加载dns节点
	private boolean hasLoadDns;
	//最后获取地址的时间
	private long lastGetAddrTime;
	//最后一次存储时间，节点信息会在启动时加载到内存里面，然后更新维护都是在内存里面，定期进行持久化
	private long lastStorageTime;
	
	@Autowired
	private PeerKit peerKit;
	@Autowired
	private NetworkParams network;

	/**
	 * 初始化
	 * 加载本地存储数据
	 */
	public void startSync() {
		new Thread() {
			public void run() {
				PeerDiscoveryService.this.start();
			};
		}.start();
	}
	
	/**
	 * 程序关闭时，持久化内存里面的节点信息到文件
	 */
	public void shutdown() {
		runing = false;
		
		writeObjectToFile(netaddressMaps);
	}
	
	/*
	 * 启动
	 */
	private void start() {
		locker.lock();
		
		try {
			//读取节点列表信息
			netaddressMaps = readObjectFromFile();
			
			if(netaddressMaps == null) {
				netaddressMaps = new CopyOnWriteArrayList<PeerAddressStore>();
			} else {
				//加载已认证的节点
				for (PeerAddressStore peerAddressStore : netaddressMaps) {
					if(peerAddressStore.getStatus() == PEER_STATUS_VERIFY_SUCCESS) {
						verifyedMaps.add(peerAddressStore);
						canuseMaps.add(peerAddressStore);
					}
				}
				//打乱顺序
				Collections.shuffle(canuseMaps);
			}
			
			//启动时标记最后存储时间为当前时间
			lastStorageTime = TimeService.currentTimeMillis();
		} finally {
			locker.unlock();
		}
		
		log.info("节点发现服务初始化成功，加载节点{}个", netaddressMaps.size());

		runing = true;
		
		while(runing) {
			
			//扫描需要重试的节点
			Iterator<Entry<PeerAddressStore, Seed>> it = connectedStatusMaps.entrySet().iterator();
			while(it.hasNext()) {
				Entry<PeerAddressStore, Seed> entry = it.next();
				Seed seed = entry.getValue();
				if(seed.getStaus() == Seed.SEED_CONNECT_FAIL || seed.getStaus() == Seed.SEED_CONNECT_CLOSE) {
					if(!seed.isRetry()) {
						it.remove();
					} else if(seed.isRetry() && TimeService.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()) {
						//如果还在已认证列表里，则加入可用列表进行重试
						PeerAddressStore peerAddress = entry.getKey();
						if(verifyedMaps.contains(peerAddress)) {
							canuseMaps.add(peerAddress);
						}
						it.remove();
					}
				}
			}
			
			//需要移除的节点，一般验证失败次数过多，则会被移除
			List<PeerAddressStore> removePeerAddresses = null;
			
			for (PeerAddressStore peerAddressStore : netaddressMaps) {
				if(!runing) {
					return;
				}
				//该节点是否需要验证
				boolean needVerify = false;
				if((peerAddressStore.getStatus() == PEER_STATUS_NEED_VERIFY || peerAddressStore.getStatus() == PEER_STATUS_VERIFY_FAIL) && 
						(TimeService.currentTimeMillis() - peerAddressStore.getLastVerifyTime() > VERIFY_FAIL_RETRY_TIME)) {
					//没有验证过的节点，或者验证失败的节点，每10分钟验证一次
					needVerify = true;
				} else if(peerAddressStore.getStatus() == PEER_STATUS_VERIFY_SUCCESS && 
						(TimeService.currentTimeMillis() - peerAddressStore.getLastVerifyTime() > VERIFY_SUCCESS_RETRY_TIME)) {
					//验证成功的节点，每3小时验证一次
					needVerify = true;
				}
				if(needVerify) {
					//待验证
					peerAddressStore.setLastVerifyTime(TimeService.currentTimeMillis());
					//那么就去验证
					boolean success = peerKit.verifyPeer(new PeerAddress(peerAddressStore.getAddr(), peerAddressStore.getPort()));
					if(success) {
						peerAddressStore.setStatus(PEER_STATUS_VERIFY_SUCCESS);
						peerAddressStore.setVerifySuccessCount(peerAddressStore.getVerifySuccessCount() + 1);
						
						//加入已认证列表
						if(!verifyedMaps.contains(peerAddressStore)) {
							verifyedMaps.add(peerAddressStore);
							canuseMaps.add(peerAddressStore);
						}
					} else {
						peerAddressStore.setStatus(PEER_STATUS_VERIFY_FAIL);
						peerAddressStore.setVerifyFailCount(peerAddressStore.getVerifyFailCount() + 1);
						
						//从已认证列表中删除
						if(verifyedMaps.contains(peerAddressStore)) {
							verifyedMaps.remove(peerAddressStore);
							canuseMaps.remove(peerAddressStore);
						}
						
						//是否移除该节点
						boolean isRemove = false;
						//通过一个简单的规则判断是否移除，失败次数达到成功次数的10倍以上
						//比如某个节点曾经验证成功2次，则失败次数达到20次以上，则认为该节点不是稳定的节点，则移除掉
						if(peerAddressStore.getVerifySuccessCount() == 0 && peerAddressStore.getVerifyFailCount() > 10) {
							isRemove = true;
						} else if(peerAddressStore.getVerifySuccessCount() != 0 && peerAddressStore.getVerifyFailCount() / peerAddressStore.getVerifySuccessCount() >= 10) {
							isRemove = true;
						}
						if(isRemove) {
							//加入移除列表
							if(removePeerAddresses == null) {
								removePeerAddresses = new ArrayList<PeerAddressStore>();
							}
							removePeerAddresses.add(peerAddressStore);
						}
					}
					try {
						//限制频率，太快会对网络造成影响
						Thread.sleep(1000l);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			//定期清理验证失败的节点
			if(removePeerAddresses != null) {
				netaddressMaps.removeAll(removePeerAddresses);
			}
			try {
				//定期把节点信息刷新到磁盘
				if(TimeService.currentTimeMillis() - lastStorageTime > STORAGE_INTERVAL_TIME) {
					lastStorageTime = TimeService.currentTimeMillis();
					writeObjectToFile(netaddressMaps);
				}
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 添加一个节点
	 * @param peerAddress
	 */
	public boolean add(PeerAddress peerAddress) {
		return add(peerAddress, true);
	}
	
	/**
	 * 添加一个节点
	 * @param peerAddress
	 * @param hasVerify 是否已经验证
	 */
	public boolean add(PeerAddress peerAddress, boolean hasVerify) {
		//如果已经存在，则返回false
		PeerAddressStore pas = getPeerAddressStore(peerAddress);
		if(pas == null) {
			pas = new PeerAddressStore();
			pas.setStatus(hasVerify ? PEER_STATUS_VERIFY_SUCCESS:PEER_STATUS_NEED_VERIFY);	//未经验证的，验证通过才能使用
			pas.setAddr(peerAddress.getAddr());
			pas.setHostname(peerAddress.getHostname());
			pas.setPort(peerAddress.getPort());
			pas.setServices(peerAddress.getServices());
			pas.setTime(peerAddress.getTime());
			netaddressMaps.add(pas);
			return true;
		} else {
			//修改时间
			pas.setTime(peerAddress.getTime());
			return false;
		}
	}
	
	/**
	 * 添加一个节点
	 * @param peerAddress
	 * @param hasVerify 是否已经验证
	 */
	public PeerAddressStore addAndReturn(PeerAddress peerAddress, boolean hasVerify) {
		add(peerAddress, hasVerify);
		return getPeerAddressStore(peerAddress);
	}
	
	/**
	 * 批量添加节点，未经验证的
	 * @param addresses
	 */
	public void addBath(List<PeerAddress> addresses) {
		for (PeerAddress peerAddress : addresses) {
			add(peerAddress, false);
		}
	}

	/**
	 * 获取可用的节点列表
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getAvailablePeerAddress() {
		return getAvailablePeerAddress(AddressMessage.MAX_ADDRESSES);
	}

	/**
	 * 获取可用的节点列表
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getAvailablePeerAddress(int maxCount) {
		List<PeerAddress> list = new ArrayList<PeerAddress>();
		
		for (PeerAddressStore peerAddress : netaddressMaps) {
			if(peerAddress.getStatus() == PEER_STATUS_VERIFY_SUCCESS) {
				list.add(new PeerAddress(peerAddress.getAddr(), peerAddress.getPort()));
				if(list.size() >= maxCount) {
					return list;
				}
			}
		}
		return list;
	}
	
	/**
	 * 获取可连接的节点列表
	 * @return List<Seed>
	 */
	public List<Seed> getCanConnectPeerSeeds() {
		return getCanConnectPeerSeeds(AddressMessage.MAX_ADDRESSES);
	}
	
	/**
	 * 获取可连接的节点列表
	 * 逻辑相对比较复杂
	 * 1、首先获取本地缓存的节点，如果不存在，则通过dns获取种子节点
	 * 2、获取到的dns种子节点，一般认为是7X24小时在线的完整节点，特别设置一个断线重试参数
	 * 3、如果本地缓存节点不为空，则首先过滤曾经验证过，并且连接过的节点尝试连接，如果没有认证过的节点，则通过dns获取种子节点
	 * 4、如果本地缓存的节点数量不够，则依然加入dns的节点
	 * 5、如果本地存储的节点，和dns种子节点都不够用，那么每隔一段时间会像已连接的节点发送getaddr消息，已尝试发现更多的节点进行连接
	 * 6、未来通过tcp打洞方式，穿透内网主机，让更多的内网机器为外网提供服务
	 * 
	 * @param maxCount 获取可用节点的最大数量
	 * @return List<Seed>
	 */
	public List<Seed> getCanConnectPeerSeeds(int maxCount) {
		locker.lock();
		
		List<Seed> list = new ArrayList<Seed>();
		try {
			if(canuseMaps.isEmpty() && !hasLoadDns) {
				return getDnsSeeds(maxCount);
			} else {
				int count = Math.min(maxCount, canuseMaps.size());
				for (int i = 0; i < count; i++) {
					PeerAddressStore peerAddress = canuseMaps.remove(0);
					Seed seed = new Seed(new InetSocketAddress(peerAddress.getAddr(), peerAddress.getPort()));
					list.add(seed);
					connectedStatusMaps.put(peerAddress, seed);
				}
				//如果没有了，则请求，但是最多10分钟请求一次
				if(canuseMaps.isEmpty() && (TimeService.currentTimeMillis() - lastGetAddrTime > GET_ADDR_INTERVAL_TIME) 
						&& peerKit.canBroadcast()) {
					lastGetAddrTime = TimeService.currentTimeMillis();
					peerKit.broadcastMessage(new GetAddressMessage(network, TimeService.currentTimeMillis()));
				}
			}
		} catch (Exception e) {
			log.error("获取可连接的节点列表出错: {}", e.getMessage());
		} finally {
			locker.unlock();
		}
		return list;
	}

	/*
	 * 获取dns节点
	 */
	private List<Seed> getDnsSeeds(int maxCount) {
		//本次验证列表为空，那么立刻返回dns节点
		SeedManager seedManager = network.getSeedManager();
		//一般dns绑定的的节点数量都会比设置的最大连接数少，所以这里获取dns节点只加载一次即可
		hasLoadDns = true;
		if(seedManager.hasMore()) {
			List<Seed> list = seedManager.getSeedList(maxCount);
			List<Seed> result = new ArrayList<Seed>();
			//把这些节点放到 netaddressMaps中去
			for (Seed seed : list) {
				//加入列表，不会重复加入
				PeerAddressStore peerAddress = addAndReturn(new PeerAddress(seed.getAddress()), false);
				//过滤掉重复的，也就是已经连接上的
				boolean hasConnected = false;
				for (PeerAddressStore peerAddressStore : connectedStatusMaps.keySet()) {
					if(peerAddress.getAddr().equals(peerAddressStore.getAddr()) && peerAddress.getPort() == peerAddressStore.getPort()) {
						hasConnected = true;
						break;
					}
				}
				if(!hasConnected) {
					result.add(seed);
					connectedStatusMaps.put(peerAddress, seed);
				}
			}
			return result;
		} else {
			return new ArrayList<Seed>();
		}
	}

	/**
	 * 刷新节点的连接状态
	 * @param seed
	 */
	@Override
	public void refreshSeedStatus(Seed seed) {
		//一共有3中状态，连接成功，连接失败，连接断开
		//连接成功的，设置断开重新连接时间
		//连接失败的，设置更久
		if(seed.getStaus() == Seed.SEED_CONNECT_FAIL) {
			//5分钟之后重试，一共3次，15分钟内不成功则永远剔除
			if(seed.getFailCount() < 3) {
				seed.setFailCount(seed.getFailCount() + 1);
				seed.setRetry(true);
				seed.setRetryInterval(5 * 60 * 1000);
				seed.setLastTime(TimeService.currentTimeMillis());
			} else {
				seed.setRetry(false);
			}
		} else if(seed.getStaus() == Seed.SEED_CONNECT_CLOSE) {
			//主动被关闭的节点，1分钟之后重试
			seed.setRetry(true);
			seed.setRetryInterval(1 * 60 * 1000);
			seed.setLastTime(TimeService.currentTimeMillis());
		} else if(seed.getStaus() == Seed.SEED_CONNECT_SUCCESS) {
			//连接成功，则重置失败次数
			seed.setFailCount(0);
		}
	}
	
	/**
	 * 节点是否已经存在（已被发现）
	 * @param peerAddress
	 * @return boolean
	 */
	@Override
	public boolean hasExist(PeerAddress peerAddress) {
		if(peerAddress == null) {
			return false;
		}
		for (PeerAddressStore pas : netaddressMaps) {
			if(pas.getAddr().equals(peerAddress.getAddr()) && pas.getPort() == peerAddress.getPort()) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * 获取对应的存储节点信息
	 */
	private PeerAddressStore getPeerAddressStore(PeerAddress peerAddress) {
		if(peerAddress == null) {
			return null;
		}
		for (PeerAddressStore pas : netaddressMaps) {
			if(pas.getAddr().equals(peerAddress.getAddr()) && pas.getPort() == peerAddress.getPort()) {
				return pas;
			}
		}
		return null;
	}

	/**
	 * 检查本机服务是否对外提供，如果提供则上传
	 */
	public void checkMyserviceAndReport() {
		//获取本机的外网ip地址
		List<byte[]> ips = new ArrayList<byte[]>(); 
		for (Peer peer : peerKit.findAvailablePeers()) {
			PeerAddress addr = peer.getPeerVersionMessage().getTheirAddr();
			ips.add(addr.getAddr().getAddress());
		}
		//次数最多的外网ip
		//避免ip地址欺骗
		Map<String, Integer> maps = new HashMap<String, Integer>();
		for (byte[] ipTemp : ips) {
//			if(!IpUtil.internalIp(ipTemp)) {
				String str = Hex.encode(ipTemp);
				Integer count = maps.get(str);
				if(count == null) {
					maps.put(str, 1);
				} else {
					maps.put(str, count+1);
				}
//			}
		}
		
		if(maps.size() == 0) {
			//没有获取到外网ip，不对外提供服务
			return;
		}
		
		String ipStr = null;
		int count = 0;
		for (Entry<String, Integer> entry : maps.entrySet()) {
			Integer v = entry.getValue();
			if(v != null && v.intValue() > count) {
				count = v;
				ipStr = entry.getKey();
			}
		}
		InetAddress myIp = null;
		try {
			myIp = InetAddress.getByAddress(Hex.decode(ipStr));
		} catch (Exception e) {
			log.error("地址上报出错，无法解析本机外网ip");
			return;
		}
		//探测本机是否能为外网提供服务
		if(log.isDebugEnabled()) {
			log.debug("本机ip {} 开始探测是否能对外提供服务···", myIp);
		}
		PeerAddress peerAddress = new PeerAddress(network, myIp);
		if(!hasExist(peerAddress) && peerKit.verifyPeer(peerAddress)) {
			AddressMessage addressMessage = new AddressMessage(network);
			addressMessage.addAddress(peerAddress);
			int broadcastCount = peerKit.broadcastMessage(addressMessage);

			if(log.isDebugEnabled()) {
				log.debug("本机能对外提供服务, 已通过{}个节点广播到p2p网络中", broadcastCount);
			}
		}
	}
	
	/**
	 * 把对象写进文件
	 * @param objs
	 */
	public static void writeObjectToFile(List<PeerAddressStore> objs) {
        File file = new File(PEER_INFO_SAVE_FILE);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(objs);
            objOut.flush();
            objOut.close();
            out.close();

            if(log.isDebugEnabled()) {
            	log.debug("write peer nodes success!");
            }
        } catch (IOException e) {
        	log.warn("write peer nodes failed! {}", e.getMessage());
        }
    }
	
	/**
	 * 从文件中读取对象
	 * @return Object
	 */
	@SuppressWarnings("unchecked")
	public static List<PeerAddressStore> readObjectFromFile() {
        File file =new File(PEER_INFO_SAVE_FILE);
        if(!file.exists()) {
        	return new CopyOnWriteArrayList<PeerAddressStore>(); 
        }
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn = new ObjectInputStream(in);
            Object temp = objIn.readObject();
            objIn.close();
            in.close();
            
            if(log.isDebugEnabled()) {
            	log.debug("read peer nodes success!");
            }
            return (List<PeerAddressStore>) temp;
        } catch (Exception e) {
        	log.warn("read peer file failed! {}", e.getMessage());
        	file.delete();
        }
        return null;
    }
}
