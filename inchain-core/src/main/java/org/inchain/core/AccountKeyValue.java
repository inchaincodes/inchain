package org.inchain.core;

import java.io.UnsupportedEncodingException;

public class AccountKeyValue extends KeyValue {
	
	public final static AccountKeyValue NAME = new AccountKeyValue("name", "名称");
	public final static AccountKeyValue LOGO = new AccountKeyValue("logo", "图标");

	public AccountKeyValue(String code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public AccountKeyValue(String code, String name, byte[] value) {
		this.code = code;
		this.name = name;
		this.value = value;
	}
	
	public AccountKeyValue(String code, String name, String value) {
		this.code = code;
		this.name = name;
		try {
			this.value = value.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public AccountKeyValue(byte[] content) {
		super(content);
	}
	
	
//	//账户信息类型
//			UNKNOWN("备注"),					//备注
//			NAME("名称"),						//名称
//			TYPE("类型"),						//类型
//			LOGO("图标", "img"),				//图标
//			ADDRESS("地址"), 					//地址
//			PHONE("固定电话"),					//固定电话
//			MOBILE("移动电话"),				//移动电话
//			DESCRIPTION("介绍"),				//简介
//			CREDIT_CODE("信用代码"),			//社会信用代码
//			LEGAL_REPRESENTATIVE("法人"),		//法人代表
//			REGISTERED_CAPITAL("注册资本"),	//企业注册资本
//			EXECUTIVES("联系人"),				//企业负责人
//			EXECUTIVES_PHONE("联系电话"),		//企业负责人联系电话
//			WEBSITE("官网"),					//企业官网
//			//...更多
//			;

}
