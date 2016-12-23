package org.inchain.utils;

/**
 * 随机数工具
 * @author ln
 *
 */
public final class RandomUtil {

	/**
	 * 生成一个长整形随机数
	 * @return long
	 */
	public static long randomLong() {
		return (long)(Math.random()*Long.MAX_VALUE);
	}
}
