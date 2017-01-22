package org.inchain.wallet.controllers;

import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.wallet.listener.AccountInfoListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Label;

/**
 * 账户信息控制器
 * @author ln
 *
 */
public class AccountInfoController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(AccountInfoController.class);
	
	//账户信息监听器，加载完账户之后执行
	private AccountInfoListener accountInfoListener;
	
	public Label canUseBlanaceId;					//可用余额
	public Label canNotUseBlanaceId;				//不可用余额
	public Label certId;							//信用
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {

    }
    
    /**
     * 初始化钱包信息
     */
    public void initDatas() {
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	List<Account> accountList = accountKit.getAccountList();
    	
    	if(accountList != null && accountList.size() > 0) {
    		Account account = accountList.get(0);
    		if(accountInfoListener != null) {
    			accountInfoListener.onLoad(account);
    		}
    		//设置内页的余额
    		Address address = account.getAddress();
    		Platform.runLater(new Runnable() {
			    @Override
			    public void run() {
			    	canUseBlanaceId.setText(address.getBalance().toText());
			    	canNotUseBlanaceId.setText(address.getUnconfirmedBalance().toText());
			    }
			});
    	}
    }

	public void setAccountInfoListener(AccountInfoListener accountInfoListener) {
		this.accountInfoListener = accountInfoListener;
	}
}
