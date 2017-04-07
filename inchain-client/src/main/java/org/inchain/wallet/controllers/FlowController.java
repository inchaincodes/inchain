package org.inchain.wallet.controllers;

import java.net.URL;

import org.inchain.SpringContextUtils;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Definition;
import org.inchain.kits.AccountKit;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
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
		customizeId.setText("");
	}

	/*
	 * 进行流转
	 * */
	private void define() {
		
		String antifakeCode = antifakeCodeId.getText();
		String customize = customizeId.getText();
		String description = descriptionId.getText();
		
		if("".equals(antifakeCode)) {
			antifakeCodeId.requestFocus();
			DailogUtil.showTip("请输入防伪码");
    		return;
		}
		if("".equals(customize)) {
			customizeId.requestFocus();
			DailogUtil.showTip("请选择事件标签");
			return;
		}
		if("".equals(description)) {
			descriptionId.requestFocus();
			DailogUtil.showTip("请输入流转说明");
			return;
		}
		AccountKit accountKit = SpringContextUtils.getBean(AccountKit.class);
		if(accountKit.accountIsEncrypted()) {
			decryptWallet(accountKit);
			return;
		}
		BroadcastResult res = accountKit.addCirculation(antifakeCode, customize, description, null);
		
		if(res.isSuccess()) {
			DailogUtil.showTip("添加流转信息成功！");
			reset();
		} else {
			DailogUtil.showTip(res.getMessage());
		}
	}
	
	private void decryptWallet(final AccountKit accountKit) {
		//解密账户
		URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
		FXMLLoader loader = new FXMLLoader(location);
		DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
			@Override
			public void ok(Object param) {
				if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
					new Thread() {
	    				public void run() {
	    					Platform.runLater(new Runnable() {
	    					    @Override
	    					    public void run() {
	    					    	try {
	    					    		define();
	    					    	} finally {
	    					    		accountKit.resetKeys();
	    					    	}
	    					    }
	    					});
	    				};
	    			}.start();
				}
			}
		});
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
