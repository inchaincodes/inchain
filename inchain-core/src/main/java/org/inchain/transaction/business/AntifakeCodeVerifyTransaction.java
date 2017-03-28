package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.transaction.Output;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Utils;

/**
 * 防伪码验证交易
 * 和普通流程的防伪码认证一样，只接受普通账户的验证，认证商家自己不能参与验证
 * 后期新增认证账户的子账户，对产品进行流转信息的跟踪，比如某个厂家下面有很多经销商，每个经销商作为认证商家的一个字账户存在，产品在到了经销商手里时，经销商可以对商品做记录跟踪	//TODO 子账户功能
 * 和普通防伪码不同的是，这个流程的防伪码，有可能是附带验证奖励的，验证奖励目前是代币，后面新增商家自己发行的资产	//TODO 商家发行资产功能
 * 一个商品的存活周期分为3个阶段，第一阶段生产周期，第二阶段销售周期，第三阶段使用周期
 * 第一阶段在商品生产之前的原材料供应等方面，对多个原材料的组合进行合并，产生一个商品，这属于真正的“溯源”范畴，目前这个流程要落地太难，咱不考虑，直接从厂里出库入库即可
 * 第二阶段销售出库，有可能是先到多个经销商，也有可能是商家通过自己的电商渠道直接出库发往用户手中
 * 第三阶段是用户购买之后，商品的一些售后问题，归属权证明，以及转让等。当然很多产品是快销品，没有第三阶段，但比如手机家电等电子产品，是会有第三阶段的。
 * 
 * 这个地方的流程，是终结第二阶段，也就是用户验证之后，产品就进入第三阶段，第一和第二阶段就不会再接受新的记录
 * 
 * 如果有奖励，那么在广播的时候，需要添加输出，否则奖励就属于打包节点
 * 
 * @author ln
 *
 */
public class AntifakeCodeVerifyTransaction extends BaseCommonlyTransaction {

	/** 防伪码 **/
	protected byte[] antifakeCode;
	/** 经度，用户验证时的地理位置，仅用户商家防窜货分析 **/
	protected double longitude;
	/** 纬度，用户验证时的地理位置，仅用户商家防窜货分析 **/
	protected double latitude;
	
	public AntifakeCodeVerifyTransaction(NetworkParams network, TransactionInput input, byte[] antifakeCode) {
		super(network);
		
		Utils.checkNotNull(input);
		
		inputs.add(input);
		input.setParent(this);
		
		this.antifakeCode = antifakeCode;
		
		type = Definition.TYPE_ANTIFAKE_CODE_VERIFY;
	}
	
	public AntifakeCodeVerifyTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public AntifakeCodeVerifyTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}

	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		
		antifakeCode = readBytes(20);
		longitude = readDouble();
		latitude = readDouble();

		length = cursor - offset;
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		
		stream.write(antifakeCode);
		Utils.doubleToByteStream(longitude, stream);
		Utils.doubleToByteStream(latitude, stream);
	}
	
	@Override
	public void verify() throws VerificationException {
		super.verify();
		
		//验证输入脚本
		if(inputs == null || inputs.isEmpty() || inputs.size() != 1) {
			throw new VerificationException("验证输入错误");
		}
		
		//验证输入脚本是否是防伪码验证
		if(!inputs.get(0).getScriptSig().isAntifakeInputScript()) {
			throw new VerificationException("错误的输入脚本");
		}
		
		//输出脚本必需在2个以内
		if(outputs == null || outputs.size() > 2) {
			throw new VerificationException("输出错误");
		}
		
		//必须是普通账户才有权操作
		if(scriptSig.isCertAccount()) {
			throw new VerificationException("认证账户不能验证");
		}
	}
	
	@Override
	public void verifyScript() {
		super.verifyScript();
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

	/**
	 * 根据输入引用的上次输出脚本，获取防伪码
	 * @return byte[]
	 */
	public byte[] getAntifakeCode() {
		return antifakeCode;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	@Override
	public String toString() {
		return "AntifakeCodeVerifyTransaction [version=" + version + ", type=" + type + ", time=" + time
				+ ", scriptSig=" + scriptSig + ", inputs=" + inputs + ", outputs=" + outputs + ", longitude="
				+ longitude + ", latitude=" + latitude + "]";
	}
	
}
