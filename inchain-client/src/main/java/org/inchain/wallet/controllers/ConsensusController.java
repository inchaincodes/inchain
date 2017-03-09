package org.inchain.wallet.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.core.Result;
import org.inchain.core.Definition;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.listener.Listener;
import org.inchain.network.NetworkParams;
import org.inchain.store.AccountStore;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.Context;
import org.inchain.wallet.entity.ConensusEntity;
import org.inchain.wallet.utils.ConfirmDailog;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * 共识列表页面控制器
 * @author ln
 *
 */
public class ConsensusController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(ConsensusController.class);
	
	public TableView<ConensusEntity> table;
	
	public Label certNumberId;
	public Label statusLabelId;
	public Button buttonId;
	
	public TableColumn<ConensusEntity, Integer> status;
	public TableColumn<ConensusEntity, String> address;
	public TableColumn<ConensusEntity, Long> cert;
	public TableColumn<ConensusEntity, String> time;
	
	private List<AccountStore> consensusList;
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	
    	//status.setCellValueFactory(new PropertyValueFactory<ConensusEntity, Integer>("status"));
    	address.setCellValueFactory(new PropertyValueFactory<ConensusEntity, String>("address"));
    	cert.setCellValueFactory(new PropertyValueFactory<ConensusEntity, Long>("cert") {
    		@Override
    		public ObservableValue<Long> call(CellDataFeatures<ConensusEntity, Long> param) {
    			return new ReadOnlyObjectWrapper<Long>(param.getValue().getCert());
    		}
    	});
    	time.setCellValueFactory(new PropertyValueFactory<ConensusEntity, String>("time") {
    		@Override
    		public ObservableValue<String> call(CellDataFeatures<ConensusEntity, String> param) {
    			return new ReadOnlyObjectWrapper<String>(DateUtil.convertDate(new Date(param.getValue().getTime())));
    		}
    	});
    	
    	buttonId.setOnAction(e -> addOrDeleteConsensus());
    }
    
    /**
     * 初始化
     */
    public void initDatas() {
    	
    	if(log.isDebugEnabled()) {
    		log.debug("加载商家列表···");
    	}
    	
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	consensusList = accountKit.getConsensusAccounts();
    	
    	List<ConensusEntity> list = tx2Entity();
    	
    	ObservableList<ConensusEntity> datas = FXCollections.observableArrayList(list);
    	datas.sort(new Comparator<ConensusEntity>() {
			@Override
			public int compare(ConensusEntity o1, ConensusEntity o2) {
				return o2.getTime() > o1.getTime() ? 1 : -1;
			}
		});
    	
    	table.setItems(datas);
    	
    	//自己的共识信息
    	AccountStore accountStore = accountKit.getAccountInfo();
    	certNumberId.setText(String.valueOf(accountStore.getCert()));
    	
    	//当前共识状态，是否正在共识中
    	boolean consensusStatus = accountKit.checkConsensusing();
    	if(consensusStatus) {
    		statusLabelId.setText("您当前正在共识中···");
    		
    		buttonId.setText("退出共识");
			buttonId.setDisable(false);
    	} else {
    		if(accountStore.getCert() >= Configure.CONSENSUS_CREDIT) {
    			//可参与共识
    			statusLabelId.setText("您已达到参与共识所需信用 " + Configure.CONSENSUS_CREDIT + " , 可参与共识");
    			
    			buttonId.setText("申请共识");
    			buttonId.setDisable(false);
    		} else {
    			//不可参与共识
    			statusLabelId.setText("您离共识所需信用 " + Configure.CONSENSUS_CREDIT + " 还差 " + (Configure.CONSENSUS_CREDIT - accountStore.getCert()));
    			
    			buttonId.setText("申请共识");
    			buttonId.setDisable(true);
    		}
    	}
    }
    
    /**
     * 参加或者退出共识
     */
    public void addOrDeleteConsensus() {
    	
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	//当前共识状态，是否正在共识中
    	boolean consensusStatus = accountKit.checkConsensusing();
    	
    	String tip = consensusStatus ? "您当前正在共识中，确认要退出共识吗？" : "一旦参与到共识中，对您的本地环境稳定性要求很高，确认参与吗？";
    	ConfirmDailog dailog = new ConfirmDailog(Context.getMainStage(), tip);
    	dailog.setListener(new Listener() {
			@Override
			public void onComplete() {
				//账户是否加密
				if(accountKit.accountIsEncrypted()) {
					//解密账户
	    			URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
			        FXMLLoader loader = new FXMLLoader(location);
					DailogUtil.showDailog(loader, "输入钱包密码", new Runnable() {
						@Override
						public void run() {
							if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
								try {
									doAction(accountKit, consensusStatus);
								} finally {
									accountKit.resetKeys();
								}
							}
						}
					});
				} else {
					doAction(accountKit, consensusStatus);
				}
			}
		});
    	dailog.show();
    }

    /**
     * 参与或者退出共识
     * @param accountKit
     * @param consensusStatus
     */
    private void doAction(AccountKit accountKit, boolean consensusStatus) {
    	Result result = null;
		if(consensusStatus) {
			//取消共识(退出共识)
			result = accountKit.quitConsensus();
		} else {
			//注册共识
			result = accountKit.registerConsensus();
		}
		DailogUtil.showTip(result.getMessage(), Context.getMainStage());
		if(result.isSuccess()) {
			initDatas();
		}
	}
    
	private List<ConensusEntity> tx2Entity() {
		
		List<ConensusEntity> bes = new ArrayList<ConensusEntity>();
		
		if(consensusList == null || consensusList.size() == 0) {
			return bes;
		}
		
		NetworkParams network = InchainInstance.getInstance().getAppKit().getNetwork();
		
		for (AccountStore account : consensusList) {
			ConensusEntity entity = new ConensusEntity();
			entity.setTime(account.getCreateTime());
			entity.setAddress(new Address(network, account.getType(), account.getHash160()).getBase58());
			entity.setCert(account.getCert());
			
			bes.add(entity);
		}
		
		Collections.reverse(bes);	
		
		return bes;
	}
	
	@Override
	public void onShow() {
		initDatas();
	}

	@Override
	public void onHide() {
		
	}
}
