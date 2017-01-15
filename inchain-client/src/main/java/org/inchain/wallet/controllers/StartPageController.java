package org.inchain.wallet.controllers;

import org.inchain.kit.InchainInstance;
import org.inchain.listener.Listener;
import org.inchain.wallet.listener.StartupListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Label;

public class StartPageController {
	
	private static final Logger log = LoggerFactory.getLogger(StartPageController.class);
	
	private Listener listener;
	//核心启动完成监听器
	private Listener appKitInitListener;
	private StartupListener startupListener;
	
	//控制器
	private MainController mainController;
	
	private Label tipLabel;
	
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	log.info("==========================");
    	
    	//TODO
    	
    	//正在启动程序核心
    	
    }
    
    public void init() {
    	//核心启动完成监听器
		appKitInitListener = new Listener() {
			@Override
			public void onComplete() {
				if(listener != null) {
					initDatas();
		    	}
			}
		};
		
    	InchainInstance instance = InchainInstance.getInstance();
		instance.startup(2, appKitInitListener);
    }
    
    protected void initDatas() {
    	try {
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	startupListener = new StartupListener() {
			@Override
			public void onComplete() {
	    		listener.onComplete();
			}
			
			@Override
			public void onChange(String tip, float completionRate) {
				//TODO
				tipLabel.setText(tip);
				
			}
		};
		
    	mainController.init(startupListener);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
    
    public void setMainController(MainController mainController) {
		this.mainController = mainController;
	}

}
