package org.inchain.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * 字符串工具类
 * 
 * @author ln
 * 
 */
public final class StringUtil {

	public static final String EMPTY_SRING = "";
	private static final String DEFAULT_SPLIT = "\\s|,|\\|";
	private static final String MOBILE_REG = "^0?(13[0-9]|15[0-9]|18[0-9]|14[157])[0-9]{8}$";

	public static String toString(Object value) {
		return String.valueOf(value);
	}

	/**
	 * 验证字符串是否为空
	 */
	public static boolean isEmpty(Object str) {
		return str == null || EMPTY_SRING.equals(str) || "null".equals(str);
	}

	/**
	 * 验证字符串是否为不为空
	 */
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	public static boolean isNotEmpty(Object str) {
		return !isEmpty(str);
	}

	/**
	 * 给字符串加密，采用md5加密算法
	 * 
	 * @param str
	 * @return String
	 */
	public static String encryption(String str) {
		if (str == null) {
			return null;
		}
		return DigestUtils.md5DigestAsHex(str.getBytes());
	}

	/**
	 * 两个字符串链接后md5加密
	 * 
	 * @param username
	 * @param password
	 * @return String
	 */
	public static String encryption(String username, String password) {
		String result = null;
		if (username != null) {
			result = username;
		}
		if (password != null) {
			result = result == null ? password : result.concat(password);
		}
		if (result == null) {
			return null;
		}
		return DigestUtils.md5DigestAsHex(result.getBytes());
	}
	
	public static void main(String[] args) throws Exception {
		String str = "012";
		System.out.println(encryption(str));
	}
    
	/**   
     * 生成签名数据
     *    
     * @param data 待加密的数据   
     * @param key  加密使用的key   
     */    
    public static byte[] sha1Signature(String data,byte[] key) {  
        try {
        	SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
	        Mac mac = Mac.getInstance("HmacSHA1");
	        mac.init(signingKey);
	        return mac.doFinal(data.getBytes());
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
    /**   
     * 生成签名数据
     *    
     * @param data 待加密的数据  
     * @param key  加密使用的key   
     */    
    public static String sha1Signature(String data,String key) {  
        return byte2hex(sha1Signature(data, key.getBytes()));
    }
    
	/**
	 * sha1加密字符串
	 * 
	 * @param str
	 * @return String
	 */
	public static String sha1(String str) {
		if (str == null) {
			return str;
		} else {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-1");
				digest.update(str.getBytes());
				return byte2hex(digest.digest());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * 将字节数组转换成16进制字符串
	 * 
	 * @param b
	 * @return String
	 */
	public static String byte2hex(byte[] b) {
		StringBuilder sbDes = new StringBuilder();
		String tmp = null;
		for (int i = 0; i < b.length; i++) {
			tmp = (Integer.toHexString(b[i] & 0xFF));
			if (tmp.length() == 1) {
				sbDes.append("0");
			}
			sbDes.append(tmp);
		}
		return sbDes.toString();
	}

	/**
	 * 默认空格，“，”，“|”
	 * 
	 * @param string
	 * @return String[]
	 */
	public static String[] split(String string) {
		if (isEmpty(string)) {
			return null;
		}
		return string.split(DEFAULT_SPLIT);
	}

	public static String[] split(String string, String split) {
		if (isEmpty(string)) {
			return null;
		}
		if (isEmpty(split)) {
			return new String[] { string };
		}
		return string.split(split);
	}

	public static String filter(String str, String filterStr) {
		if (isEmpty(str))
			return EMPTY_SRING;
		if (isEmpty(filterStr)) {
			return str;
		}
		return str.replaceAll(filterStr, EMPTY_SRING);
	}

	/**
	 * 随机生成一个字符串
	 * 
	 * @param letterLength
	 * @param numberLength
	 * @return String
	 */
	public static String randStr(int letterLength, int numberLength) {
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < letterLength; i++) {
			int choice = random.nextInt(2) % 2 == 0 ? 65 : 97; // 取得大写字母还是小写字母
			sb.append((char) (choice + random.nextInt(26)));
		}
		for (int i = 0; i < numberLength; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	public static boolean isQQEmail(String email) {
		if (isEmpty(email)) {
			return false;
		}
		String[] strs = split(email, "@");
		if (strs.length != 2) {
			return false;
		} else {
			String sux = strs[1];
			return "qq.com".equals(sux);
		}
	}

	/**
	 * 判断是否是合法的手机号码
	 * 
	 * @param mobile
	 * @return boolean
	 */
	public static boolean isMobileNumber(String mobile) {
		if (isEmpty(mobile)) {
			return false;
		}
		Pattern pattern = Pattern.compile(MOBILE_REG);
		return pattern.matcher(mobile).find();
	}

	public static boolean isNumber(String content) {
		try {
			Long.valueOf(content);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 校验密码难度
	 * @param password
	 * @return boolean
	 */
	public static boolean validPassword(String password) {
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

	/**
	 * 读取最后一行内容
	 * 
	 * @param file
	 * @param charset
	 * @return String
	 * @throws IOException
	 */
	public static String readLastLine(File file, String charset) {
		if (!file.exists() || file.isDirectory() || !file.canRead()) {
			return null;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			long len = raf.length();
			if (len == 0L) {
				return "";
			} else {
				long pos = len - 1;
				while (pos > 0) {
					pos--;
					raf.seek(pos);
					if (raf.readByte() == '\n') {
						break;
					}
				}
				if (pos == 0) {
					raf.seek(0);
				}
				byte[] bytes = new byte[(int) (len - pos)];
				raf.read(bytes);
				if (charset == null) {
					return new String(bytes);
				} else {
					return new String(bytes, charset);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception e2) {
				}
			}
		}
		return null;
	}
}
