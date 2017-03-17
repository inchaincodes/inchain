package org.inchain.wallet.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.inchain.Configure;
import org.inchain.account.Account;
import org.inchain.account.AccountBody.ContentType;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.KeyValuePair;
import org.inchain.core.NotBroadcastBlockViolationEvidence;
import org.inchain.core.Product;
import org.inchain.core.Product.ProductType;
import org.inchain.crypto.Sha256Hash;
import org.inchain.core.TimeService;
import org.inchain.core.ViolationEvidence;
import org.inchain.kit.InchainInstance;
import org.inchain.mempool.MempoolContainer;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.Input;
import org.inchain.transaction.Output;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionOutput;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeCodeVerifyTransaction;
import org.inchain.transaction.business.CertAccountRegisterTransaction;
import org.inchain.transaction.business.CreditTransaction;
import org.inchain.transaction.business.GeneralAntifakeTransaction;
import org.inchain.transaction.business.ProductTransaction;
import org.inchain.transaction.business.ViolationTransaction;
import org.inchain.utils.DateUtil;
import org.inchain.utils.Utils;
import org.inchain.wallet.Constant;
import org.inchain.wallet.entity.DetailValue;
import org.inchain.wallet.entity.DetailValueCell;
import org.inchain.wallet.entity.TransactionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
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
    	status.setCellFactory(new Callback<TableColumn<TransactionEntity,Long>, TableCell<TransactionEntity,Long>>() {
			public TableCell<TransactionEntity, Long> call(TableColumn<TransactionEntity, Long> param) {
				return new TableCell<TransactionEntity, Long>(){
					
					protected void updateItem(Long item, boolean empty) {
						super.updateItem(item, empty);
						Label status_icon = new Label();
						Image icon;
						Tooltip tip = new Tooltip();
					
						if (item == null || empty) {
							setGraphic(null);
							return ;
						} 
						if (item.longValue() >= Constant.CONFIRM_NUMBER) {
							icon = new Image("/images/confirmed.png");
							tip = new Tooltip("已确认交易\n"+"确认数为"+item);
						} else {
							icon = new Image("/images/unconfirmed.png");
							tip = new Tooltip("交易待确认\n"+"经过"+ item +"次确认");
						}
						tip.setFont(Font.font(14));
						tip.setWrapText(true);
						status_icon = new Label(null, new ImageView(icon));
						status_icon.setTooltip(tip);
						setGraphic(status_icon);
					}
				};
			}
		});
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
//    	if(1==1)return;
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
				if(o1.getTime() == null || o2.getTime() == null || o2.getTime().equals(o1.getTime())) {
					return o2.getTxHash().compareTo(o1.getTxHash());
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
				
				//是否是转出
				boolean isSendout = false;
				
				Transaction tx = txs.getTransaction();
				
				String type = null, detail = "", amount = null, time = null;
				DetailValue detailValue = new DetailValue();
				
				if(tx.getType() == Definition.TYPE_COINBASE || 
						tx.getType() == Definition.TYPE_PAY) {
					
					if(tx.getType() == Definition.TYPE_COINBASE) {
						type = "共识奖励";
					} else {
						type = "现金交易";
					}
					
					String inputAddress = null;
					String outputAddress = null;
					
					List<Input> inputs = tx.getInputs();
					if(tx.getType() != Definition.TYPE_COINBASE && inputs != null && inputs.size() > 0) {
						for (Input input : inputs) {
							TransactionOutput from = input.getFrom();
							TransactionStore fromTx = InchainInstance.getInstance().getAccountKit().getTransaction(from.getParent().getHash());
							
							Transaction ftx = null;
							if(fromTx == null) {
								//交易不存在区块里，那么应该在内存里面
								ftx = MempoolContainer.getInstace().get(from.getParent().getHash());
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
								inputAddress = new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58();
								
//								detail += "\r\n" + new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58()+"(-"+Coin.valueOf(fromOutput.getValue()).toText()+")";
							}
						}
					}
					
//					if(!"".equals(detail)) {
//						detail += "\r\n -> ";
//					}
					
					List<Output> outputs = tx.getOutputs();
					
					for (Output output : outputs) {
						Script script = output.getScript();
						if(script.isSentToAddress()) {
							outputAddress = new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58();
							
//							detail += "\r\n" + new Address(network, script.getAccountType(network), script.getChunks().get(2).data).getBase58()+"(+"+Coin.valueOf(output.getValue()).toText()+")";
							if(tx.getLockTime() == -1 || output.getLockTime() == -1) {
								detail += "(永久锁定)";
							} else if(((tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > TimeService.currentTimeMillis()) ||
									(tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight)) ||
									((output.getLockTime() > Definition.LOCKTIME_THRESHOLD && output.getLockTime() > TimeService.currentTimeMillis()) ||
											(output.getLockTime() < Definition.LOCKTIME_THRESHOLD && output.getLockTime() > bestBlockHeight))) {
								long lockTime;
								if(tx.getLockTime() > output.getLockTime()) {
									lockTime = tx.getLockTime();
								} else {
									lockTime = output.getLockTime();
								}
								if(lockTime > Definition.LOCKTIME_THRESHOLD) {
									detail += "("+DateUtil.convertDate(new Date(lockTime))+"后可用)";
								} else {
									detail += "(区块高度达到"+lockTime+"时可用)";
								}
							}
						}
					}
					
					if(isSendout) {
						detail = "转给 "+outputAddress+"";
					} else {
						//接收，判断是否是共识奖励
						if(tx.getType() != Definition.TYPE_COINBASE) {
							detail = "来自 "+inputAddress+"";
						} else {
							detail = outputAddress+" (+"+Coin.valueOf(outputs.get(0).getValue()).toText()+")";
						}
					}
				} else if(tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER || 
						tx.getType() == Definition.TYPE_CERT_ACCOUNT_UPDATE) {
					//认证账户注册
					CertAccountRegisterTransaction crt = (CertAccountRegisterTransaction) tx;
					type = tx.getType() == Definition.TYPE_CERT_ACCOUNT_REGISTER ? "账户注册" : "修改信息";
					
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
				} else if(tx.getType() == Definition.TYPE_REG_CONSENSUS || 
						tx.getType() == Definition.TYPE_REM_CONSENSUS) {
					type = tx.getType() == Definition.TYPE_REG_CONSENSUS ? "注册共识" : "退出共识";

					detail = "-";
				} else if(tx.getType() == Definition.TYPE_CREATE_PRODUCT) {
					
					ProductTransaction ptx = (ProductTransaction) tx;
					
					type = "创建商品";
					
					List<KeyValuePair> bodyContents = ptx.getProduct().getContents();
					for (KeyValuePair keyValuePair : bodyContents) {
						if(!"".equals(detail)) {
							detail += "\r\n";
						}
						if(ProductType.from(keyValuePair.getKey()) == ProductType.CREATE_TIME) {
							//时间
							detail += keyValuePair.getKeyName()+" : " + DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)));
						} else {
							detail += keyValuePair.getKeyName()+" : " + keyValuePair.getValueToString();
						}
					}
					
				} else if(tx.getType() == Definition.TYPE_GENERAL_ANTIFAKE) {

					GeneralAntifakeTransaction gtx = (GeneralAntifakeTransaction) tx;
					
					type = "防伪验证";
					
					Product product = null;
					
					if(gtx.getProduct() != null) {
						product = gtx.getProduct();
					}
					if(gtx.getProductTx() != null) {
						TransactionStore ptx = InchainInstance.getInstance().getAccountKit().getTransaction(gtx.getProductTx());
						//必要的NPT验证
						if(ptx == null) {
							log.error("防伪验证，商品信息没有找到，将跳过不显示该交易");
							continue;
						}
						product = ((ProductTransaction)ptx.getTransaction()).getProduct();
					}
					
					if(product == null) {
						log.error("防伪验证，商品没有找到，将跳过不显示该交易");
						continue;
					}
					
					List<KeyValuePair> bodyContents = product.getContents();
					for (KeyValuePair keyValuePair : bodyContents) {
						if(!"".equals(detail)) {
							detail += "\r\n";
						}
						if(ProductType.from(keyValuePair.getKey()) == ProductType.CREATE_TIME) {
							//时间
							detail += keyValuePair.getKeyName()+" : " + DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)));
						} else {
							detail += keyValuePair.getKeyName()+" : " + keyValuePair.getValueToString();
						}
					}
				} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_VERIFY) {

					AntifakeCodeVerifyTransaction atx = (AntifakeCodeVerifyTransaction) tx;
					
					atx.verifyScript();
					
					type = "防伪验证";
					
					byte[] makeCodeTxBytes = InchainInstance.getInstance().getAccountKit().getChainstate(atx.getAntifakeCode().getBytes());
					//必要的NPT验证
					if(makeCodeTxBytes == null) {
						log.error("防伪码对应的生产记录没有找到，没有找到，将跳过不显示该交易");
						continue;
					}
					TransactionStore makeCodeTxStore = InchainInstance.getInstance().getAccountKit().getTransaction(Sha256Hash.wrap(makeCodeTxBytes));
					if(makeCodeTxStore == null) {
						log.error("防伪码对应的生产记录没有找到，没有找到");
						continue;
					}
					AntifakeCodeMakeTransaction makeCodeTx = (AntifakeCodeMakeTransaction)makeCodeTxStore.getTransaction();
					
					TransactionStore productTxStore = InchainInstance.getInstance().getAccountKit().getTransaction(makeCodeTx.getProductTx());
					if(productTxStore == null) {
						log.error("防伪验证，商品没有找到，将跳过不显示该交易");
						continue;
					}
					
					List<KeyValuePair> bodyContents = ((ProductTransaction)productTxStore.getTransaction()).getProduct().getContents();
					for (KeyValuePair keyValuePair : bodyContents) {
						if(!"".equals(detail)) {
							detail += "\r\n";
						}
						if(ProductType.from(keyValuePair.getKey()) == ProductType.CREATE_TIME) {
							//时间
							detail += keyValuePair.getKeyName()+" : " + DateUtil.convertDate(new Date(Utils.readInt64(keyValuePair.getValue(), 0)));
						} else {
							detail += keyValuePair.getKeyName()+" : " + keyValuePair.getValueToString();
						}
					}
				} else if(tx.getType() == Definition.TYPE_ANTIFAKE_CODE_MAKE) {
					AntifakeCodeMakeTransaction atx = (AntifakeCodeMakeTransaction) tx;
					
					type = "防伪码生产";
					
					TransactionStore ptx = InchainInstance.getInstance().getAccountKit().getTransaction(atx.getProductTx());
					//必要的NPT验证
					if(ptx == null) {
						log.error("防伪验证，商品信息没有找到，将跳过不显示该交易");
						continue;
					}
					Product product = ((ProductTransaction)ptx.getTransaction()).getProduct();
					
					detail += product.getName();
					
					if(atx.getRewardCoin() != null) {
						detail += "\r\n";
						detail += "附带验证奖励 " + atx.getRewardCoin().toText() + " INS";
					}
				} else if(tx.getType() == Definition.TYPE_CREDIT) {
					CreditTransaction ctx = (CreditTransaction) tx;
					
					type = "增加信用";
					
					String reason = "初始化";
					if(ctx.getReasonType() == Definition.CREDIT_TYPE_PAY) {
						reason = String.format("%s小时内第一笔转账", Configure.CERT_CHANGE_PAY_INTERVAL/3600000l);
					}
					detail += "信用 +" + ctx.getCredit() + " 原因：" + reason;
				} else if(tx.getType() == Definition.TYPE_VIOLATION) {
					ViolationTransaction vtx = (ViolationTransaction) tx;
					
					type = "违规处罚";
					
					ViolationEvidence evidence = vtx.getViolationEvidence();
					int violationType = evidence.getViolationType();
					String reason = "";
					long credit = 0;
					if(violationType == ViolationEvidence.VIOLATION_TYPE_NOT_BROADCAST_BLOCK) {
						NotBroadcastBlockViolationEvidence nbve = (NotBroadcastBlockViolationEvidence) evidence;
						reason = String.format("共识过程中，开始时间为%s的轮次超时未出块", DateUtil.convertDate(new Date(nbve.getCurrentPeriodStartTime() * 1000)));
						credit = Configure.CERT_CHANGE_TIME_OUT;
					}
					detail += "信用 " + credit + " 原因：" + reason;
				}
				
				if(tx.isPaymentTransaction() && tx.getOutputs().size() > 0) {
					
					if(isSendout) {
						amount = "-" + Coin.valueOf(tx.getOutput(0).getValue()).toText();
					} else {
						amount = "+" + Coin.valueOf(tx.getOutput(0).getValue()).toText();
						
					}
				}
				time = DateUtil.convertDate(new Date(tx.getTime()), "yyyy-MM-dd HH:mm:ss");
				
				detailValue.setValue(detail);
				list.add(new TransactionEntity(tx.getHash(), bestBlockHeight - txs.getHeight() + 1, type, detailValue, amount, time));
			}
		}
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
}
