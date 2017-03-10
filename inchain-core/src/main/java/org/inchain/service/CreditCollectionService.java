package org.inchain.service;

/**
 * 信用服务，存放信用累计凭证，这样能快速的验证
 * @author ln
 *
 */
public interface CreditCollectionService {

	/**
	 * 验证是否可以获得信用
	 * @param type			类型，参考 Definition 里的定义
	 * @param hash160		信用获得人
	 * @param time			以凭证打包进区块的时间为准，也就是以凭证所在的区块时间为准来判断
	 * @return boolean
	 */
	boolean verification(int type, byte[] hash160, long time);
	
	/**
	 * 增加信用
	 * @param type		类型，参考 Definition 里的定义
	 * @param hash160	信用获得人
	 * @param time			凭证所在的区块时间
	 * @return boolean
	 */
	boolean addCredit(int type, byte[] hash160, long time);
	
	/**
	 * 系统启动初始加载
	 * 会阻塞直到加载完成
	 * @return boolean
	 */
	boolean onload();
}
