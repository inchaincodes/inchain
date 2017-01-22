package org.inchain.validator;

/**
 * 交易验证器验证器
 * @author ln
 * @param <T>
 *
 */
public interface Validator<T> {
	
	/**
	 * 交易验证
	 * @param t
	 * @return
	 */
	ValidatorResult<?> valDo(T t);
}
