package org.inchain.wallet.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.inchain.core.Peer;
import org.inchain.core.TimeService;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AppKit;
import org.inchain.kits.PeerKit;
import org.inchain.listener.ConnectionChangedListener;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.entity.NodeInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * 节点信息
 * 
 * @author cj
 *
 */
public class NodeInfoController implements SubPageController {

	private static final Logger log = LoggerFactory.getLogger(NodeInfoController.class);

	public Label number;

	public TableView<NodeInfoEntity> table;
	public TableColumn<NodeInfoEntity, String> ip;
	public TableColumn<NodeInfoEntity, String> version;
	public TableColumn<NodeInfoEntity, String> sort;
	public TableColumn<NodeInfoEntity, String> time;
	public TableColumn<NodeInfoEntity, String> offsetTime;
	public TableColumn<NodeInfoEntity, String> duration;
	//public TableColumn<NodeInfoEntity, String> types;

	AppKit appKit;
	PeerKit peerKit;
	List<NodeInfoEntity> list = new ArrayList<NodeInfoEntity>();
	
	public void initialize() {
		initListeners();
		ip.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("ip"));
		version.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("version"));
		sort.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("sort"));
		time.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("time"));
		offsetTime.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("offsetTime"));
		duration.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("duration"));
		// types.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity,
		// String>("types"));
	}

	/*
	 * 初始化数据变化监听器
	 */
	private void initListeners() {

		// 注入区块变化监听器
		InchainInstance instance = InchainInstance.getInstance();
		appKit = instance.getAppKit();
		peerKit = instance.getPeerKit();
		// 节点变化
		NodeChange();
	}

	private void NodeChange() {
		new Thread() {
			public void run() {
				while (true) {
					try {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								list.clear();
								List<Peer> peerList = peerKit.findAvailablePeers();
								for(Peer peer : peerList) {
									// 相差的秒数
									Date startTime,endTime;
									startTime = new Date(TimeService.currentTimeMillis());
									endTime = new Date(peer.getSendVersionMessageTime());
									long time = (startTime.getTime() - endTime.getTime()) / 1000;
									String ipString = peer.getPeerAddress().getAddr().toString().split("/")[1];
									list.add(new NodeInfoEntity(ipString,
											"" + peer.getNetwork().getSystemAccountVersion(), "" + peer.getAddress().getPort(),
											"" + DateUtil.convertDate(new Date((TimeService.currentTimeSeconds()+peer.getTimeOffset()) * 1000)),
											"" + peer.getTimeOffset(),DateUtil.analyzeTime(time, false) , ""));
								}
								number.setText(list.size()+"个");
								ObservableList<NodeInfoEntity> datas = FXCollections.observableArrayList(list);
								table.setItems(datas);
							}
						});
						Thread.sleep(1000l);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	@Override
	public void initDatas() {

	}

	@Override
	public void onShow() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onHide() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean refreshData() {
		// TODO Auto-generated method stub
		return true;
	}
}
