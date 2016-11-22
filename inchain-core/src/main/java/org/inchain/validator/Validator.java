package org.inchain.validator;

/**
 * 验证器
 * @author ln
 * @param <T>
 *
 */
public interface Validator<T> {
	
	/**
	 * 验证
	 * @param t
	 * @return
	 */
	ValidatorResult valDo(T t);
}
