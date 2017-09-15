package org.inchain.wallet.controllers;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import org.inchain.Configure;
import org.inchain.SpringContextUtils;
import org.inchain.account.Account;
import org.inchain.account.Address;
import org.inchain.core.Definition;
import org.inchain.core.Result;
import org.inchain.core.TimeService;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.listener.Listener;
import org.inchain.store.AccountStore;
import org.inchain.store.TransactionStoreProvider;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.Context;
import org.inchain.wallet.listener.AccountInfoListener;
import org.inchain.wallet.utils.Callback;
import org.inchain.wallet.utils.ConfirmDailog;
import org.inchain.wallet.utils.DailogUtil;
import org.inchain.wallet.utils.TipsWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

/**
 * 账户信息控制器
 * 
 * @author ln
 *
 */
public class AccountInfoController implements SubPageController {

	private static final Logger log = LoggerFactory.getLogger(AccountInfoController.class);

	// 文件选择器
	private FileChooser fileChooser = new FileChooser();
	// 账户信息监听器，加载完账户之后执行
	private AccountInfoListener accountInfoListener;

	public Label totalBalanceId; // 总余额
	public Label canUseBalanceId; // 可用余额
	public Label canNotUseBalanceId; // 不可用余额
	public Label addressId; // 账户地址
	public TextField aliasId; // 账户别名
	public Label certId; // 信用
	public Label transactionNumberId; // 交易数量
	public Label encryptionStatusId; // 加密状态
	public Label consensusStatusId; // 当前共识状态

	public Button weChatId;        //微信同步
	public Button backupWalletId; // 备份钱包
	public Button importWalletId; // 导入钱包
	public Button encryptWalletId; // 加密钱包
	public Button cancelButtonId; // 取消别名修改
	public Button aliasButtonId; // 设置别名
	public Button lockMoneyId; 	// 锁仓

	private Image imageDecline;
	// 别名状态，1可操作，2等待网络确认
	private int aliasStatus;
	private byte[] alias;
	// 编辑按钮状态 flase编辑 true确认
	private boolean aliasButtonStatus = true;
	private boolean importing;

	/**
	 * FXMLLoader 调用的初始化
	 */
	public void initialize() {
		// 初始化按钮
		addImageToButton(backupWalletId, "backupWallet");
		addImageToButton(importWalletId, "importWallet");
		addImageToButton(weChatId, "wechat");
		// 点击备份钱包事件
		backupWalletId.setOnAction(e -> backupWallet());
		// 点击导入钱包事件
		importWalletId.setOnAction(e -> importWallet());
		// 点击加密钱包事件
		encryptWalletId.setOnAction(e -> encryptWallet());

		weChatId.setOnAction(e -> weChatSync());
		// 设置文件格式
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("DAT files (*.dat)", "*.dat");
		fileChooser.getExtensionFilters().add(extFilter);
		// 默认选择程序运行目录

		fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		cancelButtonId.setOnAction(e -> initialState());
		aliasButtonId.setOnAction(e -> setOrUpdateAlias());
		lockMoneyId.setOnAction(e -> lockMoney());
		lockMoneyId.setTooltip(new Tooltip("锁仓"));
	}

	private void addImageToButton(Button button, String name) {
		imageDecline = new Image(getClass().getResourceAsStream("/images/" + name + "_icon.png"));
		button.setGraphic(new ImageView(imageDecline));
		button.setGraphicTextGap(10);
	}

	/**
	 * 初始化钱包信息
	 */
	public void initDatas() {
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		Account account = accountKit.getDefaultAccount();
		if(account == null || importing) {
			return;
		}
		if (accountInfoListener != null) {
			accountInfoListener.onLoad(account);
		}
		// 设置内页的余额
		final Address address = account.getAddress();
		// 自己的账户信息
		AccountStore accountStore = accountKit.getAccountInfo();
		// 自己的交易信息
		TransactionStoreProvider transactionStoreProvider = SpringContextUtils
				.getBean(TransactionStoreProvider.class);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				totalBalanceId.setText(address.getBalance().add(address.getUnconfirmedBalance()).toText());
				canUseBalanceId.setText(address.getBalance().toText());
				canNotUseBalanceId.setText(address.getUnconfirmedBalance().toText());
				addressId.setText(address.getBase58());
				certId.setText(String.valueOf(accountStore.getCert()));
				transactionNumberId.setText(String.valueOf(transactionStoreProvider.getTransactions().size()));
				if (accountKit.isWalletEncrypted()) {
					encryptionStatusId.setText("已加密");
				} else {
					encryptionStatusId.setText("未加密,为了资金安全,请加密钱包");
				}
				if (accountKit.checkConsensusing(null)) {
					consensusStatusId.setText("正在共识中");
				} else {
					consensusStatusId.setText("未参与共识");
				}
				if (!Arrays.equals(alias, accountStore.getAlias())) {
					aliasStatus = 1;
					alias = accountStore.getAlias();
				}
				// 别名是否已设置
				if (accountStore.getAlias() == null || accountStore.getAlias().length == 0) {
					// 未设置
					// 是否达到条件
					if (accountStore.getCert() >= Configure.REG_ALIAS_CREDIT) {
						aliasId.setText("别名未设置");
						aliasButtonId.setDisable(aliasStatus == 2 ? true : false);
						aliasButtonId.setTooltip(new Tooltip("设置别名"));
					} else {
						aliasId.setText("信用值达到" + Configure.REG_ALIAS_CREDIT + "后可免费设置别名");
						aliasButtonId.setDisable(true);
					}
				} else {
					try {
						aliasId.setText(new String(accountStore.getAlias(), "utf-8"));
						aliasButtonId.setDisable(aliasStatus == 2 ? true : false);
						aliasButtonId.setTooltip(new Tooltip("修改别名"));
					} catch (UnsupportedEncodingException e) {
					}
				}
			}
		});
	}

	public void setAccountInfoListener(AccountInfoListener accountInfoListener) {
		this.accountInfoListener = accountInfoListener;
	}

	@Override
	public void onShow() {
		// noting to do
	}

	@Override
	public void onHide() {
		// noting to do
	}

	/*
	 *初始状态 
	 * */
	private void initialState() {
		aliasButtonStatus = !aliasButtonStatus;
		aliasId.setStyle("-fx-border-color: transparent;-fx-background-color: null;-fx-text-fill: #fff;");
		aliasId.setEditable(false);
		cancelButtonId.setVisible(false);
		aliasId.setOnKeyPressed(e->{});
		aliasButtonId.setStyle("-fx-background-image:url(\"/images/changepassword_icon.png\");");
		aliasButtonId.setOnMouseMoved(new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				aliasButtonId.setStyle("-fx-background-image:url(\"/images/changepasswordH_icon.png\");");
			}
		});
		aliasButtonId.setOnMouseExited(new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				aliasButtonId.setStyle("-fx-background-image:url(\"/images/changepassword_icon.png\");");
			}
		});
	}
	/*
	 * 编辑状态
	 * */
	private void braveryState() {
		aliasId.setStyle("-fx-border-color: #4474af;");
		aliasId.setEditable(true);
		aliasId.requestFocus();
		cancelButtonId.setVisible(true);
		aliasButtonId.setStyle("-fx-background-image:url(\"/images/save_icon.png\");");
		aliasId.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(event.getCode() == KeyCode.ENTER) {
					setOrUpdateAlias();
				}
			}
		});
		aliasButtonId.setOnMouseMoved(new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				aliasButtonId.setStyle("-fx-background-image:url(\"/images/saveH_icon.png\");");
			}
		});
		aliasButtonId.setOnMouseExited(new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				aliasButtonId.setStyle("-fx-background-image:url(\"/images/save_icon.png\");");
			}
		});
	}
	/*
	 * 设置或者修改别名
	 */
	private void setOrUpdateAlias() {

		aliasButtonStatus = !aliasButtonStatus;
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		// 自己的账户信息
		AccountStore accountStore = accountKit.getAccountInfo();
		if (aliasButtonStatus == false) { // 修改
			if (accountStore.getAlias() == null || accountStore.getAlias().length == 0) {
				aliasId.setText("");
				braveryState();
			} else {
				ConfirmDailog confirmDailog = new ConfirmDailog(Context.getMainStage(),
						"注意：修改账户别名会消耗 " + Math.abs(Configure.UPDATE_ALIAS_SUB_CREDIT) + " 点信用", 1);
				confirmDailog.setListener(new Listener() {
					public void onComplete() {
						braveryState();
					}
				}, new Listener() {
					public void onComplete() {
						aliasButtonStatus = !aliasButtonStatus;
					}
				});
				confirmDailog.show();
			}

		} else { // 确认修改或者设置
			aliasButtonStatus = !aliasButtonStatus;
			try {

				// 判断账户是否已设置别名
				if (accountStore.getAlias() == null || accountStore.getAlias().length == 0) {

					if (accountStore.getCert() >= Configure.REG_ALIAS_CREDIT) {
						// 如果账户已加密，则需要先解密
						if (accountKit.isWalletEncrypted()) {
							// 解密账户
							URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
							FXMLLoader loader = new FXMLLoader(location);
							final AccountKit accountKitTemp = accountKit;
							DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
								@Override
								public void ok(Object param) {
									if (!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
										try {
											setAlias();
										} catch (UnsupportedEncodingException e) {
											e.printStackTrace();
										} finally {
											accountKitTemp.resetKeys();
										}
									}
								}
							});
						} else {
							setAlias();
						}

					} else {
						DailogUtil.showTip("信用值达到" + Configure.REG_ALIAS_CREDIT + "后才能设置别名");
					}
				} else {
					// 修改账户别名
					if (accountStore.getCert() >= Configure.UPDATE_ALIAS_CREDIT) {

						// 如果账户已加密，则需要先解密
						if (accountKit.isWalletEncrypted()) {
							// 解密账户
							URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
							FXMLLoader loader = new FXMLLoader(location);
							final AccountKit accountKitTemp = accountKit;
							DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
								@Override
								public void ok(Object param) {
									if (!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
										try {
											updateAlias();
										} catch (UnsupportedEncodingException e) {
											e.printStackTrace();
										} finally {
											accountKitTemp.resetKeys();
										}
									}
								}
							});
						} else {
							updateAlias();
						}
					} else {
						DailogUtil.showTip("信用值达到" + Configure.UPDATE_ALIAS_CREDIT + "后才能修改别名");
					}
				}
			} catch (Exception e) {
				initialState();
				log.error("修改账户别名出错", e);
			}
		}
	}


	private void updateAlias() throws UnsupportedEncodingException {
		// 校验
		String alias = aliasId.getText().trim();
		if (StringUtils.isEmpty(alias)) {
			aliasId.requestFocus();
			DailogUtil.showTip("别名不能为空");
			return;
		}

		// 修改密码并判断结果
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();

		Result result = accountKit.updateAlias(alias);
		if (result.isSuccess()) {
			aliasStatus = 2;
			aliasButtonId.setDisable(true);
			DailogUtil.showTip("请求已提交，等待网络确认");
		} else {
			log.error("修改别名失败,{}", result);
			DailogUtil.showTip("修改别名失败," + result.getMessage());
		}
		initialState();
	}

	private void setAlias() throws UnsupportedEncodingException {
		// 校验
		String alias = aliasId.getText().trim();
		if (StringUtils.isEmpty(alias)) {
			aliasId.requestFocus();
			DailogUtil.showTip("别名不能为空");
			return;
		}

		// 修改密码并判断结果
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();

		Result result = accountKit.setAlias(alias);
		if (result.isSuccess()) {
			aliasStatus = 2;
			aliasButtonId.setDisable(true);
			// DailogUtil.showTip(result.getMessage());
			DailogUtil.showTip("请求已提交，等待网络确认");
		} else {

			log.error("别名设置失败,{}", result);
			DailogUtil.showTipDailogCenter("别名设置失败," + result.getMessage(), Context.getMainStage());
		}
		initialState();
	}

	/*
	 * 备份钱包
	 */
	private void backupWallet() {
		// 创建一个文件选择器
		fileChooser.setTitle("设置钱包备份路径");
		// 默认备份文件名
		fileChooser.setInitialFileName("wallet_backup_"
				.concat(DateUtil.convertDate(new Date(TimeService.currentTimeMillis()), "yyyyMMddHHmm")));
		File file = fileChooser.showSaveDialog(Context.getMainStage());
		if (file == null) {
			return;
		}
		// 用户选择的完整路径
		String backupFilePath = file.getAbsolutePath();
		// 去掉用户自己设置的后缀.dat
		if (backupFilePath.endsWith(".dat") || backupFilePath.endsWith(".DAT")) {
			backupFilePath = backupFilePath.substring(0, backupFilePath.length() - 4).concat(".dat");
		}
		// 备份
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		try {
			Result result = accountKit.backupWallet(backupFilePath);
			if (result.isSuccess()) {
				DailogUtil.showTip("备份成功");
			} else {
				log.error("备份钱包失败,{}", result);
				DailogUtil.showTip("备份钱包失败," + result.getMessage());
			}
		} catch (Exception e) {
			log.error("备份钱包时出错 {} ", e.getMessage(), e);
			DailogUtil.showTip("备份钱包时出错" + e.getMessage());
		}
	}

	/*
	 * 导入钱包
	 */
	private void importWallet() {
		// 创建一个文件选择器
		fileChooser.setTitle("选择需要导入的钱包文件");
		// 默认备份文件名
		fileChooser.setInitialFileName("wallet_backup_"
				.concat(DateUtil.convertDate(new Date(TimeService.currentTimeMillis()), "yyyyMMddHHmm")));
		final File file = fileChooser.showOpenDialog(Context.getMainStage());
		if (file == null) {
			return;
		}

		importing = true;
		TipsWindows tips = new TipsWindows(null, "钱包导入中，请稍候···");
		tips.show();
		new Thread() {
			public void run() {
				doImport(file);
				tips.close();
				importing = false;
			}
		}.start();
	}

	private void doImport(File file) {
		// 用户选择的完整路径
		String walletFilePath = file.getAbsolutePath();
		// 去掉用户自己设置的后缀.dat
		if (walletFilePath.endsWith(".dat") || walletFilePath.endsWith(".DAT")) {
			walletFilePath = walletFilePath.substring(0, walletFilePath.length() - 4).concat(".dat");
		}
		// 备份
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		
		try {
			Result result = accountKit.importWallet(walletFilePath);
			if (result.isSuccess()) {
				// 更新余额及交易记录
				accountKit.getTransactionListener().newTransaction(null);
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
				    	DailogUtil.showTip(result.getMessage());
					}
				});
			} else {
				log.error("导入钱包失败,{}", result);
				Platform.runLater(new Runnable() {
				    @Override
				    public void run() {
				    	DailogUtil.showTip("导入钱包失败," + result.getMessage());
					}
				});
			}
		} catch (Exception e) {
			log.error("导入钱包时出错 {} ", e.getMessage(), e);
			Platform.runLater(new Runnable() {
			    @Override
			    public void run() {
					DailogUtil.showTip("导入钱包时出错" + e.getMessage());
				}
			});
		}
	}

	/*
	 * 加密钱包
	 */
	private void encryptWallet() {

		try {
			AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
			// 判断账户是否加密
			if (accountKit.isWalletEncrypted()) {
				// 修改密码
				URL location = null;
				if (accountKit.isCertAccount()) {
					location = getClass().getResource("/resources/template/changeCertAccountPassword.fxml");
				} else {
					location = getClass().getResource("/resources/template/changeWalletPassword.fxml");
				}
				FXMLLoader loader = new FXMLLoader(location);
				DailogUtil.showDailog(loader, "修改密码");
			} else {
				// 加密
				URL location = getClass().getResource("/resources/template/encryptWallet.fxml");
				FXMLLoader loader = new FXMLLoader(location);

				DailogUtil.showDailog(loader, "加密钱包", new Callback() {
					@Override
					public void ok(Object param) {
						AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
						// 判断账户是否加密
						if (accountKit.isWalletEncrypted()) {
							encryptionStatusId.setText("已加密");
						}
					}
				});
			}
		} catch (Exception e) {
			log.error("加密钱包出错", e);
		}
	}

	/**
	 * 微信同步
	 */
	private void weChatSync() {
		AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		Account account = accountKit.getDefaultAccount();
		//判断是否已加密
		if(!accountKit.isWalletEncrypted()) {
			showPriKeyQRCode();
		}else {
			URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
			FXMLLoader loader = new FXMLLoader(location);
			DailogUtil.showDailog(loader, "输入钱包密码", new Callback() {
				@Override
				public void ok(Object param) {
					if(!((!account.isCertAccount() && account.isEncrypted()) ||
						  (account.isCertAccount() && account.isEncryptedOfTr()))) {
						new Thread() {
							@Override
							public void run() {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										showPriKeyQRCode();
									}
								});
							}
						}.start();
					}
				}
			});
		}
	}

	private void showPriKeyQRCode() {
		URL location = getClass().getResource("/resources/template/weChatCode.fxml");
		FXMLLoader loader = new FXMLLoader(location);
		DailogUtil.showDailog(loader, "扫描二维码",300,350);
	}

	/*
	 * 锁仓
	 */
	private void lockMoney() {
		try {
			AccountKit accountKit = InchainInstance.getInstance().getAccountKit();

			// 锁仓
			URL location = getClass().getResource("/resources/template/lockMoney.fxml");
			FXMLLoader loader = new FXMLLoader(location);

			DailogUtil.showDailog(loader, "余额锁仓", new Callback() {
				@Override
				public void ok(Object param) {
				}
			});
		} catch (Exception e) {
			log.error("锁仓出错", e);
		}
	}

	@Override
	public boolean refreshData() {
		return aliasButtonStatus;
	}

	@Override
	public boolean startupInit() {
		return true;
	}
}
