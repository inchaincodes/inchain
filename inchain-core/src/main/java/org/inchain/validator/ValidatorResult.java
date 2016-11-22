package org.inchain.validator;

/**
 * 验证结果
 * @author ln
 *
 */
public interface ValidatorResult {
	
	/**
	 * 获取验证结果
	 * @return
	 */
	<T> T getResult();
}
