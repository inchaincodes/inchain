package org.inchain.account;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.inchain.core.AccountKeyValue;
import org.inchain.core.VarInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.inchain.core.exception.ContentErrorExcetption;

/**
 * 账户主体
 * @author ln
 *
 */
public class AccountBody {
	
	private static Logger log = LoggerFactory.getLogger(AccountBody.class);
	
	private List<AccountKeyValue> contents;
	
	public AccountBody(byte[] content) throws ContentErrorExcetption{
			parse(content);
	}
	
	public AccountBody(List<AccountKeyValue> contents) {
		this.contents = contents;
	}
	
	public AccountBody(AccountKeyValue[] contents) {
		this.contents = Arrays.asList(contents);
	}
	
	public static AccountBody empty() {
		AccountBody ab = null;
		try {
			ab = new AccountBody(new byte[0]);
		}catch (Exception e){
			System.out.print("never happen");
		}
		return ab;
	}
	
	public final byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			if(contents != null) {
				for (AccountKeyValue keyValuePair : contents) {
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
	

	public void parse(byte[] content) throws ContentErrorExcetption{
		if(content == null || content.length == 0) {
			return;
		}
		int cursor = 0;
		contents = new ArrayList<AccountKeyValue>();
		while(true) {
			VarInt varint = new VarInt(content, cursor);
	        cursor += varint.getOriginalSizeInBytes();
	        
	        AccountKeyValue keyValuePair = new AccountKeyValue(Arrays.copyOfRange(content, cursor, cursor + (int)varint.value));
	        contents.add(keyValuePair);
	        
	        cursor += varint.value;
	        if(cursor >= content.length) {
	        	break;
	        }
		}
	}

	public List<AccountKeyValue> getContents() {
		return contents;
	}

	public void setContents(List<AccountKeyValue> contents) {
		this.contents = contents;
	}
	
	public String getName() {
		for (AccountKeyValue keyValuePair : contents) {
			if(keyValuePair.getCode().equals(AccountKeyValue.NAME.getCode())) {
				return keyValuePair.getValueToString();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "AccountBody [contents=" + contents + "]";
	}
}
