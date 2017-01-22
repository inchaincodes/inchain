package org.inchain.wallet.listener;

import org.inchain.account.Account;

/**
 * 账户信息监听器
 * @author ln
 *
 */
public interface AccountInfoListener {

	/**
	 * 加载完成
	 * @param account 账户
	 */
	void onLoad(Account account);
}
