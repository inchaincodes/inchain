package org.inchain.wallet.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.inchain.core.Peer;
import org.inchain.core.TimeService;
import org.inchain.kit.InchainInstance;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.entity.NodeInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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

	List<NodeInfoEntity> list = new ArrayList<NodeInfoEntity>();
	
	public void initialize() {
		
		ip.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("ip"));
		version.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("version"));
		sort.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("sort"));
		time.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("time"));
		offsetTime.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("offsetTime"));
		duration.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity, String>("duration"));
		// types.setCellValueFactory(new PropertyValueFactory<NodeInfoEntity,
		// String>("types"));
	}

	@Override
	public void initDatas() {
		nodeChange();
	}
	
	private void nodeChange() {
		list.clear();
		List<Peer> peerList = InchainInstance.getInstance().getPeerKit().findAvailablePeers();
		for(Peer peer : peerList) {
			// 相差的秒数
			Date startTime,endTime;
			startTime = new Date(TimeService.currentTimeMillis());
			endTime = new Date(peer.getSendVersionMessageTime());
			long time = (startTime.getTime() - endTime.getTime()) / 1000;
			String offsetTime = "0";
			String ipString = peer.getPeerAddress().getAddr().toString().split("/")[1];
			if(peer.getTimeOffset() > 0) {
				offsetTime = "快"+Math.abs(peer.getTimeOffset())+"毫秒";
			} else if(peer.getTimeOffset() < 0) {
				offsetTime = "慢"+Math.abs(peer.getTimeOffset())+"毫秒";
			}
			
			list.add(new NodeInfoEntity(ipString,
					peer.getPeerVersionMessage().getSubVer(), "" + peer.getAddress().getPort(),
					"" + DateUtil.convertDate(new Date((TimeService.currentTimeMillis() + peer.getTimeOffset()))),
					offsetTime,DateUtil.analyzeTime(time, false) , ""));
		}
		number.setText(list.size()+"个");
		ObservableList<NodeInfoEntity> datas = FXCollections.observableArrayList(list);
		table.setItems(datas);
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
		return true;
	}
}
