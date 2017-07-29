package org.inchain.transaction.business;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证商家把防伪码绑定到产品
 * @author ln
 *
 */
public class AntifakeCodeBindTransaction extends CommonlyTransaction {
	/** 关联产品，商家产品信息事先广播到连上时，直接进行关联 **/
	protected Sha256Hash productTx;
	/**产品防伪码**/
	protected byte[] antifakeCode;
	/** 商品编号，类似于商品身份证，用户防止生成重复的防伪码 **/
	protected long nonce;

	public AntifakeCodeBindTransaction(NetworkParams network,Sha256Hash productTx,byte[] antiCode){
		super(network);
		this.productTx = productTx;
		this.antifakeCode = antiCode;
		nonce = RandomUtil.randomLong();
		type = Definition.TYPE_ANTIFAKE_CODE_BIND;
	}

	public AntifakeCodeBindTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }

	public AntifakeCodeBindTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	@Override
	protected void parseBody() throws ProtocolException {
		antifakeCode = readBytes(20);
		productTx = readHash();
		nonce = readInt64();
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		stream.write(antifakeCode);
		stream.write(productTx.getReversedBytes());
		Utils.int64ToByteStreamLE(nonce, stream);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		if( productTx == null) {
			throw new VerificationException("商品信息不存在");
		}
		
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
//	public byte[] getAntifakeCode() throws IOException {
//		return Utils.sha256hash160(getAntifakeConent());
//	}
	
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
	public byte[] getAntifakeCode() {
		return antifakeCode;
	}

	public void setAntifakeCode(byte[] getAntifakeCode) {
		this.antifakeCode = getAntifakeCode;
	}
	@Override
	public String toString() {
		return "AntifakeCodeMakeTransaction [hash=" + getHash() + ", type=" + type + ", time=" + time + ", outputs="
				+ outputs + ", inputs=" + inputs + ", scriptSig=" + scriptSig + ", productTx=" + productTx + ", nonce="
				+ nonce + "]";
	}
}