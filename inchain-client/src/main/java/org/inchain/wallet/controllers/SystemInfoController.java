package org.inchain.wallet.controllers;

import java.util.Date;

import org.inchain.SpringContextUtils;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.MiningInfos;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.utils.ConsensusRewardCalculationUtil;
import org.inchain.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
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
	public Label sortId;
	
	private AppKit appKit;
	private AccountKit accountKit;
	private PeerKit peerKit;
	
	/**
	 * FXML初始化调用
	 * @return 
	 */
	public void initialize() {
		initListeners();
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
    	peerKit = SpringContextUtils.getBean(PeerKit.class);
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
				    	if(accountKit.checkConsensusing()) {
				    		ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
				    		consensusMeeting.waitMeeting();
				    		MiningInfos miningInfo = consensusMeeting.getMineMiningInfos();
				    		String periodStartTime = DateUtil.convertDate(new Date(miningInfo.getPeriodStartTime()*1000));
				    		String beginTime = DateUtil.convertDate(new Date(miningInfo.getBeginTime()*1000));
				    		String endTime = DateUtil.convertDate(new Date(miningInfo.getEndTime()*1000));
				    		if(miningInfo.getPeriodStartTime() > miningInfo.getBeginTime()) {
				    			
				    			consensusStatus.setText("正在等待进入共识队列");
				    		}else if(endTime.compareTo(DateUtil.convertDate(new Date())) <= 0) {
				    			consensusStatus.setText("正在等待进入下一轮共识队列");
				    		}else {
				    			consensusStatus.setText("正在排队\n当前轮开始时间："+periodStartTime+"\n我的共识开始时间："+beginTime+"\n我的共识结束时间："+endTime);
				    		}
				    		
				    	} else {
				    		consensusStatus.setText("未参与共识");
				    	}
				    	consensusNodeNumber.setText(String.valueOf(accountKit.getConsensusAccounts().size()));
				    	consensusBonusNumber.setText(ConsensusRewardCalculationUtil.calculat(appKit.getNetwork().getBestHeight()).toText() +" INS");
				    	
				    	networkNumber.setText(String.valueOf(peerKit.getAvailablePeersCount()));
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
