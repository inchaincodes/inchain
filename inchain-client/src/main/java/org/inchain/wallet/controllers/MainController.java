package org.inchain.wallet.controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.inchain.core.Peer;
import org.inchain.core.TimeHelper;
import org.inchain.crypto.Sha256Hash;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AppKit;
import org.inchain.listener.BlockChangedListener;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.listener.StartupListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

public class MainController {
	
	private static final Logger log = LoggerFactory.getLogger(MainController.class);
	
	public Label nowNetTimeId;					//当前网络时间
	public Label localNewestHeightId;			//本地最新高度
	public Label netNewestHeightId;				//网络最新高度
	public Label blockHeightSeparator;			//本地最新高度与网络最新高度分隔符
	public Label networkInfosTipId;				//网络连接节点信息提示
	public Label networkInfosNumId;				//网络连接节点信息

	//导航按钮
	public Button accountInfoId;			//账户信息
	public Button sendAmountId;				//转账
	public Button transactionRecordId;		//交易记录
	public Button consensusRecordId;		//共识节点列表
	public Button sellerRecordId;			//商家列表
	
	public StackPane contentId;				//内容
	
	private Map<String, Node> pageMaps = new HashMap<>();	//页面列表
	
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	Tooltip localTooltip = new Tooltip("本地最新高度");
    	localTooltip.setFont(Font.font("宋体", 14));
    	localNewestHeightId.setTooltip(localTooltip);

    	Tooltip netTooltip = new Tooltip("网络最新高度");
    	netTooltip.setFont(Font.font("宋体", 14));
    	netNewestHeightId.setTooltip(netTooltip);
    	
    	Tooltip networkInfosTooltip = new Tooltip("主动连接：0\r\n被动连接：0");
    	networkInfosTooltip.setFont(Font.font("宋体", 14));
    	networkInfosTipId.setTooltip(networkInfosTooltip);
    	networkInfosNumId.setTooltip(networkInfosTooltip);
    	
    	EventHandler<ActionEvent> buttonEventHandler = new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Button button = (Button) event.getSource();
				if(button == null) {
					return;
				}
				
				String id = button.getId();
				
				showPage(id);
			}
		};
    	
    	accountInfoId.setOnAction(buttonEventHandler);
    	sendAmountId.setOnAction(buttonEventHandler);
    	transactionRecordId.setOnAction(buttonEventHandler);
    	consensusRecordId.setOnAction(buttonEventHandler);
    	sellerRecordId.setOnAction(buttonEventHandler);
    }

    /**
     * 核心启动完成之后调用的初始化
     * @param startupListener 
     */
	public void init(StartupListener startupListener) {
		//界面时间
    	startShowTime();
    	
    	//初始化监听器
    	initListeners();
    	
    	showPage(accountInfoId.getId());
    	
    	//加载完成
    	startupListener.onComplete();
	}

	/*
	 * 初始化数据变化监听器
	 */
	private void initListeners() {
		//注入区块变化监听器
    	InchainInstance instance = InchainInstance.getInstance();
    	AppKit appKit = instance.getAppKit();
    	appKit.addBlockChangedListener(new BlockChangedListener() {
			@Override
			public void onChanged(long localNewestHeight, long netNewestHeight, Sha256Hash localNewestHash,
					Sha256Hash netNewestHash) {
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
				    	if(localNewestHeight != -1l) {
					    	localNewestHeightId.setText(String.valueOf(localNewestHeight));
					    	localNewestHeightId.getTooltip().setText(String.format("本地最新高度 %d", localNewestHeight));
				    	}
				    	if(netNewestHeight != -1l) {
				    		if(!blockHeightSeparator.isVisible()) {
				    			blockHeightSeparator.setVisible(true);
				    			netNewestHeightId.setVisible(true);
				    		}
					    	netNewestHeightId.setText(String.valueOf(netNewestHeight));
					    	netNewestHeightId.getTooltip().setText(String.format("网络最新高度 %d", netNewestHeight));
				    	}
				    }
				});
			}
		});
    	
    	//网络变化监听器
    	appKit.addConnectionChangedListener(new ConnectionChangedListener() {
			@Override
			public void onChanged(int inCount, int outCount, CopyOnWriteArrayList<Peer> inPeers,
					CopyOnWriteArrayList<Peer> outPeers) {
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
						networkInfosNumId.setText(String.valueOf(inCount + outCount));
						networkInfosNumId.getTooltip().setText(String.format("主动连接：%d\r\n被动连接：%d", outCount, inCount));
				    }
				});
			}
		});
	}
	
	/**
	 * 显示子页面
	 * @param id
	 */
	private void showPage(String id) {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
				Node page = pageMaps.get(id);
				
				if(page == null) {
					page = getPage(id);
					if(page == null) {
						return;
					}
					pageMaps.put(id, page);
				}
				contentId.getChildren().clear();
				contentId.getChildren().add(page);
		    }
		});
	}

    /**
     * 根据ID获取中间内容页面* 
     * @param id
     * @return
     */
	private Node getPage(String id) {
		
		String fxml = null;
		
		switch (id) {
		case "accountInfoId":
			//点击账户信息按钮
			fxml = "/resources/template/accountInfo.fxml";
			break;
		case "sendAmountId":
			//点击转账按钮
			fxml = "/resources/template/sendAmount.fxml";
			break;
		case "transactionRecordId":
			//点击交易记录按钮
			
			break;
		case "consensusRecordId":
			//点击共识列表按钮
			
			break;
		case "sellerRecordId":
			//点击商家列表按钮
			
			break;
		default:
			break;
		}
		
		if(fxml != null) {
			try {
				Pane page = FXMLLoader.load(getClass().getResource(fxml));
				return page;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/*
	 * 开始显示时间，这里显示的是网络时间，并不是本地时间
	 */
	private void startShowTime() {
		new Thread(){
    		public void run() {
    			while(true) {
    				try {
    					Platform.runLater(new Runnable() {
    					    @Override
    					    public void run() {
    					    	nowNetTimeId.setText(DateUtil.convertDate(new Date(TimeHelper.currentTimeMillis())));
    					    }
    					});
    					Thread.sleep(1000l);
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    			}
    		};
    	}.start();
	}

}
