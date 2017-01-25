package org.inchain.validator;

/**
 * 交易验证器验证器
 * @author ln
 * @param <T>
 *
 */
public interface Validator<T> {
	
	/**
	 * 交易验证，普通的交易验证
	 * 不包含共识时的交易验证，共识时的验证是独立的流程
	 * @param t
	 * @return ValidatorResult<?>
	 */
	ValidatorResult<?> valDo(T t);
	
}
