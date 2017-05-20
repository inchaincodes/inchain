package org.inchain.listener;

public interface VersionUpdateListener {
	
	/**
	 * 开始下载
	 */
	void startDownload();
	
	/**
	 * 下载完成率
	 * @param filename
	 * @param rate
	 */
	void downloading(String filename, float rate);
	
	/**
	 * 下载完成
	 */
	void onComplete();
}
