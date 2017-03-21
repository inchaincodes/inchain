package org.inchain.wallet.controllers;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.core.Result;
import org.inchain.core.TimeService;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.store.AccountStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.Context;
import org.inchain.wallet.listener.AccountInfoListener;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

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
	
	public Label totalBalanceId;					//总余额
	public Label canUseBlanaceId;					//可用余额
	public Label canNotUseBlanaceId;				//不可用余额
	public Label addressId;							//账户地址
	public Label certId;							//信用
	public Label transactionNumberId;				//交易数量
	public Label encryptionStatusId;				//加密状态
	public Label consensusStatusId;					//当前共识状态
	
	public Button backupWalletId;					//备份钱包
	public Button importWalletId;					//导入钱包
	public Button encryptWalletId;					//加密钱包
	
	Image imageDecline;
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	//初始化按钮
    	addImageToButton(backupWalletId,"backupWallet");
    	addImageToButton(importWalletId,"importWallet");
    	addImageToButton(encryptWalletId,"encryptWallet");
    	//点击备份钱包事件
    	backupWalletId.setOnAction(e -> backupWallet());
    	//点击导入钱包事件
    	importWalletId.setOnAction(e -> importWallet());
    	//点击加密钱包事件
    	encryptWalletId.setOnAction(e -> encryptWallet());
    	//设置文件格式
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("DAT files (*.dat)", "*.dat");  
        fileChooser.getExtensionFilters().add(extFilter);
        //默认选择程序运行目录
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
    }

    private void addImageToButton(Button button,String name) {
		imageDecline = new Image(getClass().getResourceAsStream("/images/"+name+"_icon.png"));  
    	button.setGraphic(new ImageView(imageDecline));
    	button.setGraphicTextGap(10);
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
    		//自己的账户信息
	    	AccountStore accountStore = accountKit.getAccountInfo();
	    	//自己的交易信息
	    	TransactionStoreProvider transactionStoreProvider = SpringContextUtils.getBean(TransactionStoreProvider.class);
	    	if(transactionStoreProvider == null) {
	    		//return;
	    	}
    		Platform.runLater(new Runnable() {
			    @Override
			    public void run() {
			    	totalBalanceId.setText(address.getBalance().add(address.getUnconfirmedBalance()).toText());
			    	canUseBlanaceId.setText(address.getBalance().toText());
			    	canNotUseBlanaceId.setText(address.getUnconfirmedBalance().toText());
			    	addressId.setText(address.getBase58());
			    	certId.setText(String.valueOf(accountStore.getCert()));
			    	transactionNumberId.setText(String.valueOf(transactionStoreProvider.getTransactions().size()));
			    	if(accountKit.accountIsEncrypted()) {
			    		encryptionStatusId.setText("已加密");
			    	} else {
			    		encryptionStatusId.setText("未加密,为了资金安全,请加密钱包");
			    	}
			    	if(accountKit.checkConsensusing()) {
			    		consensusStatusId.setText("正在共识中");
			    	} else {
			    		consensusStatusId.setText("未参与共识");
			    	}
			    }
			});
    	}
    	
		//判断账户是否加密
//    	Platform.runLater(new Runnable() {
//		    @Override
//		    public void run() {
//				if(accountKit.accountIsEncrypted()) {
//		    		encryptWalletId.setText("修改密码");
//				} else {
//					encryptWalletId.setText("加密钱包");
//				} 
//		    }
//		});
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
        //默认备份文件名
        fileChooser.setInitialFileName("wallet_backup_".concat(DateUtil.convertDate(new Date(TimeService.currentTimeMillis()), "yyyyMMddHHmm")));
		File file = fileChooser.showSaveDialog(Context.getMainStage());
		if(file==null) {
			return;
		}
		//用户选择的完整路径
		String backupFilePath = file.getAbsolutePath();
		//去掉用户自己设置的后缀.dat
		if(backupFilePath.endsWith(".dat") || backupFilePath.endsWith(".DAT")) {
			backupFilePath = backupFilePath.substring(0, backupFilePath.length() - 4).concat(".dat");
		}
		//备份
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	try {
    		Result result = accountKit.backupWallet(backupFilePath);
    		if(result.isSuccess()) {
        		DailogUtil.showTip("备份成功");
    		} else {
    			log.error("备份钱包失败,{}", result);
    			DailogUtil.showTip("备份钱包失败," + result.getMessage());
    		}
    	} catch (Exception e) {
    		log.error("备份钱包时出错 {} ", e.getMessage(), e);
    		DailogUtil.showTip("备份钱包时出错" + e.getMessage());
		}
	}

	/*
	 * 导入钱包
	 */
	private void importWallet() {
		//创建一个文件选择器
		fileChooser.setTitle("选择需要导入的钱包文件");
        //默认备份文件名
        fileChooser.setInitialFileName("wallet_backup_".concat(DateUtil.convertDate(new Date(TimeService.currentTimeMillis()), "yyyyMMddHHmm")));
		File file = fileChooser.showOpenDialog(Context.getMainStage());
		if(file==null) {
			return;
		}
		//用户选择的完整路径
		String walletFilePath = file.getAbsolutePath();
		//去掉用户自己设置的后缀.dat
		if(walletFilePath.endsWith(".dat") || walletFilePath.endsWith(".DAT")) {
			walletFilePath = walletFilePath.substring(0, walletFilePath.length() - 4).concat(".dat");
		}
		//备份
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	try {
    		Result result = accountKit.importWallet(walletFilePath);
    		if(result.isSuccess()) {
    			//更新余额及交易记录
    			accountKit.getTransactionListener().newTransaction(null);
        		DailogUtil.showTip(result.getMessage());
    		} else {
    			log.error("导入钱包失败,{}", result);
    			DailogUtil.showTip("导入钱包失败," + result.getMessage());
    		}
    	} catch (Exception e) {
    		log.error("导入钱包时出错 {} ", e.getMessage(), e);
    		DailogUtil.showTip("导入钱包时出错" + e.getMessage());
		}
	}
	
	/*
	 * 加密钱包
	 */
	private void encryptWallet() {
		
		try {
			AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
			//判断账户是否加密
			if(accountKit.accountIsEncrypted()) {
				//修改密码
				URL location = null;
				if(accountKit.isCertAccount()) {
					location = getClass().getResource("/resources/template/changeCertAccountPassword.fxml");
				} else {
					location = getClass().getResource("/resources/template/changeWalletPassword.fxml");
				}
		        FXMLLoader loader = new FXMLLoader(location);
				DailogUtil.showDailog(loader, "修改密码");
			} else {
				//加密
				URL location = getClass().getResource("/resources/template/encryptWallet.fxml");
		        FXMLLoader loader = new FXMLLoader(location);
		        
				DailogUtil.showDailog(loader, "加密钱包", new Runnable() {
					@Override
					public void run() {
						AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
						//判断账户是否加密
						if(accountKit.accountIsEncrypted()) {
				    		encryptWalletId.setText("修改密码");
						}
					}
				});
			}
		} catch (Exception e) {
			log.error("加密钱包出错" ,e);
		}
	}

	@Override
	public boolean refreshData() {
		return true;
	}

	@Override
	public boolean startupInit() {
		return true;
	}
}
