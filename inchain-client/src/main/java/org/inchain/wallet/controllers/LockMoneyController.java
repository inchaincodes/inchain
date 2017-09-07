package org.inchain.wallet.controllers;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.Result;
import org.inchain.kit.InchainInstance;
import org.inchain.kits.AccountKit;
import org.inchain.utils.DateUtil;
import org.inchain.wallet.utils.DailogUtil;
import org.springframework.util.StringUtils;

import java.net.URL;

/**
 * 余额锁仓
 * @author ln
 *
 */
public class LockMoneyController extends DailogController {

	public TextField lockAmountId;
	public TextField unlockTimeId;
	
	public Button okId;
	public Button cancelId;
	
	public void initialize() {
		cancelId.setOnAction(e -> resetAndclose());

		okId.setOnAction(e -> lockMoney());
	}
	
	/*
	 * 取消
	 */
	private void resetAndclose() {
		lockAmountId.setText("");
		unlockTimeId.setText("");
		close();
	}

	/*
	 * 锁仓
	 */
	private void lockMoney() {
		
		//校验密码
		String lockAmountStr = lockAmountId.getText();
		String unlockTimeStr = unlockTimeId.getText();
		if(StringUtils.isEmpty(lockAmountStr)) {
			lockAmountId.requestFocus();
			DailogUtil.showTipDailogCenter("锁仓金额不能为空", getThisStage());
			return;
		} else if(StringUtils.isEmpty(unlockTimeStr)) {
			unlockTimeId.requestFocus();
			DailogUtil.showTipDailogCenter("解锁日期不能为空", getThisStage());
			return;
		}

		Coin lockAmount = Coin.ZERO;
		long unlockTime = 0l;
		try {
			lockAmount = Coin.parseCoin(lockAmountStr);
		} catch (Exception e) {
			DailogUtil.showTipDailogCenter("错误的锁仓金额", getThisStage());
			return;
		}
		try {
			unlockTime = DateUtil.convertStringToDate(unlockTimeStr, "yyyy-MM-dd").getTime() / 1000;
		} catch (Exception e) {
			DailogUtil.showTipDailogCenter("错误的日期,格式为yyyy-MM-dd", getThisStage());
			return;
		}

		//判断账户是否加密
		final AccountKit accountKit = InchainInstance.getInstance().getAccountKit();
		if(accountKit.isWalletEncrypted()) {
			//解密账户
			URL location = getClass().getResource("/resources/template/decryptWallet.fxml");
			FXMLLoader loader = new FXMLLoader(location);
			final Coin lockAmountTemp = lockAmount;
			final long unlockTimeTemp = unlockTime;
			DailogUtil.showDailog(loader, "输入钱包密码",460,250, new org.inchain.wallet.utils.Callback() {
				@Override
				public void ok(Object param) {
					if(!accountKit.accountIsEncrypted(Definition.TX_VERIFY_TR)) {
						try {
							doLockMoney(accountKit, lockAmountTemp, unlockTimeTemp);
						} finally {
							accountKit.resetKeys();
						}
					}
				}
			});
		} else {
			doLockMoney(accountKit, lockAmount, unlockTime);
		}
	}

	private void doLockMoney(AccountKit accountKit, Coin lockAmount, long unlockTime) {
		try {
			Result result = accountKit.lockMoney(lockAmount, unlockTime, null, null, "用户锁仓");
			if (result.isSuccess()) {
				DailogUtil.showTipDailogCenter(result.getMessage(), getThisStage());
				resetAndclose();
			} else {
				log.error("锁仓失败,{}", result);
				DailogUtil.showTipDailogCenter("锁仓失败," + result.getMessage(), getThisStage());
			}
		} catch (Exception e) {
			log.error("锁仓出错,{}", e.getMessage());
			DailogUtil.showTipDailogCenter("锁仓失败," + e.getMessage(), getThisStage());
		}
	}
}
