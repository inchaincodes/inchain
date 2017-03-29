package org.inchain.wallet.controllers;

import org.inchain.core.Result;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.wallet.utils.DailogUtil;
import org.springframework.util.StringUtils;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 设置账户别名
 * @author ln
 *
 */
public class SetAliasController extends DailogController {

	public TextField aliasId;
	public Label addressId;
	
	public Button okId;
	public Button cancelId;
	
	public void initialize() {
		cancelId.setOnAction(e -> resetAndclose());
		okId.setOnAction(e -> doSave());
	}
	
	/*
	 * 取消
	 */
	private void resetAndclose() {
		aliasId.setText("");
		close();
	}

	/*
	 * 确定
	 */
	private void doSave() {
		
		//校验
		String alias = aliasId.getText();
		if(StringUtils.isEmpty(alias)) {
			aliasId.requestFocus();
			DailogUtil.showTipDailogCenter("别名不能为空", getThisStage());
			return;
		}
		
		//修改密码并判断结果
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		
    	Result result = accountKit.setAlias(alias);
		if(result.isSuccess()) {
			if(callback != null) {
				callback.run();
			}
    		DailogUtil.showTipDailogCenter(result.getMessage(),getThisStage());
    		resetAndclose();
		} else {
			log.error("别名设置失败,{}", result);
			DailogUtil.showTipDailogCenter("别名设置失败," + result.getMessage(), getThisStage());
		}
		
	}
}
