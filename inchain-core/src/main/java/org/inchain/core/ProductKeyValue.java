package org.inchain.core;

import java.io.UnsupportedEncodingException;

public class ProductKeyValue extends KeyValue {

	public final static ProductKeyValue NAME = new ProductKeyValue("name", "名称");
	public final static ProductKeyValue LOGO = new ProductKeyValue("logo", "图标");
	public final static ProductKeyValue IMG = new ProductKeyValue("img", "图片");
	public final static ProductKeyValue CREATE_TIME = new ProductKeyValue("createTime", "创建时间");
	
//	NAME("名称", "name"),						//名称
//	TYPE("类型", "type"),						//类型
//	LOGO("图标", "img"),						//图标
//	DESCRIPTION("介绍", "description"),		//简介
//	CONTENT("详情", "content"),				//详情
//	PRICE("价格", "price"),					//价格
//	CREATE_TIME("创建时间", "createTime"),				//创建时间
//	PRODUCTION_DATE("生产日期", "productionDate"),		//生产日期
//	PRODUCTION_BATCH("生产批号", "productionBatch"),	//生产批号
//	BARCODE("条形码", "barcode"),						//条形码
//	SALES_AREA("销售地区", "salesArea"),				//销售地区
//
//	REMARK("备注", "remark"),							//备注
//	UNKNOWN("未知", "unknown"),						//未知
	
	
	public ProductKeyValue(String code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public ProductKeyValue(String code, String name, byte[] value) {
		this.code = code;
		this.name = name;
		this.value = value;
	}
	
	public ProductKeyValue(String code, String name, String value) {
		this.code = code;
		this.name = name;
		try {
			this.value = value.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public ProductKeyValue(byte[] content) {
		super(content);
	}
	

}
