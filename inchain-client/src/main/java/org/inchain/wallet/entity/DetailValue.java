package org.inchain.wallet.entity;

import java.util.Arrays;

/**
 * 列表详情数据实体
 * @author ln
 *
 */
public class DetailValue {

	private String value;
	private byte[] img;
	
	public DetailValue() {
	}
	public DetailValue(String value, byte[] img) {
		super();
		this.value = value;
		this.img = img;
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public byte[] getImg() {
		return img;
	}
	public void setImg(byte[] img) {
		this.img = img;
	}
	@Override
	public String toString() {
		return "DetailValue [value=" + value + ", img=" + Arrays.toString(img) + "]";
	}
}
