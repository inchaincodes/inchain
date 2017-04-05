package org.inchain.wallet.controllers;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class ApplicationListController implements SubPageController{

	public Button antiCounterfeitingId; //防伪溯源
	public AnchorPane antiCounterfeitingBodyId;
	
	public StackPane contentId;				//子页面内容控件
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	antiCounterfeitingId.setOnAction(e->antiCounterfeiting());
    }
    
	@Override
	public void initDatas() {
		
	}

	private void antiCounterfeiting() {
		Pane page = null;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/template/antiCounterfeiting.fxml"));
		try {
			page = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(page != null) {
			contentId = (StackPane) antiCounterfeitingBodyId.getParent();
			contentId.getChildren().clear();
			contentId.getChildren().add(page);
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
