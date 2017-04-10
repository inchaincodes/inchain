package org.inchain.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.inchain.Configure;
import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
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
	
	private final Map<byte[], byte[][]> container = new ConcurrentHashMap<byte[], byte[][]>();
	private final Map<byte[], Sha256Hash> txContainer = new ConcurrentHashMap<byte[], Sha256Hash>();
	
	@Autowired
	private NetworkParams network;
	@Autowired
	private ChainstateStoreProvider chainstateStoreProvider;
	
	@PostConstruct
	public void init() {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100l);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				initConsensusAccounts();
			}
		};
		t.setName("ConsensusPoolCacher init");
		t.start();
	}
	
	/*
	 * 加载当前共识账户列表
	 */
	private void initConsensusAccounts() {
		byte[] consensusAccounts = chainstateStoreProvider.getBytes(Configure.CONSENSUS_ACCOUNT_KEYS);
		if(consensusAccounts == null) {
			return;
		}
		for (int i = 0; i < consensusAccounts.length; i += (Address.LENGTH + Sha256Hash.LENGTH)) {
			byte[] hash160 = Arrays.copyOfRange(consensusAccounts, i, i + Address.LENGTH);
			AccountStore accountStore = chainstateStoreProvider.getAccountInfo(hash160);
			if(accountStore == null) {
				continue;
			}
			
			byte[] txhash = Arrays.copyOfRange(consensusAccounts, i + Address.LENGTH, i + Address.LENGTH + Sha256Hash.LENGTH);
			add(accountStore.getHash160(), Sha256Hash.wrap(txhash), accountStore.getPubkeys());
		}
		
		log.info("加载已有的{}个共识", container.size());
	}
	
	/**
	 * 新增共识结点
	 * @param hash160
	 */
	public void add(byte[] hash160, Sha256Hash txhash, byte[][] pubkey) {
		if(!verifyOne(hash160, pubkey)) {
			log.warn("公钥不匹配的共识");
			return;
		}
		if(contains(hash160)) {
			log.warn("重复的共识节点");
			return;
		}
		container.put(hash160, pubkey);
		txContainer.put(hash160, txhash);
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
		Iterator<Entry<byte[], byte[][]>> it = container.entrySet().iterator();
		while(it.hasNext()) {
			Entry<byte[], byte[][]> entry = it.next();
			byte[] key = entry.getKey();
			if(Arrays.equals(hash160, key)) {
				txContainer.remove(key);
				it.remove();
			}
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

	/**
	 * 获取注册共识时的交易hash
	 */
	@Override
	public Sha256Hash getTx(byte[] hash160) {
		for (Entry<byte[], Sha256Hash> entry : txContainer.entrySet()) {
			if(Arrays.equals(hash160, entry.getKey())) {
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
		
		return list;
	}

	@Override
	public int getCurrentConsensus() {
		return container.size();
	}
}
