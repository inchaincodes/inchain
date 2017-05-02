package org.inchain.wallet.controllers;

import java.util.HashMap;
import java.util.Map;

import org.inchain.Configure;
import org.inchain.utils.StringUtil;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;

public class TransferController extends DailogController{

	private static final Logger log = LoggerFactory.getLogger(TransferController.class);
	
	public Label tips;
	public Button transferId;
	public TextArea remarkId;
	public TextField receiverId;
	
	public void initialize() {
		
		tips.setText("转让商品会消耗" + Math.abs(Configure.TRANSFER_ANTIFAKECODE_SUB_CREDIT) + "点信用值！");
		
		transferId.setOnAction(e-> transfer());
	}
	 
	private void transfer() {
		if(StringUtil.isEmpty(receiverId.getText().trim())) {
			receiverId.requestFocus();
			DailogUtil.showTipDailogCenter("请输入接收人的地址/别名！",getThisStage());
			return;
		}
		if(StringUtil.isEmpty(remarkId.getText().trim())) {
			remarkId.requestFocus();
			DailogUtil.showTipDailogCenter("请输入转让备注！",getThisStage());
			return;
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("controller", this);
		map.put("receiver", receiverId.getText().trim());
		map.put("remark", remarkId.getText().trim());
		
		callback.ok(map);
	}
	
	/*
	 * 取消
	 */
	public void resetAndclose() {
		remarkId.setText("");
		receiverId.setText("");
		close();
	}
}
