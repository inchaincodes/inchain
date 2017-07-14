package org.inchain.wallet.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.core.AntifakeInfosResult;
import org.inchain.core.BroadcastResult;
import org.inchain.core.Definition;
import org.inchain.core.exception.AddressFormatException;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.store.AccountStore;
import org.inchain.store.ChainstateStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.AntifakeTransferTransaction;
import org.inchain.utils.Base58;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Hex;
import org.inchain.wallet.entity.MyProductListEntity;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MyProductListController implements SubPageController{

	private static final Logger log = LoggerFactory.getLogger(MyProductListController.class);
	
	//public Button transferId; 	//转让

	public TableView<MyProductListEntity> table;
	
	public TableColumn<MyProductListEntity, String> name;
	public TableColumn<MyProductListEntity, String> business;
	public TableColumn<MyProductListEntity, String> verifyCode;
	public TableColumn<MyProductListEntity, String> result;
	public TableColumn<MyProductListEntity, String> time;
	public TableColumn<MyProductListEntity, String> operating;
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	//transferId.setOnAction(e -> transfer());
    	
    	name.setCellValueFactory(new PropertyValueFactory<MyProductListEntity,String>("name"));
    	business.setCellValueFactory(new PropertyValueFactory<MyProductListEntity,String>("business"));
    	verifyCode.setCellValueFactory(new PropertyValueFactory<MyProductListEntity,String>("verifyCode"));
    	result.setCellValueFactory(new PropertyValueFactory<MyProductListEntity,String>("result"));
    	time.setCellValueFactory(new PropertyValueFactory<MyProductListEntity,String>("time"));
    	operating.setCellValueFactory(new PropertyValueFactory<MyProductListEntity,String>("operating"));
    	operating.setCellFactory(new javafx.util.Callback<TableColumn<MyProductListEntity,String>, TableCell<MyProductListEntity,String>>() {
			@Override
			public TableCell<MyProductListEntity, String> call(TableColumn<MyProductListEntity, String> status) {
				return new TableCell<MyProductListEntity,String>(){
					protected void updateItem(String code, boolean empty) {
						super.updateItem(code, empty);
						if(!empty && code != null) {
							Button transferId = new Button("转让");
							transferId.setOnAction(e -> transfer(code));
							setGraphic(transferId);
						} else {
							setGraphic(null);
						}
					}
				};
			}
		});
    	
    }
    
	@Override
	public void initDatas() {
		
		table.setItems(FXCollections.observableArrayList(new ArrayList<MyProductListEntity>()));
		
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		ChainstateStoreProvider chainstateStoreProvider = SpringContextUtils.getBean(ChainstateStoreProvider.class);
		
		List<TransactionStore> txList = accountKit.getTransactions();
		
		List<MyProductListEntity> dataList = new ArrayList<MyProductListEntity>();
		
		Set<String> antifakeCodes = new HashSet<String>();
		
		for (TransactionStore txs : txList) {
			Transaction tx = txs.getTransaction();
			if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY || tx.getType() == Definition.TYPE_ANTIFAKE_TRANSFER) {
				
				byte[] antifakeCodeBytes = null;
				boolean isOwner = true;				
				
				if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {
					AntifakeCodeVerifyTransaction avtx = (AntifakeCodeVerifyTransaction) tx;
					antifakeCodeBytes = avtx.getAntifakeCode();
					
				} else {
					AntifakeTransferTransaction atx = (AntifakeTransferTransaction) tx;
					antifakeCodeBytes = atx.getAntifakeCode();
					
				}
				
				String antifakceCodeHex = Hex.encode(antifakeCodeBytes);
				if(antifakeCodes.contains(antifakceCodeHex)) {
					continue;
				}
				antifakeCodes.add(antifakceCodeHex);
				
				byte[] owner = chainstateStoreProvider.getAntifakeCodeOwner(antifakeCodeBytes);
				if(owner != null && (accountKit.getDefaultAccount() == null || !Arrays.equals(accountKit.getDefaultAccount().getAddress().getHash(), owner))) {
					isOwner = false;
				}
				
				MyProductListEntity data = new MyProductListEntity();
				
				AntifakeInfosResult infoResult = accountKit.getProductAndBusinessInfosByAntifake(antifakeCodeBytes);
				
				
				data.setName(infoResult.getProductTx().getProduct().getName());
		    	data.setBusiness(infoResult.getBusiness().getAccountBody().getName());
		    	data.setVerifyCode(Base58.encode(antifakeCodeBytes));
		    	data.setResult("成功");
		    	data.setTime(DateUtil.convertDate(new Date(tx.getTime() * 1000)));
		    	if(isOwner) {
		    		data.setOperating(data.getVerifyCode());
		    	} else {
		    		data.setOperating(null);
		    	}
		    	dataList.add(data);
			}
		}
		
		Collections.reverse(dataList);
		
		ObservableList<MyProductListEntity> datas = FXCollections.observableArrayList(dataList);
    	table.setItems(datas);
    	
	}
	/*
	 * 转让
	 * */
	private void transfer(final String antifakeCode) {
		//检查信用值是否达标
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		AccountStore accountInfo = accountKit.getAccountInfo();
		if(accountInfo.getCert() < Configure.TRANSFER_ANTIFAKECODE_CREDIT) {
			DailogUtil.showTip("信用值达到" + Configure.TRANSFER_ANTIFAKECODE_CREDIT + "才可以转让商品，目前信用为" + accountInfo.getCert(), 5000);
			return;
		}
		
		URL url = getClass().getResource("/resources/template/transfer.fxml");
		FXMLLoader loader = new FXMLLoader(url);
		DailogUtil.showDailog(loader, "转让商品", new Callback() {
			@Override
			public void ok(Object param) {
				
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) param;
				
				TransferController controller = (TransferController) map.get("controller");
				
				String receiver = (String) map.get("receiver");
				String remark = (String) map.get("remark");
				
				AccountKit accountKit = SpringContextUtils.getBean(AccountKit.class);
				if(accountKit.accountIsEncrypted()) {
					decryptWallet(antifakeCode, accountKit, controller, receiver, remark);
					return;
				} else {
					saveDo(antifakeCode, controller, receiver, remark);
				}
			}
		});
	}
	
	/*
	 * 保存
	 */
	private void saveDo(String antifakeCode, TransferController controller, String receiver, String remark) {
		
		AccountKit accountKit = SpringContextUtils.getBean(AccountKit.class);
		
		try {
			BroadcastResult res = accountKit.transferAntifake(antifakeCode, receiver, remark, null);
			
			if(res.isSuccess()) {
				DailogUtil.showTip("转让成功！");
				controller.resetAndclose();
				initDatas();
			} else {
				DailogUtil.showTip(res.getMessage());
			}
		} catch (Exception e) {
			if(e instanceof AddressFormatException) {
				DailogUtil.showTip("接收地址不正确");
			} else {
				DailogUtil.showTip("出错了：" + e.getMessage());
			}
		}
	}
	
	private void decryptWallet(final String antifakeCode, final AccountKit accountKit, final TransferController controller, final String receiver, final String remark) {
		//解密账户
		URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
		FXMLLoader loader = new FXMLLoader(location);
		DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
			@Override
			public void ok(Object param) {
				if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
					new Thread() {
	    				public void run() {
	    					Platform.runLater(new Runnable() {
	    					    @Override
	    					    public void run() {
	    					    	try {
	    					    		saveDo(antifakeCode, controller, receiver, remark);
	    					    	} finally {
	    					    		accountKit.resetKeys();
	    					    	}
	    					    }
	    					});
	    				};
	    			}.start();
				}
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
