package org.inchain.service;

/**
 * 系统状态服务
 * @author ln
 *
 */
public interface SystemStatusService {

	/**
	 * 初始化中
	 */
	public final static int INITING = 0;
	/**
	 * 数据同步中
	 */
	public final static int DATA_SYNCHRONIZE = 1;
	/**
	 * 数据修复中
	 */
	public final static int DATA_RESET = 2;
	
	/**
	 * 设置系统当前状态
	 * @param status
	 */
	public void setStatus(int status);
	
	/**
	 * 获取当前系统状态
	 * @return int
	 */
	public int getStatus();
	
	/**
	 * 判断当前系统状态是否是同步数据中
	 * @return boolean
	 */
	public boolean isDataSynchronize();
	
	/**
	 * 判断当前是否在数据修复中
	 * @return boolean
	 */
	public boolean isDataReset();
}
