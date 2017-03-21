package org.inchain.wallet.utils;

import java.net.URL;

import org.inchain.wallet.Constant;
import org.inchain.wallet.Context;
import org.inchain.wallet.controllers.DailogController;
import org.inchain.wallet.controllers.DailogDecorationController;
import org.inchain.wallet.entity.Point;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 弹出框工具类
 * @author ln
 *
 */
public class DailogUtil {
	
	public final static long DEFAULT_HIDE_TIME = 1000l;
	
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
		Point point = getDailogPoint(100, 300);
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
	 * 弹出框中间位置提示消息
	 * */
	public static void showTipDailogCenter(String message, Stage stage) {
		showTip(message,stage,DEFAULT_HIDE_TIME,stage.getX()+stage.getWidth()/3,stage.getY()+stage.getHeight()/3);
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
		double x = mainStage.getX() + 60 + (mainStage.getWidth() - dailogWidth) / 2;
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
	 * @param ui
	 * @param title
	 */
	public static void showDailog(FXMLLoader loader, String title,final Runnable callback) {
		showDailog(loader, title,460,280, callback);
	}
	/**
	 * 显示弹出层
	 * @param ui
	 * @param title
	 */
	public static void showDailog(FXMLLoader loader, String title,double width,double height) {
		showDailog(loader, title,width,height, null);
	}
	/**
	 * 显示弹出层
	 * @param content
	 * @param title
	 * @param callback 关闭时的回调
	 */
	public static void showDailog(FXMLLoader content, String title,double width,double height,final Runnable callback) {
		try {
			URL url = DailogUtil.class.getClass().getResource("/resources/template/dailogDecoration.fxml");
			FXMLLoader loader =  new FXMLLoader(url);
			Pane ui = loader.load();
			DailogDecorationController dailogDecorationController = loader.getController();
			Pane dailogContent = content.load();
			ui.setPrefWidth(width);
			ui.setPrefHeight(height);
			dailogDecorationController.getDailogContent().getChildren().add(dailogContent);
			Group root = addShadow(ui);
			
			Stage window = new Stage(StageStyle.TRANSPARENT);
			dailogDecorationController.setStage(window);
			dailogDecorationController.setTitle(title);
			Point point = getDailogPoint(ui.getPrefWidth(), ui.getPrefHeight());
			window.setX(point.getX());
			window.setY(point.getY());
			//设置程序标题
			window.setTitle(title);
			window.initModality(Modality.APPLICATION_MODAL);
			//设置程序图标
			window.getIcons().add(new Image(DailogUtil.class.getClass().getResourceAsStream(Constant.APP_ICON)));
			Scene scene = new Scene(root);
			scene.fillProperty().set(Color.TRANSPARENT);
			window.setScene(scene);
			scene.getStylesheets().add("/resources/css/dailogDecoration.css");
			if(dailogContent.getId() != null) {
				Context.addStage(dailogContent.getId(), window);
			}
			
			DailogController controller = content.getController();
			
			controller.setCallback(callback);
			controller.setPageId(dailogContent.getId());
			
			window.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Group addShadow(Pane ui) {
		//添加阴影
		Group root = new Group();
		DropShadow dropShadow = new DropShadow();
		dropShadow.setBlurType(BlurType.GAUSSIAN);
		dropShadow.setSpread(0.2);
		dropShadow.setRadius(15);
		root.setEffect(dropShadow);
		
		root.getChildren().add(ui);
		return root;
	}
	/**
	 * 显示弹出层
	 * @param content
	 * @param title
	 */
	public static void showDailog(Node node, String title) {
		try {
			URL url = DailogUtil.class.getClass().getResource("/resources/template/dailogDecoration.fxml");
			FXMLLoader loader =  new FXMLLoader(url);
			Pane ui = loader.load();
			DailogDecorationController dailogDecorationController = loader.getController();
			dailogDecorationController.getDailogContent().getChildren().add(node);
			
			Group root = addShadow(ui);
			Stage window = new Stage(StageStyle.TRANSPARENT);
			dailogDecorationController.setStage(window);
			dailogDecorationController.setTitle(title);
			Point point = getDailogPoint(ui.getPrefWidth(), ui.getPrefHeight());
			window.setX(point.getX());
			window.setY(point.getY());
			//设置程序标题
			window.setTitle(title);
			window.initModality(Modality.APPLICATION_MODAL);
			//设置程序图标
			window.getIcons().add(new Image(DailogUtil.class.getClass().getResourceAsStream(Constant.APP_ICON)));
			Scene scene = new Scene(root);
			scene.fillProperty().set(Color.TRANSPARENT);
			window.setScene(scene);
			scene.getStylesheets().add("/resources/css/dailogDecoration.css");

			window.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 显示控制台弹出层
	 * @param content
	 * @param title
	 * @param callback 关闭时的回调
	 */
	public static void showConsoleDailog(FXMLLoader content, String title) {
		showConsoleDailog(content, title, Modality.APPLICATION_MODAL);
	}
	
	/**
	 * 显示控制台弹出层
	 * @param content
	 * @param title
	 * @param callback 关闭时的回调
	 */
	public static void showConsoleDailog(FXMLLoader content, String title, Modality applicationModal) {
		try {
			URL url = DailogUtil.class.getClass().getResource("/resources/template/dailogDecoration.fxml");
			FXMLLoader loader =  new FXMLLoader(url);
			Pane ui = loader.load();
			DailogDecorationController dailogDecorationController = loader.getController();
			Pane dailogContent = content.load();
			ui.setPrefWidth(800);
			ui.setPrefHeight(550);
			dailogDecorationController.getDailogContent().getChildren().add(dailogContent);
			Group root = addShadow(ui);
			Stage window = new Stage(StageStyle.TRANSPARENT);
			dailogDecorationController.setStage(window);
			dailogDecorationController.setTitle(title);
			Point point = getDailogPoint(ui.getPrefWidth(), ui.getPrefHeight());
			window.setX(point.getX());
			window.setY(point.getY());
			//设置程序标题
			window.setTitle(title);
			window.initModality(applicationModal);
			//设置程序图标
			window.getIcons().add(new Image(DailogUtil.class.getClass().getResourceAsStream(Constant.APP_ICON)));
			Scene scene = new Scene(root);
			scene.fillProperty().set(Color.TRANSPARENT);
			window.setScene(scene);
			scene.getStylesheets().add("/resources/css/console.css");
			if(dailogContent.getId() != null) {
				Context.addStage(dailogContent.getId(), window);
			}
			
			DailogController controller = content.getController();
			
			controller.setCallback(null);
			controller.setPageId(dailogContent.getId());
			
			window.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
