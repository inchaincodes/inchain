package org.inchain.wallet.controllers;

import java.net.URL;
import java.util.Date;

import javax.annotation.PreDestroy;

import org.codehaus.jettison.json.JSONObject;
import org.inchain.SpringContextUtils;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.consensus.MiningInfos;
import org.inchain.core.Coin;
import org.inchain.core.DataSynchronizeHandler;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.service.impl.VersionService;
import org.inchain.utils.ConsensusCalculationUtil;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * 系统信息
 * @author cj
 *
 */

public class SystemInfoController implements SubPageController{

	private static final Logger log = LoggerFactory.getLogger(SystemInfoController.class); 
	
	public Label version;	//版本
	public Label network;	//所在网络
	public Label networkHeight;	//网络最新高度
	public Label localHeight;	//本地最新高度
	public Label networkNumber; 	//网络连接数
	public Label networkTime;	//网络时间
	public Label localTime;	//本地时间
	public Label offsetTime;	//时间偏移
	public Label consensusNodeNumber;	//共识节点数
	public Label consensusBonusNumber;	//当前共识奖励数
	public Label consensusStatus;	//共识状态
	public Label totalAmount;	//代币总量
	public Label rewardTotalAmount;	//共识奖励总量
	public Label rewardAmount;		//已产出奖励数
	public Label sortId;
	
	public Button updateId;			//更新
	
	private boolean runing;
	
	private AppKit appKit;
	private AccountKit accountKit;
	
	/**
	 * FXML初始化调用
	 * @return 
	 */
	public void initialize() {
		initListeners();
		Thread updateTh = new Thread() {
			@Override
			public void run() {
				 startVersionCheck();
			}
		};
		updateTh.setName("updateVersion service");
		updateTh.start();
	}
	@PreDestroy
	public void stop() {
		runing = false;
	}

	protected void startVersionCheck() {
		runing = true;
		
		VersionService versionService = SpringContextUtils.getBean(VersionService.class);
		
		while(runing) {
			try {
				Thread.sleep(10000l);
				//检查是否有更新
				JSONObject result = versionService.check();
				
				if(result.getBoolean("success") && result.getBoolean("newVersion")) {
					updateId.setVisible(true);
				}
				Thread.sleep(50000l);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void initDatas() {
		startShowTime();
	}
	/*
	 * 初始化数据变化监听器
	 */
	private void initListeners() {
		
    	InchainInstance instance = InchainInstance.getInstance();
    	appKit = instance.getAppKit();
    	accountKit = instance.getAccountKit();
    	
    	consensusStatus.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				Integer status = (Integer) consensusStatus.getUserData();
				if(status == null || status.intValue() == 0) {
					return;
				}
				if(status.intValue() == 1) {
					//解密账户
	    			URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
			        FXMLLoader loader = new FXMLLoader(location);
			        final AccountKit accountKitTemp = accountKit;
					DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
						@Override
						public void ok(Object param) {
							if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
								try {
									ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
									consensusMeeting.waitMining();
								} finally {
									accountKitTemp.resetKeys();
								}
							}
						}
					});
				}
			}
		});
    	//更新按钮
    	updateId.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				
				//弹出更新弹窗
				URL location = getClass().getResource("/resources/template/updateVersion.fxml");
				FXMLLoader loader = new FXMLLoader(location);
				DailogUtil.showDailog(loader, "更新版本");
			}
		});
	}

	/*
	 * 开始显示时间，这里显示的是网络时间，并不是本地时间
	 */
	private void startShowTime() {
			try {
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
				    	
				    	version.setText(Definition.LIBRARY_SUBVER);
				    	
				    	networkTime.setText(DateUtil.convertDate(new Date(TimeService.currentTimeMillis())));
				    	localTime.setText(DateUtil.convertDate(new Date()));
				    	if(TimeService.getNetTimeOffset() > 0) {
				    		offsetTime.setText("落后"+Math.abs(TimeService.getNetTimeOffset())+"毫秒");
				    	} else if(TimeService.getNetTimeOffset() < 0) {
				    		offsetTime.setText("快"+Math.abs(TimeService.getNetTimeOffset())+"毫秒");
				    	} else {
				    		offsetTime.setText(""+"0毫秒");
				    	}
				    	networkHeight.setText(String.valueOf(appKit.getNetwork().getBestHeight()));
				    	
				    	localHeight.setText(String.valueOf(appKit.getNetwork().getBestBlockHeight()));
				    	sortId.setText(String.valueOf(appKit.getNetwork().getPort()));
				    	if(appKit.getNetwork().getLocalServices() == 1) {
				    		network.setText("主网络");
				    	} else if(appKit.getNetwork().getLocalServices() ==2) {
				    		network.setText("测试网络");
				    	}
						if(accountKit.checkConsensusing(null)) {
				    		ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
				    		DataSynchronizeHandler dataSynchronizeHandler = SpringContextUtils.getBean(DataSynchronizeHandler.class);
				    		if(dataSynchronizeHandler.hasComplete()) {
				    			consensusMeeting.waitMeeting();
				    			MiningInfos miningInfo = consensusMeeting.getMineMiningInfos();
				    			String periodStartTime = DateUtil.convertDate(new Date(miningInfo.getPeriodStartTime()*1000), "HH:mm:ss");
				    			String beginTime = DateUtil.convertDate(new Date(miningInfo.getBeginTime()*1000), "HH:mm:ss");
				    			String endTime = DateUtil.convertDate(new Date(miningInfo.getEndTime()*1000), "HH:mm:ss");
				    			
				    			consensusStatus.setUserData(0);
				    			if(miningInfo.getBeginTime() == 0l) {
				    				if(consensusMeeting.getAccount() == null) {
				    					consensusStatus.setText("点我输入密码进行共识");
				    					consensusStatus.setUserData(1);
				    				} else {
				    					consensusStatus.setText("正在等待当前轮结束：预计" + (miningInfo.getPeriodEndTime() - TimeService.currentTimeSeconds()) + "秒");
				    				}
				    			} else if(TimeService.currentTimeSeconds() < miningInfo.getBeginTime()) {
				    				consensusStatus.setText("正在排队共识,预计" + (miningInfo.getBeginTime() - TimeService.currentTimeSeconds()) + "秒\n当前轮开始时间：" + periodStartTime + "\n我的共识时间：" + beginTime + " - " + endTime);
				    			} else if(TimeService.currentTimeSeconds() > miningInfo.getEndTime()) {
				    				consensusStatus.setText("正在等待进入下一轮共识队列：预计" + (miningInfo.getPeriodEndTime() - TimeService.currentTimeSeconds()) + "秒");
				    			} else {
				    				consensusStatus.setText("正在打包中: 倒计时 " + (miningInfo.getEndTime() - TimeService.currentTimeSeconds()) + "秒");
				    			}
				    		} else {
				    			consensusStatus.setText("区块同步中···");
				    		}
				    	} else {
				    		consensusStatus.setText("未参与共识");
				    	}
						ConsensusPool consensusPoll = SpringContextUtils.getBean(ConsensusPool.class);
				    	consensusNodeNumber.setText(String.valueOf(consensusPoll.getCurrentConsensus()));
				    	consensusBonusNumber.setText(ConsensusCalculationUtil.calculatReward(appKit.getNetwork().getBestHeight()).toText() + " INS");
				    	
				    	totalAmount.setText(Coin.MAX.toText() + " INS");
				    	rewardTotalAmount.setText(ConsensusCalculationUtil.TOTAL_REWARD.toText() + " INS");
				    	rewardAmount.setText(ConsensusCalculationUtil.calculatTotal(appKit.getNetwork().getBestHeight()).toText() + " INS");
				    	
//				    	networkNumber.setText(String.valueOf(peerKit.getAvailablePeersCount()));
				    }
				});
			} catch (Exception e) {
				log.error("", e);
			}
	}
	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
		
	}

	@Override
	public boolean refreshData() {
		return true;
	}

	@Override
	public boolean startupInit() {
		return false;
	}
}
