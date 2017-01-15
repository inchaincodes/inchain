package org.inchain.wallet.listener;

/**
 * 程序启动监听
 * @author ln
 *
 */
public interface StartupListener {

	/**
	 * 启动状态变化
	 * @param tip				当前加载的项目
	 * @param completionRate	加载完成率
	 */
	void onChange(String tip, float completionRate);
	
	/**
	 * 启动完成
	 */
	void onComplete();
}
