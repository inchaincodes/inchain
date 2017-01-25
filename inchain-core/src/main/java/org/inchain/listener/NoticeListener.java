package org.inchain.listener;

/**
 * 通知监听器
 * @author ln
 *
 */
public interface NoticeListener {
	
	public final static int NOTICE_TYPE_NONE = 1;
	public final static int NOTICE_TYPE_INFO = 2;
	public final static int NOTICE_TYPE_WARNING = 3;
	public final static int NOTICE_TYPE_ERROR = 4;

	void onNotice(String title, String message);
	
	void onNotice(int type, String title, String message);
}
