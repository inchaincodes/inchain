package org.inchain.core;

import org.inchain.message.Message;

/**
 * 下载数据结果
 * @author ln
 *
 */
public class GetDataResult {
	//结果
	private boolean success;
	private Message data;

	public GetDataResult(boolean success) {
		this.success = success;
	}
	
	public GetDataResult(Message data, boolean success) {
		this.data = data;
		this.success = success;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public Message getData() {
		return data;
	}
}
