package org.inchain.wallet.controllers;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.core.TimeHelper;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.Context;
import org.inchain.wallet.listener.AccountInfoListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 账户信息控制器
 * @author ln
 *
 */
public class AccountInfoController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(AccountInfoController.class);
	
	//文件选择器
	private FileChooser fileChooser = new FileChooser();
	//账户信息监听器，加载完账户之后执行
	private AccountInfoListener accountInfoListener;
	
	public Label canUseBlanaceId;					//可用余额
	public Label canNotUseBlanaceId;				//不可用余额
	public Label certId;							//信用
	
	public Button backupWalletId;					//备份钱包
	public Button importWalletId;					//导入钱包
	public Button encryptWalletId;					//加密钱包
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	//点击备份钱包事件
    	backupWalletId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				backupWallet();
			}
		});
    	//点击导入钱包事件
    	importWalletId.setOnAction(new EventHandler<ActionEvent>() {
    		@Override
    		public void handle(ActionEvent event) {
    			importWallet();
    		}
    	});
    	//点击加密钱包事件
    	encryptWalletId.setOnAction(new EventHandler<ActionEvent>() {
    		@Override
    		public void handle(ActionEvent event) {
    			encryptWallet();
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
    		if(accountInfoListener != null) {
    			accountInfoListener.onLoad(account);
    		}
    		//设置内页的余额
    		final Address address = account.getAddress();
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

	@Override
	public void onShow() {
		//noting to do
	}

	@Override
	public void onHide() {
		//noting to do
	}

	/*
	 * 备份钱包
	 */
	private void backupWallet() {
		//创建一个文件选择器
		fileChooser.setTitle("设置钱包备份路径");
		//设置文件格式
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("DAT files (*.dat)", "*.dat");  
        fileChooser.getExtensionFilters().add(extFilter);
        //默认选择程序运行目录
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        //默认备份文件名
        fileChooser.setInitialFileName("wallet_backup_".concat(DateUtil.convertDate(new Date(TimeHelper.currentTimeMillis()), "yyyyMMddHHmm")));
        Stage stage = new Stage();
		File file = fileChooser.showSaveDialog(Context.stage);
		if(file==null) {
			return;
		}
		//用户选择的完整路径
		String exportFilePath = file.getAbsolutePath();
		//去掉用户自己设置的后缀.dat
		if(exportFilePath.endsWith(".dat") || exportFilePath.endsWith(".DAT")) {
			exportFilePath = exportFilePath.substring(0, exportFilePath.length() - 4).concat(".dat");
		}
		log.info("选择的路径为 "+ exportFilePath);
	}

	/*
	 * 导入钱包
	 */
	private void importWallet() {
		//创建一个文件选择器
		fileChooser.setTitle("选择需要导入的钱包文件");
		//设置文件格式
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("DAT files (*.dat)", "*.dat");  
        fileChooser.getExtensionFilters().add(extFilter);
        //默认选择程序运行目录
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        //默认备份文件名
        fileChooser.setInitialFileName("wallet_backup_".concat(DateUtil.convertDate(new Date(TimeHelper.currentTimeMillis()), "yyyyMMddHHmm")));
        Stage stage = new Stage();
		File file = fileChooser.showOpenDialog(Context.stage);
		if(file==null) {
			return;
		}
		//用户选择的完整路径
		String exportFilePath = file.getAbsolutePath();
		//去掉用户自己设置的后缀.dat
		if(exportFilePath.endsWith(".dat") || exportFilePath.endsWith(".DAT")) {
			exportFilePath = exportFilePath.substring(0, exportFilePath.length() - 4).concat(".dat");
		}
		log.info("选择的钱包文件为 "+ exportFilePath);
	}
	
	/*
	 * 加密钱包
	 */
	private void encryptWallet() {
		
	}
}
