package org.inchain.wallet.controllers;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.core.AntifakeCode;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.KeyValuePair;
import org.inchain.core.Product;
import org.inchain.core.Product.ProductType;
import org.inchain.core.exception.AccountEncryptedException;
import org.inchain.core.exception.VerificationException;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Utils;
import org.inchain.validator.TransactionValidator;
import org.inchain.validator.TransactionValidatorResult;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

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
    	antifakeCodeId.setBackground(Background.EMPTY);
    	Image reset = new Image(getClass().getResourceAsStream("/images/reset_icon.png"));
    	resetButId.setGraphic(new ImageView(reset));
    	Image verify = new Image(getClass().getResourceAsStream("/images/verify_icon.png"));
    	verifyButId.setGraphic(new ImageView(verify));
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

    public void verifyDo(AccountKit accountKit, String antifakeCode) throws VerificationException {
    	try{
	    	//解析防伪码字符串
			AntifakeCode Base58AntifakeCode = AntifakeCode.base58Decode(antifakeCode);
			
			//判断验证码是否存在
			ChainstateStoreProvider chainstateStoreProvider = SpringContextUtils.getBean("chainstateStoreProvider");
			byte[] txBytes = chainstateStoreProvider.getBytes(Base58AntifakeCode.getAntifakeTx().getBytes());
			if(txBytes == null) {
				throw new VerificationException("防伪码不存在");
			}
			BlockStoreProvider blockStoreProvider = SpringContextUtils.getBean("blockStoreProvider");
			TransactionStore txStore = blockStoreProvider.getTransaction(txBytes);
			//必须存在
			if(txStore == null) {
				throw new VerificationException("防伪码生产交易不存在");
			}
			
			Transaction fromTx = txStore.getTransaction();
			//交易类型必须是防伪码生成交易
			if(fromTx.getType() != Definition.TYPE_ANTIFAKE_CODE_MAKE) {
				throw new VerificationException("防伪码类型错误");
			}
			AntifakeCodeMakeTransaction codeMakeTx = (AntifakeCodeMakeTransaction) fromTx;
			
			//验证防伪码是否已经被验证了
			//保证该防伪码没有被验证
			byte[] txStatus = codeMakeTx.getHash().getBytes();
			byte[] txIndex = new byte[txStatus.length + 1];
			
			System.arraycopy(txStatus, 0, txIndex, 0, txStatus.length);
			txIndex[txIndex.length - 1] = 0;
			
			byte[] status = chainstateStoreProvider.getBytes(txIndex);
			if(status == null) {
				
				showProfuctInfo(blockStoreProvider, codeMakeTx,"验证失败，该防伪码已被验证");
				return;
				//throw new VerificationException("验证失败，该防伪码已被验证");
				
			}
			//防伪码验证脚本
			Script inputSig = ScriptBuilder.createAntifakeInputScript(Base58AntifakeCode.getCertAccountTx(), Base58AntifakeCode.getSigns());
			
			TransactionInput input = new TransactionInput((TransactionOutput) codeMakeTx.getOutput(0));
			input.setScriptSig(inputSig);
			NetworkParams network =SpringContextUtils.getBean("network");
			AntifakeCodeVerifyTransaction tx = new AntifakeCodeVerifyTransaction(network, input);
			
			//验证账户，不能是认证账户
			Account systemAccount = accountKit.getSystemAccount();
			if(systemAccount == null) {
				throw new VerificationException("账户不存在，不能验证");
			}
			
			//添加奖励输出
			Coin rewardCoin = codeMakeTx.getRewardCoin();
			if(rewardCoin != null && rewardCoin.isGreaterThan(Coin.ZERO)) {
				tx.addOutput(rewardCoin, systemAccount.getAddress());
			}
			
			//签名即将广播的信息
			tx.sign(systemAccount);
			
			//验证成功才广播
			tx.verify();
			tx.verifyScript();
			
			//验证交易合法才广播
			//这里面同时会判断是否被验证过了
			TransactionValidator transactionValidator = SpringContextUtils.getBean("transactionValidator");
			TransactionValidatorResult rs = transactionValidator.valDo(tx, null).getResult();
			if(!rs.isSuccess()) {
				if(rs.getErrorCode() == TransactionValidatorResult.ERROR_CODE_USED) {
					showProfuctInfo(blockStoreProvider, codeMakeTx,"验证失败，该防伪码已被验证");
					return;
				}
			}

			//加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
			boolean success = MempoolContainer.getInstace().add(tx);
			if(!success) {
				showProfuctInfo(blockStoreProvider, codeMakeTx,"验证失败，该防伪码已被验证");
				return;
			}		
			try {
				PeerKit peerKit = SpringContextUtils.getBean("peerKit");
				BroadcastResult result = peerKit.broadcast(tx).get();
				
				//等待广播回应
				if(result.isSuccess()) {
					//更新交易记录
					TransactionStoreProvider transactionStoreProvider = SpringContextUtils.getBean("transactionStoreProvider");
					transactionStoreProvider.processNewTransaction(new TransactionStore(network, tx));
					showProfuctInfo(blockStoreProvider, codeMakeTx,"恭喜您，验证通过");
					//System.out.println("恭喜您，验证通过"+product.getContents());
				}
			} catch (Exception e) {
				log.debug("广播失败，失败信息：" + e.getMessage(), e);
			}
			
    	}catch (Exception e) {
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
    /**
     *显示验证商品信息 
     * */
	private void showProfuctInfo(BlockStoreProvider blockStoreProvider, AntifakeCodeMakeTransaction codeMakeTx,String message) {
		VBox content = new VBox();
		VBox body = new VBox();
		HBox result = new HBox();
		Label name,value;
		value = new Label(message);
		if(message.equals("恭喜您，验证通过")) {
			value.setStyle("-fx-text-fill:#27d454;-fx-font-size:16;");
		}else{
			value.setStyle("-fx-text-fill:red;-fx-font-size:16;");
		}
		result.getChildren().add(value);
		
		result.setStyle("-fx-padding:0 0 0 100;");
	
		body.getChildren().add(result);
		//获取产品信息
		TransactionStore productTxStore = blockStoreProvider.getTransaction(codeMakeTx.getProductTx().getBytes());
		ProductTransaction productTransaction = (ProductTransaction) productTxStore.getTransaction();
		Product product = productTransaction.getProduct();
		List<KeyValuePair> bodyContents = product.getContents();
		
		for (KeyValuePair keyValuePair : bodyContents) {
			
			HBox item= new HBox();
			name = new Label(keyValuePair.getKeyName()+":");
			item.getChildren().add(name);
			value.setMaxWidth(300);
			if (ProductType.from(keyValuePair.getKey()) == ProductType.CREATE_TIME){
				value = new Label(DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0))));
			}else{
				value = new Label(keyValuePair.getValueToString());
			}
			Tooltip tooltip = new Tooltip(value.getText());
			tooltip.setFont(Font.font(14));
			tooltip.setMaxWidth(480);
			tooltip.setWrapText(true);
			tooltip.setStyle("-fx-padding:10;");
			value.setTooltip(tooltip);
			item.getChildren().add(value);
			
			item.setStyle("-fx-padding:0 0 10 10;");
			content.getChildren().add(item);
		}
		content.setStyle("-fx-padding:20 0 0 120;");
		body.getChildren().add(content);
		DailogUtil.showDailog(body, "验证结果");
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

	@Override
	public boolean refreshData() {
		return false;
	}

	@Override
	public boolean startupInit() {
		return false;
	}
}
