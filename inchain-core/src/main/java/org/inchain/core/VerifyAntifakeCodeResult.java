package org.inchain.core;

import org.inchain.account.AccountBody;
import org.inchain.transaction.business.ProductTransaction;

/**
 * 防伪码验证结果
 * @author ln
 *
 */
public class VerifyAntifakeCodeResult extends BroadcastResult {
	
	//验证的商品
	private ProductTransaction productTx;
	//商品所属商家
	private AccountBody businessBody;
	//验证奖励
	private Coin reward;

	public VerifyAntifakeCodeResult() {
	}
	
	public VerifyAntifakeCodeResult(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public void setProductTx(ProductTransaction productTx) {
		this.productTx = productTx;
	}

	public ProductTransaction getProductTx() {
		return productTx;
	}
	
	public AccountBody getBusinessBody() {
		return businessBody;
	}

	public void setBusinessBody(AccountBody businessBody) {
		this.businessBody = businessBody;
	}

	public Coin getReward() {
		return reward;
	}

	public void setReward(Coin reward) {
		this.reward = reward;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VerifyAntifakeCodeResult [success=");
		builder.append(success);
		builder.append(", message=");
		builder.append(message);
		builder.append(", productTx=");
		builder.append(productTx);
		builder.append("]");
		return builder.toString();
	}
	
}
