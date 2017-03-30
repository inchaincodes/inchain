package org.inchain.wallet.controllers;

import org.inchain.wallet.Context;
import org.inchain.wallet.utils.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javafx.stage.Stage;

/**
 * 弹出页面控制器
 * @author ln
 *
 */
public abstract class DailogController {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected Callback callback;
	protected String pageId;
	
	/**
	 * 校验密码难度
	 * @param password
	 * @return boolean
	 */
	protected static boolean validPassword(String password) {
		if(StringUtils.isEmpty(password)){  
            return false;  
        } 
		if(password.length() < 6){  
            return false;  
        }  
        if(password.matches("(.*)[a-zA-z](.*)") && password.matches("(.*)\\d+(.*)")){  
            return true;  
        } else {
        	return false;
        }
	}
	
	protected void close() {
		Stage window = getThisStage();
		if(window != null) {
			window.close();
		}
	}
	
	protected Stage getThisStage() {
		return Context.getStage(pageId);
	}

	public Callback getCallback() {
		return callback;
	}

	public void setCallback(Callback callback) {
		this.callback = callback;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}
}
