package org.inchain.core;

import java.io.UnsupportedEncodingException;

import org.inchain.account.AccountBody.ContentType;
import org.inchain.core.Product.ProductType;

/**
 * 通用的键值对
 * @author ln
 *
 */
public class KeyValuePair {
	
	public final static int TYPE_ACCOUNT = 1;
	public final static int TYPE_PRODUCT = 2;

	private int type;	//1账户信息，2产品信息
	private int key;
	private byte[] value;
	
	public KeyValuePair(Enum<?> type, byte[] value) {
		this.key = type.ordinal();
		this.value = value;
		if(type instanceof ContentType) {
			this.type = TYPE_ACCOUNT;
		} else if(type instanceof ProductType) {
			this.type = TYPE_PRODUCT;
		}
	}

	public KeyValuePair(Enum<?> type, String value) {
		this.key = type.ordinal();
		if(value == null) {
			this.value = new byte[0];
		} else {
			this.value = value.getBytes();
		}
		if(type instanceof ContentType) {
			this.type = TYPE_ACCOUNT;
		} else if(type instanceof ProductType) {
			this.type = TYPE_PRODUCT;
		}
	}
	
	public KeyValuePair(int type, int key, byte[] value) {
		this.type = type;
		this.key = key;
		this.value = value;
	}

	public byte[] toByte() {
		if(value == null) {
			return new byte[0];
		}
		byte[] keyValue = new byte[1+value.length];
		keyValue[0] = (byte) key;
		System.arraycopy(value, 0, keyValue, 1, value.length);
		return keyValue;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public byte[] getValue() {
		return value;
	}
	
	public String getValueToString() {
		try {
			return new String(value, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	public void setValue(byte[] value) {
		this.value = value;
	}
	
	public String getKeyName() {
		if(type == TYPE_ACCOUNT) {
			return ContentType.from(key).getName();
		} else if(type == TYPE_PRODUCT) {
			return ProductType.from(key).getName();
		} else {
			return "";
		}
	}

	@Override
	public String toString() {
		try {
			return "KeyValuePair [name=" + getKeyName() + ", value=" + new String(value, "utf-8") + "]";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
}
