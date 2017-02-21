package org.inchain.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.inchain.Configure;
import org.inchain.core.PeerAddress;
import org.inchain.kits.PeerKit;
import org.inchain.message.AddressMessage;
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
	
	private volatile boolean runing;
	//网络地址列表
	private volatile List<PeerAddressStore> netaddressMap;
	//状态表，1未连接，2已连接
	private volatile Map<PeerAddressStore, Integer> netaddressConnectedStatusMap = new HashMap<PeerAddressStore, Integer>();
	
	@Autowired
	private PeerKit peerKit;

	/**
	 * 初始化
	 * 加载本地存储数据
	 */
	@PostConstruct
	public void init() {
		new Thread() {
			public void run() {
				PeerDiscoveryService.this.start();
			};
		}.start();
	}
	
	/**
	 * 程序关闭时，持久化内存里面的节点信息到文件
	 */
	@PreDestroy
	public void shutdown() {
		runing = false;
		
		writeObjectToFile(netaddressMap);
	}
	
	/*
	 * 启动
	 */
	private void start() {
		netaddressMap = readObjectFromFile();
		
		if(netaddressMap == null) {
			netaddressMap = new CopyOnWriteArrayList<PeerAddressStore>();
		}
		
		log.info("初始化成功，加载节点{}个", netaddressMap.size());
		
		runing = true;
		
		while(runing) {
			
			for (PeerAddressStore peerAddressStore : netaddressMap) {
				if(!runing) {
					return;
				}
				if(peerAddressStore.getStatus() == 0) {
					//待验证
					//那么就去验证
					boolean success = peerKit.verifyPeer(new PeerAddress(peerAddressStore.getAddr(), peerAddressStore.getPort()));
					if(success) {
						peerAddressStore.setStatus(1);
					} else {
						peerAddressStore.setStatus(2);
					}
				}
			}
			//定期清理验证失败的节点
			
			try {
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
			pas.setStatus(hasVerify ? 1:0);	//未经验证的，验证通过才能使用
			pas.setAddr(peerAddress.getAddr());
			pas.setHostname(peerAddress.getHostname());
			pas.setPort(peerAddress.getPort());
			pas.setServices(peerAddress.getServices());
			pas.setTime(peerAddress.getTime());
			netaddressMap.add(pas);
			return true;
		} else {
			//修改时间
			pas.setTime(peerAddress.getTime());
			return false;
		}
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
		
		for (PeerAddressStore peerAddress : netaddressMap) {
			if(peerAddress.getStatus() == 1) {
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
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getCanConnectPeerAddress() {
		return getAvailablePeerAddress(AddressMessage.MAX_ADDRESSES);
	}

	/**
	 * 获取可连接的节点列表
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getCanConnectPeerAddress(int maxCount) {
		List<PeerAddress> list = new ArrayList<PeerAddress>();
		
		for (PeerAddressStore peerAddress : netaddressMap) {
			if(peerAddress.getStatus() == 1) {
				Integer connectionStatus = netaddressConnectedStatusMap.get(peerAddress);
				if(connectionStatus == null || connectionStatus.intValue() == 1) {
					netaddressConnectedStatusMap.put(peerAddress, 2);
					list.add(new PeerAddress(peerAddress.getAddr(), peerAddress.getPort()));
					if(list.size() >= maxCount) {
						return list;
					}
				}
			}
		}
		return list;
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
		for (PeerAddressStore pas : netaddressMap) {
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
		for (PeerAddressStore pas : netaddressMap) {
			if(pas.getAddr().equals(peerAddress.getAddr()) && pas.getPort() == peerAddress.getPort()) {
				return pas;
			}
		}
		return null;
	}


	/**
	 * 是否已经连接过了
	 * @param peerAddress
	 * @return boolean
	 */
	@Override
	public boolean hasConnected(PeerAddress peerAddress) {
		for (Entry<PeerAddressStore, Integer> entry : netaddressConnectedStatusMap.entrySet()) {
			PeerAddressStore peerAddressStore = entry.getKey();
			if(peerAddressStore.getAddr().equals(peerAddress.getAddr()) && peerAddressStore.getPort() == peerAddress.getPort()) {
				Integer status = entry.getValue();
				return status != null && status.intValue() == 2;
			}
		}
		return false;
	}
	
	public class PeerAddressStore implements Serializable {
		private static final long serialVersionUID = 8821950401288230577L;

		private InetAddress addr;
	    private String hostname;
	    private int port;
	    private long services;
	    private long time;
	    private int status;	//状态，0待验证，1验证通过，2验证失败
	    
		public InetAddress getAddr() {
			return addr;
		}
		public void setAddr(InetAddress addr) {
			this.addr = addr;
		}
		public String getHostname() {
			return hostname;
		}
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public long getServices() {
			return services;
		}
		public void setServices(long services) {
			this.services = services;
		}
		public long getTime() {
			return time;
		}
		public void setTime(long time) {
			this.time = time;
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
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
        	log.warn("read peer saved file failed! {}", e.getMessage());
        }
        return null;
    }
	
}
