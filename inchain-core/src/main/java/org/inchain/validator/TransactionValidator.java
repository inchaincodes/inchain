package org.inchain.validator;

import org.inchain.transaction.Transaction;

/**
 * 交易验证器
 * @author ln
 * @param <T>
 *
 */
public class TransactionValidator implements Validator<Transaction> {

	@Override
	public ValidatorResult valDo(Transaction t) {
		//TODO
		return null;
	}
}
