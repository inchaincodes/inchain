package org.inchain.wallet.controllers;

import org.codehaus.jettison.json.JSONException;
import org.inchain.SpringContextUtils;
import org.inchain.listener.Listener;
import org.inchain.listener.VersionUpdateListener;
import org.inchain.service.impl.VersionService;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
/**
 * 更新
 * @author cj
 *
 */
public class UpdateController extends DailogController{

	public Button closeId; //关闭
	public ProgressBar progressBarId;	//进度条
	public Label progressId;			//进度
	public Label hintId;				//提示语
	public Label tip;					//下载对象
	
	public VersionUpdateListener versionUpdateListener;
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	
    	versionUpdateListener = new VersionUpdateListener() {
    		
    		public void startDownload() {
    			tip.setText("开始下载");
    		}
			public void onComplete() {
				hintId.setVisible(true);
				closeId.setVisible(true);
			}
			public void downloading(String filename, float rate) {
				Platform.runLater(new Runnable() {
					public void run() {
						tip.setText("正在下载"+filename);
						progressBarId.setProgress(rate);
						progressId.setText(rate*100+"%");
					}
				});
			}
		};
    	hintId.setStyle("-fx-text-fill:RED;");
    	
    	closeId.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				Platform.exit();
			}
		});
    	
    	new Thread() {
    		public void run() {
		    	VersionService versionService = SpringContextUtils.getBean(VersionService.class);
		    	try {
					versionService.update(versionUpdateListener);
				} catch (JSONException e) {
					e.printStackTrace();
				}
		    }
		}.start();;
    }
}
