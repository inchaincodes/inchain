package org.inchain.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 产品信息
 * @author ln
 *
 */
public class Product {

	private static Logger log = LoggerFactory.getLogger(Product.class);
	
	//产品信息类型
	public static enum ProductType {
		NAME("名称", "name"),						//名称
		TYPE("类型", "type"),						//类型
		LOGO("图标", "img"),						//图标
		DESCRIPTION("介绍", "description"),		//简介
		CONTENT("详情", "content"),				//详情
		PRICE("价格", "price"),					//价格
		CREATE_TIME("创建时间", "createTime"),				//创建时间
		PRODUCTION_DATE("生产日期", "productionDate"),		//生产日期
		PRODUCTION_BATCH("生产批号", "productionBatch"),	//生产批号
		BARCODE("条形码", "barcode"),						//条形码
		SALES_AREA("销售地区", "salesArea"),				//销售地区

		REMARK("备注", "remark"),							//备注
		UNKNOWN("未知", "unknown"),						//未知
		
		//...更多
		;
		
		public static ProductType from(int type) {
			ProductType[] vs = values();
			for (ProductType ct : vs) {
				if(ct.type == type) {
					return ct;
				}
			}
			return UNKNOWN;
		}
		
		public static ProductType fromCode(String code) {
			ProductType[] vs = values();
			for (ProductType ct : vs) {
				if(ct.code.equals(code)) {
					return ct;
				}
			}
			return UNKNOWN;
		}
		
		private String name;
		private String code;
		private int type;

		ProductType(String name) {
			this.name = name;
			this.type = ordinal();
		}

		ProductType(String name, String code) {
			this.name = name;
			this.code = code;
			this.type = ordinal();
		}
		
		ProductType(String name, String code, int type) {
			this.name = name;
			this.code = code;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
	
	public Product(byte[] contents) {
		parse(contents);
	}
	
	public Product(List<KeyValuePair> contents) {
		this.contents = contents;
	}
	
	public Product(KeyValuePair[] contents) {
		this.contents = Arrays.asList(contents);
	}
	
	//产品信息
	private List<KeyValuePair> contents;
	
	public final byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			if(contents != null) {
				for (KeyValuePair keyValuePair : contents) {
					byte[] keyValue = keyValuePair.toByte();
					bos.write(new VarInt(keyValue.length).encode());
					bos.write(keyValue);
				}
			}
			return bos.toByteArray();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
			}
		}
		return new byte[0];
	}
	

	public void parse(byte[] content) {
		if(content == null || content.length == 0) {
			return;
		}
		int cursor = 0;
		contents = new ArrayList<KeyValuePair>();
		while(true) {
			VarInt varint = new VarInt(content, cursor);
	        cursor += varint.getOriginalSizeInBytes();
	        
	        KeyValuePair keyValuePair = new KeyValuePair(KeyValuePair.TYPE_PRODUCT, content[cursor] & 0xff, Arrays.copyOfRange(content, cursor + 1, cursor + (int)varint.value));
	        contents.add(keyValuePair);
	        
	        cursor += varint.value;
	        if(cursor >= content.length) {
	        	break;
	        }
		}
	}

	public List<KeyValuePair> getContents() {
		return contents;
	}

	public void setContents(List<KeyValuePair> contents) {
		this.contents = contents;
	}

	@Override
	public String toString() {
		return "Product [contents=" + contents + "]";
	}
}
