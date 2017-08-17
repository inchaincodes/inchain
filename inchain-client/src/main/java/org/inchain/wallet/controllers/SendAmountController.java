package org.inchain.wallet.controllers;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.network.NetworkParams;
import org.inchain.store.AccountStore;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * 交易转账控制器
 * @author ln
 *
 */
public class SendAmountController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(SendAmountController.class);
	
	public Label canUseBlanaceId;					//可用余额
	public TextField receiveAddressId;				//接收地址
	public TextField sendAmountId;					//发送金额
	public TextField feeId;							//手续费
	public TextArea remarkId;						//交易备注
	
	public Button sendButId;						//发送按钮
	public Button resetButId;						//重置按钮
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	Image sendIcon = new Image(getClass().getResourceAsStream("/images/send_icon.png"));
    	Image resetIcon = new Image (getClass().getResourceAsStream("/images/reset_icon.png"));
    	//设置回车监听
    	receiveAddressId.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(event.getCode() == KeyCode.ENTER) {
					sendAmountId.requestFocus();
				}
			}
		});
    	sendAmountId.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(event.getCode() == KeyCode.ENTER) {
					remarkId.requestFocus();
				}
			}
		});
//    	remarkId.setOnKeyPressed(new EventHandler<KeyEvent>() {
//			@Override
//			public void handle(KeyEvent event) {
//				if(event.getCode() == KeyCode.ENTER) {
//					sendAmount();
//				}
//			}
//		});
    	//设置按钮图片以及图片与字体之间的间距
    	sendButId.setGraphic(new ImageView(sendIcon));
    	sendButId.setGraphicTextGap(10);
    	resetButId.setGraphic(new ImageView(resetIcon));
    	resetButId.setGraphicTextGap(10);
    	remarkId.textProperty().addListener(new ChangeListener<String>() {
    		
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				//交易备注不能超过30字节
		    	String remark = remarkId.getText();
		    	byte[] remarkBytes = null;
				try {
					remarkBytes = remark.getBytes("utf-8");
				} catch (UnsupportedEncodingException e1) {
				}
		    	if(remarkBytes != null && remarkBytes.length > 100) {
		    		remarkId.requestFocus();
		    		DailogUtil.showTip("留言太长，最多50个英文或者30个汉字");
		    		return;
		    	}
			}
		});
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
    	//获取最新余额
    	loadNewestBalance();
    }

    /**
     * 获取最新的余额信息
     */
    protected void loadNewestBalance() {
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
    	
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	NetworkParams network = InchainInstance.getInstance().getAppKit().getNetwork();
    	
    	//接收地址
    	String address = receiveAddressId.getText().trim();
    	//验证接收地址
    	if("".equals(address)) {
    		receiveAddressId.requestFocus();
    		DailogUtil.showTip("请输入接收地址");
    		return;
    	} else {
    		//验证地址合法性
    		try {
    			Address.fromBase58(network, address);
    		} catch (Exception e) {
    			//可能是别名
    			ChainstateStoreProvider chainstateStoreProvider = SpringContextUtils.getBean(ChainstateStoreProvider.class);
    			
    			AccountStore accountInfo = null;
				try {
					accountInfo = chainstateStoreProvider.getAccountInfoByAlias(address.getBytes("utf-8"));
				} catch (UnsupportedEncodingException e1) {
					receiveAddressId.requestFocus();
	        		DailogUtil.showTip("错误的别名");
	        		return;
				}
    			if(accountInfo == null) {
	        		receiveAddressId.requestFocus();
	        		DailogUtil.showTip("错误的接收地址或别名");
	        		return;
    			} else {
    				address = new Address(network, accountInfo.getType(), accountInfo.getHash160()).getBase58();
    			}
			}
    	}
    	
    	//手续费
    	String fee = feeId.getText();
    	Coin feeCoin = null;
    	//验证手续费
    	try {
    		feeCoin = Coin.parseCoin(fee.trim());
		} catch (Exception e) {
			sendAmountId.requestFocus();
    		DailogUtil.showTip("错误的手续费金额");
    		return;
		}
    	if(feeCoin.compareTo(Definition.MIN_PAY_FEE) < 0) {
    		feeCoin = Definition.MIN_PAY_FEE;
    	}
    	
    	String amount = sendAmountId.getText().trim();
    	Coin money = null;
    	//验证金额
    	if("".equals(amount)) {
    		sendAmountId.requestFocus();
    		DailogUtil.showTip("请输入发送金额");
    		return;
    	} else {
    		//验证金额合法性
    		try {
    			money = Coin.parseCoin(amount);
    			if(money.compareTo(Coin.MAX) > 0) {
    				sendAmountId.requestFocus();
    				DailogUtil.showTip("发送金额超过可用余额");
    				return;
    			}
    			Coin balance = accountKit.getCanUseBalance();
    			if(money.add(feeCoin).compareTo(balance) > 0) {
    				sendAmountId.requestFocus();
    				DailogUtil.showTip("发送金额超过可用余额");
    				return;
    			}
    		} catch (Exception e) {
    			sendAmountId.requestFocus();
        		DailogUtil.showTip("错误的金额");
        		return;
			}
    	}
    	
    	//交易备注不能超过30字节
    	String remark = remarkId.getText().trim();
    	byte[] remarkBytes = null;
		try {
			remarkBytes = remark.getBytes("utf-8");
		} catch (UnsupportedEncodingException e1) {
		}
    	if(remarkBytes != null && remarkBytes.length > 100) {
    		remarkId.requestFocus();
    		DailogUtil.showTip("留言太长，最多50个英文或者30个汉字");
    		return;
    	}
    	
		//验证通过，调用接口广播交易
    	try {
    		//如果账户已加密，则需要先解密
    		if(accountKit.accountIsEncrypted()) {
    			//解密账户
    			URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
		        FXMLLoader loader = new FXMLLoader(location);
		        final AccountKit accountKitTemp = accountKit;
		        final String addressTemp = address;
		        final Coin feeCoinTemp = feeCoin;
		        final Coin moneyTemp = money;
		        final byte[] remarkBytesTemp = remarkBytes;
				DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
					@Override
					public void ok(Object param) {
						Account account = accountKit.getDefaultAccount();
						if(!((!account.isCertAccount() && account.isEncrypted()) ||
								(account.isCertAccount() && account.isEncryptedOfTr()))) {
							try {
								sendMoney(accountKitTemp, addressTemp, moneyTemp, feeCoinTemp, remarkBytesTemp);
							} finally {
								accountKitTemp.resetKeys();
							}
						}
					}
				});
    		} else {
				sendMoney(accountKit, address, money, feeCoin, remarkBytes);
    			
//    			for (int i = 0; i < 1; i++) {
//					
//    				accountKit.sendMoney(address, money, feeCoin);
//				}
    		}
    	} catch (Exception e) {
        	DailogUtil.showTip(e.getMessage());
        	log.error(e.getMessage(), e);
		}
	}

    public void sendMoney(AccountKit accountKit, String address, Coin money, Coin feeCoin, byte[] remark) {
    	try {
    		BroadcastResult broadcastResult = accountKit.sendMoney(address, money, feeCoin, remark, null, null);
			//返回的交易id，则成功
			if(broadcastResult.isSuccess()) {
				loadNewestBalance();
				resetForms();
			}
			DailogUtil.showTip(broadcastResult.getMessage(), 2000);
    	} catch (Exception e) {
    		DailogUtil.showTip(e.getMessage(), 3000);
		}
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
		    	remarkId.setText("");
		    }
		});
	}
	
	@Override
	public void onShow() {
		loadNewestBalance();
	}

	@Override
	public void onHide() {
	}

	@Override
	public boolean refreshData() {
		return false;
	}

	@Override
	public boolean startupInit() {
		return false;
	}
}
