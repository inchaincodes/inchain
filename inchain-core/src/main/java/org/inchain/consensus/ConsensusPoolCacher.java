package org.inchain.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.network.NetworkParams;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 共识池节点缓存，所有注册参与共识的节点都会存放到里面
 * 当有阶段加入或者退出，这里同时更新维护
 * @author ln
 *
 */
@Component
public class ConsensusPoolCacher implements ConsensusPool {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Map<byte[], byte[]> container = new HashMap<byte[], byte[]>();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	
	@PostConstruct
	public void init() {
		container.putAll(chainstateStoreProvider.loadAllConsensusAccount());
		
		if(log.isDebugEnabled()) {
			log.debug("====================");
			log.debug("加载已有的{}个共识", container.size());
			for (Entry<byte[], byte[]> entry : container.entrySet()) {
				log.debug(Hex.encode(entry.getKey()));
			}
			log.debug("====================");
		}
		
		verify();
	}
	
	private void verify() {
		List<byte[]> errors = new ArrayList<byte[]>();
		for (Entry<byte[], byte[]> entry : container.entrySet()) {
			if(!verifyOne(entry.getKey(), entry.getValue())) {
				errors.add(entry.getKey());
			}
		}
		for (byte[] hash160 : errors) {
			container.remove(hash160);
		}
	}

	/**
	 * 新增共识结点
	 * @param hash160
	 */
	public void add(byte[] hash160, byte[] pubkey) {
		if(!verifyOne(hash160, pubkey)) {
			log.warn("公钥不匹配的共识");
			return;
		}
		container.put(hash160, pubkey);
	}
	
	/**
	 * 验证hash160和公钥是否匹配
	 * @param hash160  地址
	 * @param pubkey  公钥
	 * @return
	 */
	public boolean verifyOne(byte[] hash160, byte[] pubkey) {
		Address address = AccountTool.newAddress(network, ECKey.fromPublicOnly(pubkey));
		if(!Arrays.equals(hash160, address.getHash160())) {
			return false;
		}
		return true;
	}

	/**
	 * 移除共识节点
	 */
	public void delete(byte[] hash160) {
		container.remove(hash160);
	}
	
	/**
	 * 判断是否是共识节点
	 * @param hash160
	 * @return boolean
	 */
	public boolean contains(byte[] hash160) {
		return container.get(hash160) != null;
	}
	
	/**
	 * 获取共识节点的公钥
	 * @param hash160
	 * @return byte[]
	 */
	public byte[] get(byte[] hash160) {
		return container.get(hash160);
	}
	
	public Map<byte[], byte[]> getContainer() {
		return container;
	}
}
