package org.inchain.wallet.controllers;

import org.inchain.utils.StringUtil;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class TransferController extends DailogController{

	private static final Logger log = LoggerFactory.getLogger(TransferController.class);
	
	 public Button transferId;
	 public TextArea remarkId;
	 public TextField receiverId;
	 
	 public void initialize() {
		 transferId.setOnAction(e-> transfer());
	 }
	private void transfer() {
		if(StringUtil.isEmpty(receiverId.getText())) {
			receiverId.requestFocus();
			DailogUtil.showTipDailogCenter("请输入接收人的地址/别名！",getThisStage());
			return;
		}
		if(StringUtil.isEmpty(remarkId.getText())) {
			remarkId.requestFocus();
			DailogUtil.showTipDailogCenter("请输入转让备注！",getThisStage());
			return;
		}
		resetAndclose();
	}
	/*
	 * 取消
	 */
	private void resetAndclose() {
		remarkId.setText("");
		receiverId.setText("");
		close();
		if(callback != null) {
			callback.ok(null);
		}
	}
}
