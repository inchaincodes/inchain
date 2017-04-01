package org.inchain.core;

import java.util.List;

import org.inchain.store.AccountStore;
import org.inchain.transaction.business.AntifakeCodeMakeTransaction;
import org.inchain.transaction.business.AntifakeTransferTransaction;
import org.inchain.transaction.business.BaseCommonlyTransaction;
import org.inchain.transaction.business.CirculationTransaction;
import org.inchain.transaction.business.ProductTransaction;

/**
 * 防伪码查询结果信息
 * 包含了所有可能的信息
 * @author ln
 */
public class AntifakeInfosResult extends Result {

	//防伪码
	private byte[] antifake;
	//当前验证状态，是否已验证
	private boolean hasVerify;
	//防伪码创建交易
	private AntifakeCodeMakeTransaction makeTx;
	//防伪码验证交易
	private BaseCommonlyTransaction verifyTx;
	//产品
	private ProductTransaction productTx;
	//商家
	private AccountStore business;
	//流转信息
	private List<CirculationTransaction> circulationList;
	//转让信息
	private List<AntifakeTransferTransaction> transactionList;
	//来源
	private List<AntifakeInfosResult> sourceList;
	
	public byte[] getAntifake() {
		return antifake;
	}
	public void setAntifake(byte[] antifake) {
		this.antifake = antifake;
	}
	public ProductTransaction getProductTx() {
		return productTx;
	}
	public void setProductTx(ProductTransaction productTx) {
		this.productTx = productTx;
	}
	public AccountStore getBusiness() {
		return business;
	}
	public void setBusiness(AccountStore business) {
		this.business = business;
	}
	public List<CirculationTransaction> getCirculationList() {
		return circulationList;
	}
	public void setCirculationList(List<CirculationTransaction> circulationList) {
		this.circulationList = circulationList;
	}
	public List<AntifakeTransferTransaction> getTransactionList() {
		return transactionList;
	}
	public void setTransactionList(List<AntifakeTransferTransaction> transactionList) {
		this.transactionList = transactionList;
	}
	public List<AntifakeInfosResult> getSourceList() {
		return sourceList;
	}
	public void setSourceList(List<AntifakeInfosResult> sourceList) {
		this.sourceList = sourceList;
	}
	public AntifakeCodeMakeTransaction getMakeTx() {
		return makeTx;
	}
	public void setMakeTx(AntifakeCodeMakeTransaction makeTx) {
		this.makeTx = makeTx;
	}
	public boolean isHasVerify() {
		return hasVerify;
	}
	public void setHasVerify(boolean hasVerify) {
		this.hasVerify = hasVerify;
	}
	public BaseCommonlyTransaction getVerifyTx() {
		return verifyTx;
	}
	public void setVerifyTx(BaseCommonlyTransaction verifyTx) {
		this.verifyTx = verifyTx;
	}
}
