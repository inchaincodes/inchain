package org.inchain.core;

import org.springframework.stereotype.Service;

/**
 * 时间工具，存储网络时间与本地时间，所有与网络交互的时间，经过换算，保证网络时间同步
 * @author ln
 *
 */
@Service
public final class TimeHelper {

	/**
	 * 当前毫秒时间
	 * @return long
	 */
	public static long currentTimeMillis() {
		return System.currentTimeMillis();
	}
}
