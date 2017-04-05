package org.inchain.wallet.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class AntiCounterfeitingController implements SubPageController{

	private static final Logger log = LoggerFactory.getLogger(AntiCounterfeitingController.class);
	
	public Button antifakeId;  //商品验证
	public Button myProductId; //我的商品列表
	public Button flowInfoId; //流转信息
	public StackPane contentId;  //内容
	
	private List<Button> buttons = new ArrayList<Button>();
	private Map<String, Node> pageMaps = new HashMap<String, Node>();	//页面列表
	private Map<String, SubPageController> subPageControllerMaps = new HashMap<String, SubPageController>();	//页面控制器
	private String currentPageId;			//当前显示的页面
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	
    	buttons.add(antifakeId);
    	buttons.add(flowInfoId);
    	buttons.add(myProductId);
    	EventHandler<ActionEvent> buttonEventHandler = getPageEventHandler();
		for (Button button : buttons) {
			button.setOnAction(buttonEventHandler);
		}
		initPages();
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
				
				selectButton(button);
				
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
			button.setStyle("");
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
		case "antifakeId":
			//点击账户信息按钮
			fxml = "/resources/template/antifake.fxml";
			break;
		case "flowInfoId":
			//点击转账按钮
			fxml = "/resources/template/flow.fxml";
			break;
		case "myProductId":
			//点击转账按钮
			fxml = "/resources/template/myProductList.fxml";
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
			selectButton(buttons.get(0));
		}
	}
	
	private void selectButton(Button button) {
		button.setStyle("-fx-background-color: #3b5aac;");
	}
	@Override
	public void initDatas() {
		for (SubPageController controller : subPageControllerMaps.values()) {
			controller.initDatas();
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
		return false;
	}

	@Override
	public boolean startupInit() {
		return false;
	}

}
