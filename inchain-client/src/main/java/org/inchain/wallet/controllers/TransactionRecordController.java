package org.inchain.wallet.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.inchain.account.Account;
import org.inchain.account.AccountBody.ContentType;
import org.inchain.account.AccountBody.KeyValuePair;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.TimeHelper;
import org.inchain.kit.InchainInstance;
import org.inchain.mempool.MempoolContainerMap;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.CertAccountRegisterTransaction;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionDefinition;
import org.inchain.transaction.TransactionOutput;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.entity.DetailValue;
import org.inchain.wallet.entity.DetailValueCell;
import org.inchain.wallet.entity.TransactionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

/**
 * 交易记录页面控制器
 * @author ln
 *
 */
public class TransactionRecordController implements SubPageController {
	
	private static final Logger log = LoggerFactory.getLogger(TransactionRecordController.class);
	
	public TableView<TransactionEntity> table;
	public TableColumn<TransactionEntity, Long> status;
	public TableColumn<TransactionEntity, String> type;
	public TableColumn<TransactionEntity, DetailValue> detail;
	public TableColumn<TransactionEntity, String> amount;
	public TableColumn<TransactionEntity, String> time;
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	status.setCellValueFactory(new PropertyValueFactory<TransactionEntity, Long>("status"));
    	type.setCellValueFactory(new PropertyValueFactory<TransactionEntity, String>("type"));
    	detail.setCellValueFactory(new PropertyValueFactory<TransactionEntity, DetailValue>("detail"));
    	detail.setCellFactory(new Callback<TableColumn<TransactionEntity, DetailValue>, TableCell<TransactionEntity, DetailValue>>() {
	    	@Override public TableCell<TransactionEntity, DetailValue> call(TableColumn<TransactionEntity, DetailValue> tableColumn) {
	    		return new DetailValueCell();
	    	}
	    });
    	amount.setCellValueFactory(new PropertyValueFactory<TransactionEntity, String>("amount"));
    	time.setCellValueFactory(new PropertyValueFactory<TransactionEntity, String>("time"));
    }
    
    /**
     * 初始化
     */
    public void initDatas() {
    	
    	if(log.isDebugEnabled()) {
    		log.debug("加载交易数据···");
    	}
    	
    	List<TransactionStore> txs = InchainInstance.getInstance().getAccountKit().getTransactions();
    	
    	List<TransactionEntity> list = new ArrayList<TransactionEntity>();
    	
    	tx2Entity(txs, list);
    	
    	ObservableList<TransactionEntity> datas = FXCollections.observableArrayList(list);
    	datas.sort(new Comparator<TransactionEntity>() {
			@Override
			public int compare(TransactionEntity o1, TransactionEntity o2) {
				if(o1.getTime() == null || o2.getTime() == null) {
					return 0;
				} else {
					return o2.getTime().compareTo(o1.getTime());
				}
			}
		});
    	
    	table.setItems(datas);
    }

	private void tx2Entity(List<TransactionStore> txsList, List<TransactionEntity> list) {
		if(txsList != null && txsList.size() > 0) {
			//翻转数组
			Collections.reverse(txsList);
			
			//当前最新区块高度
			NetworkParams network = InchainInstance.getInstance().getAppKit().getNetwork();
			long bestBlockHeight = network.getBestBlockHeight();
			
			List<Account> accounts = InchainInstance.getInstance().getAccountKit().getAccountList();
			
			for (TransactionStore txs : txsList) {
				
				Transaction tx = txs.getTransaction();
				
				String type = null, detail = "", amount = null, time = null;
				DetailValue detailValue = new DetailValue();
				
				if(tx.getType() == TransactionDefinition.TYPE_COINBASE || 
						tx.getType() == TransactionDefinition.TYPE_PAY) {
					
					type = "转入";
					
					//是否是转出
					boolean isSendout = false;
					
					List<Input> inputs = tx.getInputs();
					if(tx.getType() != TransactionDefinition.TYPE_COINBASE && inputs != null && inputs.size() > 0) {
						for (Input input : inputs) {
							TransactionOutput from = input.getFrom();
							TransactionStore fromTx = InchainInstance.getInstance().getAccountKit().getTransaction(from.getParent().getHash());
							
							Transaction ftx = null;
							if(fromTx == null) {
								//交易不存在区块里，那么应该在内存里面
								ftx = MempoolContainerMap.getInstace().get(from.getParent().getHash());
							} else {
								ftx = fromTx.getTransaction();
							}
							if(ftx == null) {
								continue;
							}
							Output fromOutput = ftx.getOutput(from.getIndex());
							
							Script script = fromOutput.getScript();
							for (Account account : accounts) {
								if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, account.getAddress().getHash160())) {
									isSendout = true;
									break;
								}
							}
							
							if(script.isSentToAddress()) {
								detail += "\r\n" + new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58()+"(-"+Coin.valueOf(fromOutput.getValue()).toText()+")";
							}
						}
					}
					
					if(!"".equals(detail)) {
						detail += "\r\n -> ";
					}
					
					List<Output> outputs = tx.getOutputs();
					
					Coin fee = Coin.valueOf(outputs.get(0).getValue());
					
					for (Output output : outputs) {
						Script script = output.getScript();
						if(script.isSentToAddress()) {
							detail += "\r\n" + new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58()+"(+"+Coin.valueOf(output.getValue()).toText()+")";
							if(tx.getLockTime() == -1 || output.getLockTime() == -1) {
								detail += "(永久锁定)";
							} else if(((tx.getLockTime() > TransactionDefinition.LOCKTIME_THRESHOLD && tx.getLockTime() > TimeHelper.currentTimeMillis()) ||
									(tx.getLockTime() < TransactionDefinition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight)) ||
									((output.getLockTime() > TransactionDefinition.LOCKTIME_THRESHOLD && output.getLockTime() > TimeHelper.currentTimeMillis()) ||
											(output.getLockTime() < TransactionDefinition.LOCKTIME_THRESHOLD && output.getLockTime() > bestBlockHeight))) {
								long lockTime;
								if(tx.getLockTime() > output.getLockTime()) {
									lockTime = tx.getLockTime();
								} else {
									lockTime = output.getLockTime();
								}
								if(lockTime > TransactionDefinition.LOCKTIME_THRESHOLD) {
									detail += "("+DateUtil.convertDate(new Date(lockTime))+"后可用)";
								} else {
									detail += "(区块高度达到"+lockTime+"时可用)";
								}
							}
						}
					}
					
					if(isSendout) {
						type = "转出";
					}
					amount = fee.toText();
				} else if(tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER || 
						tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_UPDATE) {
					//认证账户注册
					CertAccountRegisterTransaction crt = (CertAccountRegisterTransaction) tx;
					type = tx.getType() == TransactionDefinition.TYPE_CERT_ACCOUNT_REGISTER ? "账户注册" : "修改信息";
					
					List<KeyValuePair> bodyContents = crt.getBody().getContents();
					for (KeyValuePair keyValuePair : bodyContents) {
						if(ContentType.from(keyValuePair.getKey()) == ContentType.LOGO) {
							//图标
							detailValue.setImg(keyValuePair.getValue());
						} else {
							if(!"".equals(detail)) {
								detail += "\r\n";
							}
							detail += keyValuePair.getKeyName()+" : " + keyValuePair.getValueToString();
						}
					}
					amount = "-";
				} else if(tx.getType() == TransactionDefinition.TYPE_REG_CONSENSUS || 
						tx.getType() == TransactionDefinition.TYPE_REM_CONSENSUS) {
					type = tx.getType() == TransactionDefinition.TYPE_REG_CONSENSUS ? "注册共识" : "退出共识";

					detail = "-";
					amount = "-";
				}
				
				time = DateUtil.convertDate(new Date(tx.getTime()), "yyyy-MM-dd HH:mm:ss.SSS");
				
				detailValue.setValue(detail);
				list.add(new TransactionEntity(bestBlockHeight - txs.getHeight() + 1, type, detailValue, amount, time));
			}
		}
	}
	
	@Override
	public void onShow() {
	}

	@Override
	public void onHide() {
	}
}
