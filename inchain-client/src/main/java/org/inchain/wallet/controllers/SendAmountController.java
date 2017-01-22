package org.inchain.wallet.controllers;

import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.network.NetworkParams;
import org.inchain.wallet.Context;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 交易转账控制器
 * @author ln
 *
 */
public class SendAmountController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(SendAmountController.class);
	
	public Label canUseBlanaceId;					//可用余额
	public TextField receiveAddressId;					//接收地址
	public TextField sendAmountId;						//发送金额
	public TextField feeId;							//手续费
	
	public Button sendButId;						//发送按钮
	public Button resetButId;						//重置按钮
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	resetButId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				resetForms();
			}
		});
    	
    	sendButId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				sendAmount();
			}
		});
    }

	/**
     * 初始化钱包信息
     */
    public void initDatas() {
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	List<Account> accountList = accountKit.getAccountList();
    	
    	if(accountList != null && accountList.size() > 0) {
    		Account account = accountList.get(0);
    		//设置内页的余额
    		Address address = account.getAddress();
    		canUseBlanaceId.setText(address.getBalance().toText());
    	}
    }

    /**
     * 发送交易
     */
    protected void sendAmount() {
    	
    	//提示框位置
    	double x = Context.stage.getX() + (Context.stage.getWidth() - 100) / 2;
    	double y = Context.stage.getY() + (Context.stage.getHeight() - 30) / 2;
    	
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	NetworkParams network = InchainInstance.getInstance().getAppKit().getNetwork();
    	
    	//接收地址
    	String address = receiveAddressId.getText();
    	//验证接收地址
    	if("".equals(address)) {
    		receiveAddressId.requestFocus();
    		DailogUtil.showTip("请输入接收地址", x, y);
    		return;
    	} else {
    		//验证地址合法性
    		try {
    			Address.fromBase58(network, address);
    		} catch (Exception e) {
        		receiveAddressId.requestFocus();
        		DailogUtil.showTip("错误的接收地址", x, y);
        		return;
			}
    	}
    	
    	//手续费
    	String fee = feeId.getText();
    	Coin feeCoin = null;
    	//验证手续费
    	try {
    		feeCoin = Coin.parseCoin(fee);
		} catch (Exception e) {
			sendAmountId.requestFocus();
    		DailogUtil.showTip("错误的手续费金额", x, y);
    		return;
		}
    	if(feeCoin.compareTo(Coin.ZERO) <= 0) {
    		feeCoin = Coin.parseCoin("0.01");
    	}
    	
    	String amount = sendAmountId.getText();
    	Coin money = null;
    	//验证金额
    	if("".equals(amount)) {
    		sendAmountId.requestFocus();
    		DailogUtil.showTip("请输入发送金额", x, y);
    		return;
    	} else {
    		//验证金额合法性
    		try {
    			money = Coin.parseCoin(amount);
    			if(money.compareTo(Coin.MAX) > 0) {
    				sendAmountId.requestFocus();
    				DailogUtil.showTip("发送金额超过可用余额", x, y);
    				return;
    			}
    			Coin balance = accountKit.getCanUseBalance();
    			if(money.add(feeCoin).compareTo(balance) > 0) {
    				sendAmountId.requestFocus();
    				DailogUtil.showTip("发送金额超过可用余额", x, y);
    				return;
    			}
    		} catch (Exception e) {
    			sendAmountId.requestFocus();
        		DailogUtil.showTip("错误的金额", x, y);
        		return;
			}
    	}
    	
    	//验证通过，调用接口广播交易
    	String result = null;
    	try {
    		result = accountKit.sendMoney(address, money, feeCoin);
    	} catch (Exception e) {
    		result = e.getMessage();
    		log.error(e.getMessage(), e);
		}
    	DailogUtil.showTip(result, x, y);
		log.info("===");
	}
    
    /**
     * 重置表单
     */
	public void resetForms() {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	receiveAddressId.setText("");
		    	sendAmountId.setText("");
		    }
		});
	}
}
