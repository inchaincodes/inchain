package org.inchain.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.kits.AccountKit;
import org.inchain.network.NetworkParams;
import org.inchain.store.AccountStore;
import org.inchain.store.ChainstateStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 共识池节点缓存，所有注册参与共识的节点都会存放到里面
 * 当有节点加入或者退出，这里同时更新维护
 * @author ln
 *
 */
@Component
public class ConsensusPoolCacher implements ConsensusPool {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final Map<byte[], byte[][]> container = new HashMap<byte[], byte[][]>();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private AccountKit accountKit;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	
	@PostConstruct
	public void init() {
		List<AccountStore> list = accountKit.getConsensusAccounts();
		
		for (AccountStore account : list) {
			container.put(account.getHash160(), account.getPubkeys());
		}
		
		if(log.isDebugEnabled()) {
			log.debug("====================");
			log.debug("加载已有的{}个共识", container.size());
			for (Entry<byte[], byte[][]> entry : container.entrySet()) {
				log.debug(new Address(network, entry.getKey()).getBase58());
			}
			log.debug("====================");
		}
		
		verify();
	}
	
	private void verify() {
		List<byte[]> errors = new ArrayList<byte[]>();
		for (Entry<byte[], byte[][]> entry : container.entrySet()) {
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
	public void add(byte[] hash160, byte[][] pubkey) {
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
	 * @return boolean
	 */
	public boolean verifyOne(byte[] hash160, byte[][] pubkey) {
		//如果在链上查不到账户信息，那么就当普通账户处理
		AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
		
		if(accountStore == null || accountStore.getType() == network.getSystemAccountVersion()) {
			//普通账户
			Address address = AccountTool.newAddress(network, ECKey.fromPublicOnly(pubkey[0]));
			if(!Arrays.equals(hash160, address.getHash160())) {
				return false;
			}
		} else {
			//认证账户
			byte[][] realPubkeys = accountStore.getPubkeys();
			if(realPubkeys.length != pubkey.length) {
				return false;
			}
			for (int i = 0; i < pubkey.length; i++) {
				if(!Arrays.equals(pubkey[i], realPubkeys[i])) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 移除共识节点
	 */
	public void delete(byte[] hash160) {
		byte[] hash160Temp = null;
		for (Entry<byte[], byte[][]> entry : container.entrySet()) {
			if(Arrays.equals(hash160, entry.getKey())) {
				hash160Temp = entry.getKey();
				break;
			}
		}
		if(hash160Temp != null) {
			container.remove(hash160Temp);
		}
	}
	
	/**
	 * 判断是否是共识节点
	 * @param hash160
	 * @return boolean
	 */
	public boolean contains(byte[] hash160) {
		for (Entry<byte[], byte[][]> entry : container.entrySet()) {
			if(Arrays.equals(hash160, entry.getKey())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 获取共识节点的公钥
	 * @param hash160
	 * @return byte[]
	 */
	public byte[][] getPubkey(byte[] hash160) {
		for (Entry<byte[], byte[][]> entry : container.entrySet()) {
			if(Arrays.equals(hash160, entry.getKey())) {
				//TODO
				return entry.getValue();
			}
		}
		return null;
	}
	
	public Map<byte[], byte[][]> getContainer() {
		return container;
	}
	
	/**
	 * 当前共识节点列表快照
	 * @return List<ConsensusAccount>
	 */
	@Override
	public List<ConsensusAccount> listSnapshots() {
		//TODO处理并发情况
		
		List<ConsensusAccount> list = new ArrayList<ConsensusAccount>();
		
		for (Entry<byte[], byte[][]> entry : container.entrySet()) {
			list.add(new ConsensusAccount(entry.getKey(), entry.getValue()));
		}
		
		//排序
		if(list.size() > 1) {
			list.sort(new Comparator<ConsensusAccount>() {
				@Override
				public int compare(ConsensusAccount o1, ConsensusAccount o2) {
					return o1.getHash160Hex().compareTo(o2.getHash160Hex());
				}
			});
		}
		
		return list;
	}
}
