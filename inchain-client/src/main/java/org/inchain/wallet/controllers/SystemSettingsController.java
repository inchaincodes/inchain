package org.inchain.wallet.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;

/**
 * 系统设置
 * @author cj
 *
 */

public class SystemSettingsController implements SubPageController{

	private static final Logger log = LoggerFactory.getLogger(SystemSettingsController.class);
	
	//菜单按钮
	public Button systemInfoId;
	public Button nodeInfoId;
	public Button consoleId;
	
	public StackPane contentId;				//子页面内容控件
	private List<Button> buttons = new ArrayList<Button>();
	private Map<String, Node> pageMaps = new HashMap<String, Node>();	//页面列表
	private Map<String, SubPageController> subPageControllerMaps = new HashMap<String, SubPageController>();	//页面控制器
	private String currentPageId;			//当前显示的页面
	
	/**
	 * FXML调用的初始化
	 */
	public void initialize() {

		Image console = new Image(getClass().getResourceAsStream("/images/console.png"));
		consoleId.setGraphic(new ImageView(console));
		consoleId.setOnAction(e -> openConsole());
		
		buttons.add(systemInfoId);
		buttons.add(nodeInfoId);
		initPages();
		EventHandler<ActionEvent> buttonEventHandler = getPageEventHandler();
		for (Button button : buttons) {
			button.setOnAction(buttonEventHandler);
		}
	}

	private void openConsole() {
		//System.out.println("====");
		URL url = DailogUtil.class.getClass().getResource("/resources/template/console.fxml");
		FXMLLoader loader =  new FXMLLoader(url);
		DailogUtil.showConsoleDailog(loader, "控制台", Modality.WINDOW_MODAL);
	}

	/*
	 * 获取页面切换事件
	 */
	private EventHandler<ActionEvent> getPageEventHandler() {
		EventHandler<ActionEvent> buttonEventHandler = new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Button button = (Button) event.getSource();
				if(button == null) {
					return;
				}
				initButtonbg();
				button.setStyle("-fx-background-color: #1554a2;");
				String id = button.getId();
				//触发页面显示隐藏事件
				for (Entry<String, SubPageController> entry : subPageControllerMaps.entrySet()) {
					String pageId = entry.getKey();
					SubPageController pageController = entry.getValue();
					//隐藏上个页面
					if(pageId.equals(currentPageId)) {
						pageController.onHide();
					} else if(pageId.equals(id)) {
						
						pageController.initDatas();
						
						pageController.onShow();
					}
				}
				showPage(id);
			}
		};
		return buttonEventHandler;
	}
	protected void initButtonbg() {
		for (Button button : buttons) {
			button.setStyle(" -fx-background-color: #1b346e;");
		}
	}
	/**
	 * 显示子页面
	 * @param id
	 */
	private void showPage(final String id) {
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
				
				currentPageId = id;
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
		case "nodeInfoId":
			//点击账户信息按钮
			fxml = "/resources/template/nodeInfo.fxml";
			break;
		case "systemInfoId":
			//点击转账按钮
			fxml = "/resources/template/systemInfo.fxml";
			break;
		default:
			break;
		}
		
		if(fxml != null) {
			try {
		        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
				Pane page = loader.load();
				SubPageController subPageController = loader.getController();
				if(subPageController != null) {
					subPageControllerMaps.put(id, subPageController);
				}
				return page;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	/*
	 * 初始化各个界面
	 */
	private void initPages() {

		for (Button button : buttons) {
			String id = button.getId();
			pageMaps.put(id, getPage(id));
		}
		//显示第一个子页面
		if(buttons.size() > 0) {
			showPage(buttons.get(0).getId());
			buttons.get(0).setStyle("-fx-background-color: #1554a2;");
		}
	}
	@Override
	public void initDatas() {
		for (SubPageController controller : subPageControllerMaps.values()) {
			controller.initDatas();
		}
	}

	@Override
	public void onShow() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onHide() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean refreshData() {
		return true;
	}

}
