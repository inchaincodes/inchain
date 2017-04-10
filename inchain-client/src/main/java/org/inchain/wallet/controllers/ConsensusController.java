package org.inchain.wallet.controllers;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.account.Address;
import org.inchain.consensus.ConsensusMeeting;
import org.inchain.consensus.ConsensusPool;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.Result;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.listener.Listener;
import org.inchain.message.BlockHeader;
import org.inchain.network.NetworkParams;
import org.inchain.store.AccountStore;
import org.inchain.utils.ConsensusRewardCalculationUtil;
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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * 共识列表页面控制器
 * @author ln
 *
 */
public class ConsensusController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(ConsensusController.class);
	public static int nowStatus = 0 ; //0 没有参与共识  1 正在等待网络确认参与 2 正在参与共识
	public TableView<ConensusEntity> table;
	public Label certNumberId;
	public Label consensusNumberId;
	public Label statusLabelId;
	public Button buttonId;
	
	public TableColumn<ConensusEntity, Integer> status;
	public TableColumn<ConensusEntity, String> address;
	public TableColumn<ConensusEntity, Long> cert;
	public TableColumn<ConensusEntity, String> time;
	
	private List<AccountStore> consensusList;
	
	private int type = 0;
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	
//    	status.setCellValueFactory(new PropertyValueFactory<ConensusEntity, Integer>("status"));
    	address.setCellValueFactory(new PropertyValueFactory<ConensusEntity, String>("address"));
    	address.setCellFactory(new Callback<TableColumn<ConensusEntity, String>, TableCell<ConensusEntity, String>>() {
    		@Override 
    		public TableCell<ConensusEntity, String> call(TableColumn<ConensusEntity, String> tableColumn) {
    			return new TableCell<ConensusEntity, String>() {
    				@Override
    				protected void updateItem(String item, boolean empty) {
    					super.updateItem(item, empty);
    					if(item == null) {
    						setGraphic(null);
    					} else {
    						Label label = new Label(item);
    						label.setCursor(Cursor.HAND);
    					
    						label.setOnMouseMoved(new EventHandler<Event>() {
								public void handle(Event event) {
									label.setUnderline(true);
								}
							});
    						label.setOnMouseExited(new EventHandler<Event>() {
    							public void handle(Event event) {
    								label.setUnderline(false);
    							}
    						});
    						label.setOnMouseClicked(new EventHandler<MouseEvent>() {
								@Override
								public void handle(MouseEvent e) {
									StringSelection stsel = new StringSelection(label.getText());
									Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
									
									DailogUtil.showTip("复制成功", e.getScreenX(), e.getScreenY());
									e.consume();
								}
							});
    						setGraphic(label);
    					}
    				}
    			};
    		}
    	});
    	
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
    		log.debug("加载共识列表···");
    	}
    	
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	
    	//自己的共识信息
    	AccountStore accountStore = accountKit.getAccountInfo();
    	certNumberId.setText(String.valueOf(accountStore.getCert()));
    	
    	ConsensusPool consensusPool = SpringContextUtils.getBean(ConsensusPool.class);
    	consensusNumberId.setText("" + consensusPool.getCurrentConsensus());
    	
    	//当前共识状态，是否正在共识中
    	boolean consensusStatus = accountKit.checkConsensusing();
    	if (nowStatus == 1) {
    		if(!consensusStatus && type == 0) {
    			nowStatus = 0;
    		}
    		if(consensusStatus && type == 1) {
    			nowStatus = 2;
    		}
    		statusLabelId.setText("等待网络确认···");
    		buttonId.setDisable(true);
    	} else {
    		
        	if(consensusStatus) {
            		statusLabelId.setText("您当前正在共识中···");
            		nowStatus = 2;
            		buttonId.setText("退出共识");
            		buttonId.setDisable(false);
        	} else {
        		nowStatus = 0;
        		if(accountStore.getCert() >= Configure.CONSENSUS_CREDIT) {
					//共识保证金
					Coin recognizance = ConsensusRewardCalculationUtil.calculatRecognizance(consensusPool.getCurrentConsensus());
        			//可参与共识
        			statusLabelId.setText("参与共识所需信用 " + Configure.CONSENSUS_CREDIT + " , 共识需要提交保证金 " + recognizance.toText() + " INS");
        			//余额
        			Coin balance = accountKit.getBalance();
        			if(balance.isLessThan(recognizance)) {
        				buttonId.setText("保证金不足");
        				buttonId.setDisable(true);
        			} else {
	        			buttonId.setText("申请共识");
	        			buttonId.setDisable(false);
        			}
        		} else {
        			//不可参与共识
        			statusLabelId.setText("您离共识所需信用 " + Configure.CONSENSUS_CREDIT + " 还差 " + (Configure.CONSENSUS_CREDIT - accountStore.getCert()));
        			
        			buttonId.setText("申请共识");
        			buttonId.setDisable(true);
        		}
        	}
    	}
    	
    	List<AccountStore> consensusListTemp = accountKit.getConsensusAccounts();
    	if(hashChange(consensusListTemp)) {
    	
    		consensusList = consensusListTemp;
    		
	    	List<ConensusEntity> list = tx2Entity();
	    	
	    	ObservableList<ConensusEntity> datas = FXCollections.observableArrayList(list);
	    	datas.sort(new Comparator<ConensusEntity>() {
				@Override
				public int compare(ConensusEntity o1, ConensusEntity o2) {
					return o2.getTime() > o1.getTime() ? 1 : -1;
				}
			});
	    	
	    	table.setItems(datas);
    	}
    }
    
    private boolean hashChange(List<AccountStore> consensusListTemp) {
    	if(consensusListTemp == null || consensusList == null || consensusListTemp.size() != consensusList.size()) {
    		return true;
    	}
    	for (AccountStore accountStore : consensusList) {
    		boolean exist = false;
			for (AccountStore accountStoreTemp : consensusListTemp) {
				if(Arrays.equals(accountStore.getHash160(), accountStoreTemp.getHash160()) && accountStore.getCert() == accountStoreTemp.getCert()) {
					exist = true;
					break;
				}
			}
			if(!exist) {
				return true;
			}
		}
		return false;
	}

	/**
     * 参加或者退出共识
     */
    public void addOrDeleteConsensus() {
    	AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
    	//当前共识状态，是否正在共识中
    	boolean consensusStatus = accountKit.checkConsensusing();
    	
    	String tip = null;
    	if(consensusStatus) {
    		tip = "您当前正在共识中，确认要退出共识吗？";
    	} else {
    		BlockHeader bestBlockHeader = InchainInstance.getInstance().getAppKit().getNetwork().getBestBlockHeader();
    		Coin recognizance = ConsensusRewardCalculationUtil.calculatRecognizance(bestBlockHeader.getPeriodCount());
    		tip = "参与共识会扣除 " + recognizance.toText() + "保证金，将在退出共识时返还，确定继续吗？";
    	}
    	ConfirmDailog dailog = new ConfirmDailog(Context.getMainStage(), tip,1);
    	dailog.setListener(new Listener() {
			@Override
			public void onComplete() {
				//账户是否加密
				if(accountKit.accountIsEncrypted()) {
					//解密账户
	    			URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
			        FXMLLoader loader = new FXMLLoader(location);
					DailogUtil.showDailog(loader, "输入钱包密码",460,250, new org.inchain.wallet.utils.Callback() {
						@Override
						public void ok(Object param) {
							if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
								try {
									type = doAction(accountKit, consensusStatus);
								} finally {
									resetAccount(accountKit, type);
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

    /*
     * 重置账户
     */
    private void resetAccount(AccountKit accountKit, int type) {
		//如果是注册共识，那么延迟重置账户
		if(type == 0) {
			accountKit.resetKeys();
		} else {
			new Thread() {
				public void run() {
					//延迟重置账户
					ConsensusMeeting consensusMeeting = SpringContextUtils.getBean(ConsensusMeeting.class);
					consensusMeeting.waitMining();
					accountKit.resetKeys();
				};
			}.start();
		}
	}
    
    /*
     * 参与或者退出共识
     * @param accountKit
     * @param consensusStatus
     * @return int 0取消共识，1注册共识
     */
    private int doAction(AccountKit accountKit, boolean consensusStatus) {
    	Result result = null;
    
		if(consensusStatus) {
			//取消共识(退出共识)
			result = accountKit.quitConsensus();
			type = 0;
		} else {
			//注册共识
			result = accountKit.registerConsensus();
			type = 1;
		}    	

		DailogUtil.showTip(result.getMessage(), Context.getMainStage(),3000l);
		if(result.isSuccess()) {
			nowStatus = 1;
			initDatas();
		} else {
			if(consensusStatus) {
				nowStatus = 2;
			} else {
				nowStatus = 0;
			}
			initDatas();
		}
		return type;
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
		//initDatas();
	}

	@Override
	public void onHide() {
		
	}

	@Override
	public boolean refreshData() {
		return true;
	}

	@Override
	public boolean startupInit() {
		return false;
	}
}
