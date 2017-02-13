package org.inchain.wallet.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.inchain.account.Address;
import org.inchain.kit.InchainInstance;
import org.inchain.network.NetworkParams;
import org.inchain.store.AccountStore;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.entity.ConensusEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * 共识列表页面控制器
 * @author ln
 *
 */
public class ConsensusRecordController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(ConsensusRecordController.class);
	
	public TableView<ConensusEntity> table;
	
	public TableColumn<ConensusEntity, Integer> status;
	public TableColumn<ConensusEntity, String> address;
	public TableColumn<ConensusEntity, Long> cert;
	public TableColumn<ConensusEntity, String> time;
	
	private List<AccountStore> consensusList;
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	status.setCellValueFactory(new PropertyValueFactory<ConensusEntity, Integer>("status"));
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
    }
    
    /**
     * 初始化
     */
    public void initDatas() {
    	
    	if(log.isDebugEnabled()) {
    		log.debug("加载商家列表···");
    	}
    	
    	consensusList = InchainInstance.getInstance().getAccountKit().getConsensusAccounts();
    	
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
