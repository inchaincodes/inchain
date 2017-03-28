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
	
	public Product(byte[] contents) {
		parse(contents);
	}
	
	public Product(List<ProductKeyValue> contents) {
		this.contents = contents;
	}
	
	public Product(ProductKeyValue[] contents) {
		this.contents = Arrays.asList(contents);
	}
	
	//产品信息
	private List<ProductKeyValue> contents;
	
	public final byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			if(contents != null) {
				for (ProductKeyValue keyValuePair : contents) {
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
		contents = new ArrayList<ProductKeyValue>();
		while(true) {
			VarInt varint = new VarInt(content, cursor);
	        cursor += varint.getOriginalSizeInBytes();
	        
	        ProductKeyValue keyValuePair = new ProductKeyValue(Arrays.copyOfRange(content, cursor, cursor + (int)varint.value));
	        contents.add(keyValuePair);
	        
	        cursor += varint.value;
	        if(cursor >= content.length) {
	        	break;
	        }
		}
	}

	public List<ProductKeyValue> getContents() {
		return contents;
	}

	public void setContents(List<ProductKeyValue> contents) {
		this.contents = contents;
	}
	
	public String getName() {
		for (ProductKeyValue keyValuePair : contents) {
			if(keyValuePair.getCode().equals(ProductKeyValue.NAME.code)) {
				return keyValuePair.getValueToString();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "Product [contents=" + contents + "]";
	}
}
