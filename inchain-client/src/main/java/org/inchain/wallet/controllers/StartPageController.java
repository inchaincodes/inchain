package org.inchain.wallet.controllers;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.inchain.Configure;
import org.inchain.kit.InchainInstance;
import org.inchain.listener.Listener;
import org.inchain.wallet.listener.StartupListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * 启动页面控制器
 * @author ln
 *
 */
public class StartPageController {
	
	private static final Logger log = LoggerFactory.getLogger(StartPageController.class);
	
	//任务调度器
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private int kitStartBarCount;
	
	private Listener listener;
	//核心启动完成监听器
	private Listener appKitInitListener;
	private StartupListener startupListener;
	
	//控制器
	private MainController mainController;
	
	public Label speedLabel;
	public Label tipLabel;
	public ProgressBar progressBar;
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	//正在启动程序核心
		executor.scheduleWithFixedDelay(new Thread(){
    		public void run() {
				kitStartBarCount++;
    			Platform.runLater(new Runnable() {
					@Override
					public void run() {
						progressBar.setProgress(kitStartBarCount/800d);
					}
				});
    		};
    	}, 0, 10, TimeUnit.MILLISECONDS);
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
		instance.startup(Configure.RUN_MODE, appKitInitListener);
    }
    
    /*
     * 初始化数据
     */
    protected void initDatas() {
    	executor.shutdownNow();
    	
    	startupListener = new StartupListener() {
    		private int completionRate;
    		
			@Override
			public void onComplete() {
	    		listener.onComplete();
			}
			
			@Override
			public void onChange(final String tip) {
				try {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							tipLabel.setText(tip);
							//使用 this.getCompletionRate() 来获取进度
							progressBar.setProgress((float)(getCompletionRate()/100.0));
							speedLabel.setText(getCompletionRate()+"%");
							//progressBar.setStyle("-fx-accent:rgb("+getCompletionRate()/10+",125,125)");
						}
					});
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			@Override
			public int getCompletionRate() {
				return completionRate;
			}

			@Override
			public void setCompletionRate(int completionRate) {
				this.completionRate = completionRate;
			}
		};
		startupListener.setCompletionRate(30);
		startupListener.onChange("核心启动成功···");
    	mainController.init(startupListener);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
    
    public void setMainController(MainController mainController) {
		this.mainController = mainController;
	}

}
