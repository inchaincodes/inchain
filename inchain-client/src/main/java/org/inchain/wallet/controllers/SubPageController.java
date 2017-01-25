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
}
