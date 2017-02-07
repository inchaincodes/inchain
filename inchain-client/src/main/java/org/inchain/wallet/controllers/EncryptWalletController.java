package org.inchain.wallet.controllers;

import org.inchain.core.Result;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.wallet.utils.DailogUtil;
import org.springframework.util.StringUtils;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * 加密钱包
 * @author ln
 *
 */
public class EncryptWalletController extends DailogController {

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
		passwordId.setText("");
		repeatId.setText("");
		close();
	}

	/*
	 * 加密
	 */
	private void encryptWallet() {
		
		//校验密码
		String password = passwordId.getText();
		String passwordRepeat = repeatId.getText();
		if(StringUtils.isEmpty(password)) {
			passwordId.requestFocus();
			DailogUtil.showTip("密码不能为空", getThisStage());
			return;
		} else if(!password.equals(passwordRepeat)) {
			repeatId.requestFocus();
			DailogUtil.showTip("两次输入的密码不一致", getThisStage());
			return;
		} else if(!validPassword(password)) {
			passwordId.requestFocus();
			DailogUtil.showTip("输入的密码需6位或以上，且包含字母和数字", getThisStage());
			return;
		}
		
		//加密并判断结果
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	Result result = accountKit.encryptWallet(password);
		if(result.isSuccess()) {
    		DailogUtil.showTip(result.getMessage());
    		resetAndclose();
		} else {
			log.error("加密钱包失败,{}", result);
			DailogUtil.showTip("加密钱包失败," + result.getMessage(), getThisStage());
		}
		
	}
}
