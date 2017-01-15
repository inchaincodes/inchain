package org.inchain.wallet.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Label;

public class AccountInfoController {
	
	private static final Logger log = LoggerFactory.getLogger(AccountInfoController.class);
	
	public Label nowNetTimeId;					//当前网络时间
	public Label localNewestHeightId;			//本地最新高度
	public Label netNewestHeightId;				//网络最新高度
	public Label blockHeightSeparator;			//本地最新高度与网络最新高度分隔符
	public Label networkInfosTipId;				//网络连接节点信息提示
	public Label networkInfosNumId;				//网络连接节点信息
	
	/**
	 *  FXMLLoader 调用的初始化
	 */
    public void initialize() {
    	log.info("==========================");
    }
    
    /**
     * 初始化钱包信息
     */
    public void init() {
    	
    }
}
