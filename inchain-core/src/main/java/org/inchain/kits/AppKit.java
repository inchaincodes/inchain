package org.inchain.kits;

import java.io.IOException;

import org.inchain.Configure;
import org.inchain.consensus.Mining;
import org.inchain.core.exception.VerificationException;
import org.inchain.listener.BlockChangedListener;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.listener.Listener;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 程序核心，所有服务在此启动
 * @author ln
 *
 */
@Service
public class AppKit {
	
	private static final Logger log = LoggerFactory.getLogger(AppKit.class);
	
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	@Autowired
	private NetworkParams network;
	
	//初始化监听器
	private Listener initListener;
	
	//挖矿程序
	@Autowired
	private Mining mining;
	//节点管理
	@Autowired
	private PeerKit peerKit;
	//帐户管理 
	@Autowired
	private AccountKit accountKit;
	
	public AppKit() {
		
	}

	//异步启动
	public void startSyn() {
		new Thread(){
			public void run() {
				try {
					AppKit.this.start();
				} catch (IOException e) {
					log.error("核心程序启动失败", e);
					System.exit(-1);
				}
			}
		}.start();
	}

	/**
	 * 启动核心
	 * @throws IOException 
	 */
	protected void start() throws IOException {
		//初始化节点管理器
		initPeerKit();
		//检查区块数据
		initBlock();
		//初始化帐户信息
		initAccountKit();
		//初始化挖矿
		initMining();
		
		addShutdownListener();
		
		if(initListener != null) {
			initListener.onComplete();
		}
		//初始化一些数据变化监听器
		initDataChangeListener();
	}
	
	//初始化数据变化监听器
	private void initDataChangeListener() {
		//如果设置了区块变化监听器，那么首先通知一次本地的高度
		if(peerKit.getBlockChangedListener() != null) {
			BlockHeaderStore blockHeader = network.getBestBlockHeader();
			peerKit.getBlockChangedListener().onChanged(blockHeader.getBlockHeader().getHeight(), -1, blockHeader.getBlockHeader().getHash(), null);
		}
	}

	//系统关闭钩子，确保能正确关闭
	private void addShutdownListener() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	        @Override  
	        public void run() {
	        	try {
	        		stop();
	        	} catch (Exception e) {
	        		e.printStackTrace();
				}
	        }  
	    }));
	}

	//初始化节点管理器
	private void initPeerKit() {
		peerKit.startSyn();
	}

	//初始化帐户信息
	private void initAccountKit() throws IOException {
		accountKit.init();
	}

	//初始化挖矿
	private void initMining() {
		if(Configure.MINING) {
			new Thread() {
				public void run() {
					mining.start();
				};
			}.start();
		}
	}

	/**
	 * 停止服务
	 * @throws IOException 
	 */
	public void stop() throws IOException {
		peerKit.stop();
		mining.stop();
		
		blockStoreProvider.close();
		accountKit.close();
	}

	/*
	 * 初始化区块信息
	 */
	private void initBlock() throws IOException {
		
		checkGenesisBlock();
		
		checkPoint();
	}

	/*
	 * 效验区块链的正确性和完整性
	 */
	private void checkPoint() {
		//TODO
		
	}

	/*
	 * 检查创世块
	 * 如果创世块不存在，则新增，然后下载区块
	 * 如果存在，则检查是否正确，不正确则直接抛出异常
	 */
	private void checkGenesisBlock() throws IOException {
		BlockStore gengsisBlock = network.getGengsisBlock();
		
		BlockHeaderStore localGengsisBlockHeader = blockStoreProvider.getHeader(gengsisBlock.getBlock().getHash().getBytes());
		
		//存在，判断区块信息是否正确
		if(localGengsisBlockHeader != null && !localGengsisBlockHeader.getBlockHeader().equals(gengsisBlock.getBlock())) {
			throw new VerificationException("the genesis block check error!");
		} else if(localGengsisBlockHeader == null) {
			//新增
			blockStoreProvider.saveBlock(gengsisBlock);
		}
	}
	
	public AccountKit getAccountKit() {
		return accountKit;
	}
	public NetworkParams getNetwork() {
		return network;
	}
	
	public void setInitListener(Listener initListener) {
		this.initListener = initListener;
	}
	
	public void addBlockChangedListener(BlockChangedListener blockChangedListener) {
		peerKit.setBlockChangedListener(blockChangedListener);
	}

	public void addConnectionChangedListener(ConnectionChangedListener connectionChangedListener) {
		peerKit.addConnectionChangedListener(connectionChangedListener);
	}
}
