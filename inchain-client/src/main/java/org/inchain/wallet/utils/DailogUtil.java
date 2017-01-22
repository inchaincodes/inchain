package org.inchain.wallet.utils;

import org.inchain.wallet.Context;

import javafx.application.Platform;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;

/**
 * 弹出框工具类
 * @author ln
 *
 */
public class DailogUtil {

	public static void showTip(String message) {
		Tooltip tip = new Tooltip(message);
		tip.setFont(Font.font("宋体", 14));
		tip.show(Context.stage);
		
		hideDailog(tip);
	}
	
	public static void showTip(String message, double x, double y) {
		Tooltip tip = new Tooltip(message);
		tip.setFont(Font.font("宋体", 14));
		tip.show(Context.stage, x, y);
		
		hideDailog(tip);
	}

	private static void hideDailog(Tooltip tip) {
		new Thread(){
    		public void run() {
    			try {
					Thread.sleep(1000l);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    			close();
    		}
			private void close() {
				//1秒消失
    			Platform.runLater(new Runnable() {
    			    @Override
    			    public void run() {
						tip.hide();
    			    }
    			});
			};
    	}.start();
	}
}
