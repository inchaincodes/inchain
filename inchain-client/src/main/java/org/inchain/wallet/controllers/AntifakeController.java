package org.inchain.wallet.controllers;

import java.net.URL;

import org.inchain.core.BroadcastResult;
import org.inchain.core.Definition;
import org.inchain.core.exception.AccountEncryptedException;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

/**
 * 防伪测试控制器
 * @author ln
 *
 */
public class AntifakeController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(AntifakeController.class);
	
	public TextArea antifakeCodeId;				//防伪码内容
	
	public Button verifyButId;						//验证按钮
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
    	
    	verifyButId.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				verify();
			}
		});
    }

	/**
     * 初始方法
     */
    public void initDatas() {
    	
    }

    /**
     * 验证防伪码
     */
    protected void verify() {

    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	
    	//防伪码内容
    	String antifakeCode = antifakeCodeId.getText();
    	//验证接收地址
    	if("".equals(antifakeCode)) {
    		antifakeCodeId.requestFocus();
    		DailogUtil.showTip("请输入防伪码");
    		return;
    	}
    	
		//调用接口广播交易
    	//如果账户已加密，则需要先解密
		verifyDo(accountKit, antifakeCode);
	}

    public void verifyDo(AccountKit accountKit, String antifakeCode) {
    	try {
    		BroadcastResult broadcastResult = accountKit.verifyAntifakeCode(antifakeCode);
			//返回的交易id，则成功
			if(broadcastResult.isSuccess()) {
				resetForms();
			}
			DailogUtil.showTip(broadcastResult.getMessage(), 2000);
    	} catch (Exception e) {
    		if(e instanceof AccountEncryptedException) {
    			decryptWallet(accountKit, antifakeCode);
    			return;
    		}
    		e.printStackTrace();
    		DailogUtil.showTip(e.getMessage(), 3000);
    		if(log.isDebugEnabled()) {
    			log.debug("验证出错：{}", e.getMessage());
    		}
		}
	}

	private void decryptWallet(AccountKit accountKit, String antifakeCode) {
		//解密账户
		URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
		FXMLLoader loader = new FXMLLoader(location);
		final AccountKit accountKitTemp = accountKit;
		DailogUtil.showDailog(loader, "输入钱包密码", new Runnable() {
			@Override
			public void run() {
				if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
					try {
						verifyDo(accountKitTemp, antifakeCode);
					} finally {
						accountKitTemp.resetKeys();
					}
				}
			}
		});
	}
    
    /**
     * 重置表单
     */
	public void resetForms() {
		Platform.runLater(new Runnable() {
		    @Override
		    public void run() {
		    	antifakeCodeId.setText("");
		    }
		});
	}
	
	@Override
	public void onShow() {
		
	}

	@Override
	public void onHide() {
	}
}
