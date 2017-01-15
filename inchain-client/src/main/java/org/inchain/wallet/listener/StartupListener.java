package org.inchain.wallet.listener;

/**
 * 程序启动监听
 * @author ln
 *
 */
public interface StartupListener {

	/**
	 * 启动状态变化
	 * @param tip 当前加载的项目
	 */
	void onChange(String tip);
	
	/**
	 * 启动完成
	 */
	void onComplete();
	
	/**
	 * 获取进度
	 * @return int
	 */
	int getCompletionRate();

	/**
	 * 设置进度
	 * @param completionRate	加载完成率
	 */
	void setCompletionRate(int completionRate);
}
