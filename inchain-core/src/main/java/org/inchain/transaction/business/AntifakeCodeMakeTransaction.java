package org.inchain.transaction.business;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.transaction.Output;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.RandomUtil;
import org.inchain.utils.Utils;

/**
 * 认证商家生成防伪码
 * 分为带代币奖励的防伪码和不带代币奖励的防伪码
 * 两种防伪码除了是否有代币奖励区分外，其他并无区别
 * 与普通防伪流程区别在于，商家的防伪码在生成时就广播到网络，存储于链上
 * 
 * 其中带奖励的防伪码，和普通转账交易大致相同，不同点是这里的防伪码交易，有一笔是输出到脚本，而不是公钥
 * 不带奖励的防伪码，没有代币的输入输出
 * 
 * @author ln
 *
 */
public class AntifakeCodeMakeTransaction extends BaseCommonlyTransaction {
	//是否关联产品 0:关联  1:不关联
	protected int hasProduct;
	/** 关联产品，商家产品信息事先广播到连上时，直接进行关联 **/
	protected Sha256Hash productTx;
	/** 商品编号，类似于商品身份证，用户防止生成重复的防伪码 **/
	protected long nonce;
	
	public AntifakeCodeMakeTransaction(NetworkParams network, Sha256Hash productTx) {
		super(network);
		this.hasProduct = 0;
		this.productTx = productTx;
		
		nonce = RandomUtil.randomLong();
		
		type = Definition.TYPE_ANTIFAKE_CODE_MAKE;
	}
	public AntifakeCodeMakeTransaction(NetworkParams network){
		super(network);
		this.hasProduct = 1;
		this.productTx = null;

		nonce = RandomUtil.randomLong();

		type = Definition.TYPE_ANTIFAKE_CODE_MAKE;
	}

	public AntifakeCodeMakeTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public AntifakeCodeMakeTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		hasProduct = 0xff&(readBytes(1)[0]);
		if(hasProduct==0) {
			productTx = readHash();
		}
		nonce = readInt64();

		length = cursor - offset;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		stream.write(hasProduct);
		if (hasProduct == 0){
			stream.write(productTx.getReversedBytes());
		}
		
		Utils.int64ToByteStreamLE(nonce, stream);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		//必须是认证账户才有权操作
		if(!scriptSig.isCertAccount()) {
			throw new VerificationException("非认证账户");
		}
	}
	
	@Override
	public void verifyScript() {
		super.verifyScript();
	}
	
	/**
	 * 获取防伪码的内容
	 * @return byte[]
	 * @throws IOException
	 */
	public byte[] getAntifakeConent() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			stream.write(type);
			Utils.uint32ToByteStreamLE(version, stream);
			//时间
			Utils.int64ToByteStreamLE(time, stream);
			
			//产品
			if(hasProduct == 0)
				stream.write(productTx.getReversedBytes());

			Utils.int64ToByteStreamLE(nonce, stream);
			
			return stream.toByteArray();
		} finally {
			stream.close();
		}
	}

	/**
	 * 获取防伪码
	 * @return byte[]
	 * @throws IOException 
	 */
	public byte[] getAntifakeCode() throws IOException {
		return Utils.sha256hash160(getAntifakeConent());
	}
	
	/**
	 * 获取该笔防伪码生产交易是否带有代币奖励
	 * 如果没有则返回null，否则返回奖励的金额
	 * @return Coin
	 */
	public Coin getRewardCoin() {
		
		//输入需大于0条
		if(inputs == null || inputs.size() == 0) {
			return null;
		}
		
		//第一笔输出的数量大于0就相当于有奖励了
		Output output = outputs.get(0);
		if(output.getValue() > 0l) {
			return Coin.valueOf(output.getValue());
		}
		return null;
	}
	
	public Sha256Hash getProductTx() {
		return productTx;
	}

	public void setProductTx(Sha256Hash productTx) {
		this.productTx = productTx;
	}

	/**
	 * 获取引用的防伪码列表
	 * @return List<Sha256Hash>
	 */
	public List<Sha256Hash> getSources() {
		List<Sha256Hash> hashs = new ArrayList<Sha256Hash>();
		for (TransactionInput input : inputs) {
			Script script = input.getScriptSig();
			if(input.getFroms().size() == 1 && script.getChunks().size() == 1 && !script.getChunks().get(0).isOpCode() && script.getChunks().get(0).data.length == Sha256Hash.LENGTH) {
				hashs.add(input.getFroms().get(0).getParent().getHash());
			}
		}
		return hashs;
	}
	
	@Override
	public String toString() {
		return "AntifakeCodeMakeTransaction [hash=" + getHash() + ", type=" + type + ", time=" + time + ", outputs="
				+ outputs + ", inputs=" + inputs + ", scriptSig=" + scriptSig + ", productTx=" + productTx + ", nonce="
				+ nonce + "]";
	}

	public int getHasProduct() {
		return hasProduct;
	}

	public void setHasProduct(int hasProduct) {
		this.hasProduct = hasProduct;
	}
}