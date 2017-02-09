package org.inchain.account;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.inchain.core.VarInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账户主体
 * @author ln
 *
 */
public class AccountBody {
	
	private static Logger log = LoggerFactory.getLogger(AccountBody.class);
	
	private List<KeyValuePair> contents;
	
	public AccountBody(byte[] content) {
		parse(content);
	}
	
	public AccountBody(List<KeyValuePair> contents) {
		this.contents = contents;
	}
	
	public AccountBody(KeyValuePair[] contents) {
		this.contents = Arrays.asList(contents);
	}
	
	public static AccountBody empty() {
		return new AccountBody(new byte[0]);
	}
	
	//账户信息类型
	public static enum ContentType {
		UNKNOWN("备注"),					//备注
		NAME("名称"),						//名称
		TYPE("类型"),						//类型
		LOGO("图标", "img"),				//图标
		ADDRESS("地址"), 					//地址
		PHONE("固定电话"),					//固定电话
		MOBILE("移动电话"),				//移动电话
		DESCRIPTION("介绍"),				//简介
		CREDIT_CODE("信用代码"),			//社会信用代码
		LEGAL_REPRESENTATIVE("法人"),		//法人代表
		REGISTERED_CAPITAL("注册资本"),	//企业注册资本
		EXECUTIVES("联系人"),				//企业负责人
		EXECUTIVES_PHONE("联系电话"),		//企业负责人联系电话
		WEBSITE("官网"),					//企业官网
		//...更多
		;
		
		public static ContentType from(int type) {
			ContentType[] vs = values();
			for (ContentType ct : vs) {
				if(ct.ordinal() == type) {
					return ct;
				}
			}
			return UNKNOWN;
		}
		
		private String name;
		private String code;

		private ContentType(String name) {
			this.name = name;
		}

		private ContentType(String name, String code) {
			this.name = name;
			this.code = code;
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
	
	public static class KeyValuePair {
		public int key;
		public byte[] value;
		
		public KeyValuePair(ContentType type, byte[] value) {
			this.key = type.ordinal();
			this.value = value;
		}

		public KeyValuePair(ContentType type, String value) {
			this.key = type.ordinal();
			if(value == null) {
				this.value = new byte[0];
			} else {
				this.value = value.getBytes();
			}
		}
		
		public KeyValuePair(int key, byte[] value) {
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
			return ContentType.from(key).getName();
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
		int cursor = 0;
		contents = new ArrayList<KeyValuePair>();
		while(true) {
			VarInt varint = new VarInt(content, cursor);
	        cursor += varint.getOriginalSizeInBytes();
	        
	        KeyValuePair keyValuePair = new KeyValuePair(content[cursor] & 0xff, Arrays.copyOfRange(content, cursor + 1, cursor + (int)varint.value));
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
		return "AccountBody [contents=" + contents + "]";
	}
}
