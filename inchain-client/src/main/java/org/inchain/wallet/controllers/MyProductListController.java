package org.inchain.wallet.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.inchain.wallet.entity.MyProductListEntity;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.DailogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    	List<MyProductListEntity> test = new ArrayList<MyProductListEntity>();
    	MyProductListEntity data = new MyProductListEntity();
    	data.setBusiness("s");
    	data.setName("d");
    	data.setVerifyCode("sas");
    	data.setResult("11");
    	data.setTime("2017");
    	data.setOperating("operating");
    	test.add(data);
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
					protected void updateItem(String status, boolean empty) {
						super.updateItem(status, empty);
						if(!empty) {
							Button transferId = new Button("转让");
							transferId.setOnAction(e -> transfer());
							setGraphic(transferId);
						}
					}
				};
			}
		});
    	ObservableList<MyProductListEntity> datas = FXCollections.observableArrayList(test);
    	table.setItems(datas);
    }
    
	@Override
	public void initDatas() {
	}
	/*
	 * 转让
	 * */
	private void transfer() {
		URL url = getClass().getResource("/resources/template/transfer.fxml");
		FXMLLoader loader = new FXMLLoader(url);
		DailogUtil.showDailog(loader, "转让商品",new Callback() {
			@Override
			public void ok(Object param) {
				DailogUtil.showTip("转让成功！");
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
