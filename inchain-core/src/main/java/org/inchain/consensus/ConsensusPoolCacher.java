package org.inchain.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
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

	private CopyOnWriteArrayList<ConsensusModel> consensusModels = new CopyOnWriteArrayList<ConsensusModel>();

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
		for (int i = 0; i < consensusAccounts.length; i += (2 * Address.LENGTH + Sha256Hash.LENGTH)) {
			byte[] applicant = Arrays.copyOfRange(consensusAccounts, i, i + Address.LENGTH);
			byte[] packager = Arrays.copyOfRange(consensusAccounts, i + Address.LENGTH, i + 2 * Address.LENGTH);
			byte[] txhash = Arrays.copyOfRange(consensusAccounts, i + 2 * Address.LENGTH, i + 2 * Address.LENGTH + Sha256Hash.LENGTH);
			add(new ConsensusModel(Sha256Hash.wrap(txhash), applicant, packager));
		}
		
		log.info("加载已有的{}个共识", consensusModels.size());
	}

	/**
	 * 新增共识结点
	 * @param consensusModel
	 */
	public void add(ConsensusModel consensusModel) {
		for(ConsensusModel consensusModelTemp: consensusModels) {
			if(Arrays.equals(consensusModelTemp.getApplicant(), consensusModel.getApplicant()) && Arrays.equals(consensusModelTemp.getPackager(), consensusModel.getPackager())) {
				return;
			}
		}
		consensusModels.add(consensusModel);
	}

	/**
	 * 移除共识节点
	 */
	public void delete(byte[] hash160) {
		for(ConsensusModel consensusModelTemp: consensusModels) {
			if(Arrays.equals(consensusModelTemp.getApplicant(), hash160) || Arrays.equals(consensusModelTemp.getPackager(), hash160)) {
				consensusModels.remove(consensusModelTemp);
			}
		}
	}
	
	/**
	 * 判断是否是共识打包节点
	 * @param hash160
	 * @return boolean
	 */
	public boolean isPackager(byte[] hash160) {
		for(ConsensusModel consensusModelTemp: consensusModels) {
			if(Arrays.equals(consensusModelTemp.getPackager(), hash160)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断是否是共识节点
	 * @param hash160
	 * @return boolean
	 */
	public boolean contains(byte[] hash160) {
		for(ConsensusModel consensusModelTemp: consensusModels) {
			if(Arrays.equals(consensusModelTemp.getApplicant(), hash160) || Arrays.equals(consensusModelTemp.getPackager(), hash160)) {
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
		return null;
	}

	/**
	 * 获取注册共识时的交易hash
	 */
	@Override
	public Sha256Hash getTx(byte[] hash160) {
		for(ConsensusModel consensusModelTemp: consensusModels) {
			if(Arrays.equals(consensusModelTemp.getApplicant(), hash160) || Arrays.equals(consensusModelTemp.getPackager(), hash160)) {
				return consensusModelTemp.getTxid();
			}
		}
		return null;
	}
	
	public List<ConsensusModel> getContainer() {
		return consensusModels;
	}
	
	/**
	 * 当前共识节点列表快照
	 * @return List<ConsensusAccount>
	 */
	@Override
	public List<ConsensusAccount> listSnapshots() {
		//TODO处理并发情况
		
		List<ConsensusAccount> list = new ArrayList<ConsensusAccount>();

		for(ConsensusModel consensusModel: consensusModels) {
			list.add(new ConsensusAccount(consensusModel.getPackager(), consensusModel.getApplicant()));
		}
		
		return list;
	}

	@Override
	public int getCurrentConsensus() {
		return consensusModels.size();
	}

	@Override
	public void clearAll() {
		chainstateStoreProvider.delete(Configure.CONSENSUS_ACCOUNT_KEYS);
		consensusModels.clear();
	}
}
