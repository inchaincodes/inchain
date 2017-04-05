package org.inchain.wallet.controllers;

import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

public class FlowController implements SubPageController{

	private static final Logger log = LoggerFactory.getLogger(FlowController.class);
	
	public Button defineId; 		//确定按钮
	public Button resetId;			//重置按钮
	public ChoiceBox<String> eventTagId;  //事件标签
	public TextField customizeId;	//自定义事件标签
	
	public TextArea antifakeCodeId;	//防伪码
	public TextArea descriptionId;	//说明
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	eventTagId.setItems(FXCollections.observableArrayList("自定义","生产","入库","出库","送货"));
    	Tooltip tip = new Tooltip("选择流转事件");
    	tip.setStyle("-fx-font-size:16;");
    	eventTagId.setTooltip(tip);
    	eventTagId.getSelectionModel().selectedItemProperty().addListener((ov,oldv,newv) -> {
    		if(newv != null) {
    			if(newv.equals("自定义")) {
    				customizeId.setText("");
    				customizeId.requestFocus();
    				customizeId.setVisible(true);
    			} else {
    				customizeId.setVisible(false);
    				customizeId.setText(newv);
    			}
    		}
    	});
    	defineId.setOnAction(e -> define());
    	resetId.setOnAction(e -> reset());
    }
    /*
     * 重置内容
     * */
	private void reset() {
		customizeId.setVisible(false);
	   	eventTagId.getSelectionModel().clearSelection();;
		antifakeCodeId.setText("");
		descriptionId.setText("");
	}

	/*
	 * 进行流转
	 * */
	private void define() {
		if("".equals(antifakeCodeId.getText())) {
			antifakeCodeId.requestFocus();
			DailogUtil.showTip("请输入防伪码");
    		return;
		}
		if("".equals(customizeId.getText())) {
			customizeId.requestFocus();
			DailogUtil.showTip("请选择事件标签");
			return;
		}
		if("".equals(descriptionId.getText())) {
			descriptionId.requestFocus();
			DailogUtil.showTip("请输入流转说明");
			return;
		}
		DailogUtil.showTip("流转成功！");
		reset();
	}

	@Override
	public void initDatas() {
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
