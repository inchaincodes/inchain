package org.inchain.wallet.utils;

import java.net.URL;
import org.inchain.wallet.Constant;
import org.inchain.wallet.Context;
import org.inchain.wallet.controllers.DailogDecorationController;
import org.inchain.wallet.entity.Point;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 弹出框工具类
 * @author ln
 *
 */
public class DailogUtil {
	
	public final static long DEFAULT_HIDE_TIME = 1000l;
	public static DailogDecorationController dailogDecorationController;
	
	/**
	 * 提示消息
	 * @param message
	 */
	public static void showTip(String message) {
    	showTip(message, DEFAULT_HIDE_TIME);
	}
	
	/**
	 * 提示消息
	 * @param message
	 */
	public static void showTip(String message, Stage stage) {
		showTip(message, stage, DEFAULT_HIDE_TIME);
	}
	
	/**
	 * 提示消息，延时自动消失
	 * @param message
	 * @param hideTime
	 */
	public static void showTip(String message, long hideTime) {
    	showTip(message, Context.getMainStage(), hideTime);
	}
	
	/**
	 * 提示消息，延时自动消失
	 * @param message
	 * @param hideTime
	 */
	public static void showTip(String message, Stage stage, long hideTime) {
		Point point = getDailogPoint(100, 30);
		showTip(message, stage, hideTime, point.getX(), point.getY());
	}
	
	/**
	 * 在制定位置提示消息
	 * @param message
	 * @param x
	 * @param y
	 */
	public static void showTip(String message, double x, double y) {
		showTip(message, Context.getMainStage(), DEFAULT_HIDE_TIME, x, y);
	}
	
	/**
	 * 自定义的提示消息
	 * @param message
	 * @param hideTime
	 * @param x
	 * @param y
	 */
	public static void showTip(String message, Stage stage, long hideTime, double x, double y) {
		Tooltip tip = new Tooltip(message);
		tip.setFont(Font.font("宋体", 14));
		tip.show(stage, x, y);
		
		hideDailog(tip, hideTime);
	}

	/**
	 * 隐藏提示消息框
	 * @param tip
	 * @param hideTime
	 */
	public static void hideDailog(final Tooltip tip, final long hideTime) {
		new Thread(){
    		public void run() {
    			try {
					Thread.sleep(hideTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    			close();
    		}
			private void close() {
				//延时自动消失
    			Platform.runLater(new Runnable() {
    			    @Override
    			    public void run() {
						tip.hide();
    			    }
    			});
			};
    	}.start();
	}
	
	/**
	 * 获取弹出层的剧中位置
	 * @param dailogWidth
	 * @param dailogHeight
	 * @return Point
	 */
	public static Point getDailogPoint(double dailogWidth, double dailogHeight) {
		Stage mainStage = Context.getMainStage();
		double x = mainStage.getX() + (mainStage.getWidth() - dailogWidth) / 2;
    	double y = mainStage.getY() + (mainStage.getHeight() - dailogHeight) / 2;
    	return new Point(x, y);
	}
	
	/**
	 * 显示弹出层
	 * @param ui
	 * @param title
	 */
	public static void showDailog(FXMLLoader loader, String title) {
		showDailog(loader, title, null);
	}
	
	/**
	 * 显示弹出层
	 * @param content
	 * @param title
	 * @param callback 关闭时的回调
	 */
	public static void showDailog(FXMLLoader content, String title, final Runnable callback) {
		try {
			URL url = DailogUtil.class.getClass().getResource("/resources/template/dailogDecoration.fxml");
			FXMLLoader loader =  new FXMLLoader(url);
			Pane ui = loader.load();
			dailogDecorationController = loader.getController();
			Pane dailogContent = content.load();
			
			dailogDecorationController.getDailogContent().getChildren().add(dailogContent);
			
			Stage window = new Stage(StageStyle.TRANSPARENT);
			dailogDecorationController.setStage(window);
			dailogDecorationController.setTitle(title);
			Point point = getDailogPoint(ui.getPrefWidth(), ui.getPrefHeight());
			window.setX(point.getX());
			window.setY(point.getY());
			//设置程序标题
			window.setTitle(title);
			//设置程序图标
			window.getIcons().add(new Image(DailogUtil.class.getClass().getResourceAsStream(Constant.APP_ICON)));
			Scene scene = new Scene(ui);
			window.setScene(scene);
			scene.getStylesheets().add("/resources/css/dailogDecoration.css");
			if(ui.getId() != null) {
				Context.addStage(ui.getId(), window);
			}
			dailogDecorationController.setCallback(callback);
			dailogDecorationController.setPageId(ui.getId());
			
			window.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
