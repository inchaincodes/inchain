package org.inchain.wallet.controllers;

import org.inchain.core.Result;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.wallet.utils.DailogUtil;
import org.springframework.util.StringUtils;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * 修改钱包密码
 * @author ln
 *
 */
public class ChangeWalletPasswordController extends DailogController {

	public TextField oldPasswordId;
	public TextField passwordId;
	public TextField repeatId;
	
	public Button okId;
	public Button cancelId;
	
	public void initialize() {
		cancelId.setOnAction(e -> resetAndclose());
		okId.setOnAction(e -> encryptWallet());
	}
	
	/*
	 * 取消
	 */
	private void resetAndclose() {
		oldPasswordId.setText("");
		passwordId.setText("");
		repeatId.setText("");
		close();
	}

	/*
	 * 加密
	 */
	private void encryptWallet() {
		
		//校验密码
		String oldPassword = oldPasswordId.getText();
		String password = passwordId.getText();
		String passwordRepeat = repeatId.getText();
		if(StringUtils.isEmpty(oldPassword)) {
			oldPasswordId.requestFocus();
			DailogUtil.showTip("原密码不能为空", getThisStage());
			return;
		} else if(StringUtils.isEmpty(password)) {
			passwordId.requestFocus();
			DailogUtil.showTip("新密码不能为空", getThisStage());
			return;
		} else if(StringUtils.isEmpty(oldPassword)) {
			oldPasswordId.requestFocus();
			DailogUtil.showTip("原密码不正确", getThisStage());
			return;
		} else if(!password.equals(passwordRepeat)) {
			repeatId.requestFocus();
			DailogUtil.showTip("两次输入的新密码不一致", getThisStage());
			return;
		} else if(!validPassword(password)) {
			passwordId.requestFocus();
			DailogUtil.showTip("输入的密码需6位或以上，且包含字母和数字", getThisStage());
			return;
		}
		
		//修改密码并判断结果
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	Result result = accountKit.changeWalletPassword(oldPassword, password);
		if(result.isSuccess()) {
    		DailogUtil.showTip(result.getMessage());
    		resetAndclose();
		} else {
			log.error("密码修改失败,{}", result);
			DailogUtil.showTip("密码修改失败," + result.getMessage(), getThisStage());
		}
		
	}
}
