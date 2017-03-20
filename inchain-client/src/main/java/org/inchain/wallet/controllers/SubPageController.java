package org.inchain.wallet.controllers;

/**
 * 子页面接口
 * @author ln
 *
 */
public interface SubPageController {

	/**
	 * 初始化页面所需数据
	 */
	void initDatas();
	
	/**
	 * 页面显示事件
	 */
	void onShow();
	
	/**
	 * 页面隐藏/关闭事件
	 */
	void onHide();
	
	/**
	 * 当处于该页面时，是否动态刷新该页面的数据
	 * @return boolean
	 */
	boolean refreshData();
	
	/**
	 * 启动的时候是否初始化数据
	 * @return boolean
	 */
	boolean startupInit();
}
