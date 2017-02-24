package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Product;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 普通防伪交易流程，商家生产的防伪码，验证之前不会广播到链上，在用户验证时才会收录进区块链
 * @author ln
 *
 */
public class GeneralAntifakeTransaction extends CommonlyTransaction {
	
	//关联产品
	protected Sha256Hash productTx;
	//产品信息，和关联产品不能同时存在
	protected Product product;
	//商家签名信息，验证该产品确实是对应商家生产的，该签名是对product进行的签名
	protected byte[] signVerification;
	protected Script signVerificationScript;
	//经度
	protected double longitude;
	//纬度
	protected double latitude;
 
	public GeneralAntifakeTransaction(NetworkParams network) throws ProtocolException {
		super(network);
		type = Definition.TYPE_GENERAL_ANTIFAKE;
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, byte[] payloadBytes) throws ProtocolException {
		this(network, payloadBytes, 0);
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, byte[] payloadBytes, int offset) throws ProtocolException {
		super(network, payloadBytes, offset);
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, Product product, double longitude, double latitude, Script signScript) throws ProtocolException {
		super(network);
		
		Utils.checkNotNull(signScript);
		
		this.product = product;
		this.longitude = longitude;
		this.latitude = latitude;
		
		this.signVerificationScript = signScript;
		signVerification = signVerificationScript.getProgram();
		type = Definition.TYPE_GENERAL_ANTIFAKE;
	}
	
	@Override
	public void verfify() throws VerificationException {
		super.verfify();
		
		if(product == null || productTx == null) {
			new VerificationException("商品信息不存在");
		} else if(product != null && productTx != null) {
			new VerificationException("商品信息重复");
		}
		
		if(signVerification == null && signVerificationScript == null) {
			throw new VerificationException("验证信息不存在");
		}
		if(signVerificationScript == null) {
			signVerificationScript = new Script(signVerification);
		}
		
		if(!signVerificationScript.isCertAccount()) {
			throw new VerificationException("不合法的商家签名");
		}
	}
	
	@Override
	public void verfifyScript() {
		super.verfifyScript();
		//验证商家签名
		signVerificationScript.runVerify(Sha256Hash.twiceOf(product.serialize()));
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);

		if(signVerification == null) {
			throw new VerificationException("没有签名");
		}
		
		if(productTx != null) {
			stream.write(1);
			stream.write(productTx.getReversedBytes());
		}
		
		if(product != null) {
			stream.write(2);
			byte[] productBytes = product.serialize();
			stream.write(new VarInt(productBytes.length).encode());
			stream.write(productBytes);
		}
		
		stream.write(new VarInt(signVerification.length).encode());
		stream.write(signVerification);
		
		Utils.doubleToByteStream(longitude, stream);
		Utils.doubleToByteStream(latitude, stream);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		
		int productType = readBytes(1)[0];
		if(productType == 1) {
			productTx = readHash();
		} else {
			product = new Product(readBytes((int)readVarInt()));
			signVerification = readBytes((int)readVarInt());
			signVerificationScript = new Script(signVerification);
		}
		
		longitude = readDouble();
		latitude = readDouble();
		
		length = cursor - offset;
	}

	@Override
	public String toString() {
		return "GeneralAntifakeTransaction [version=" + version + ", time=" + time + ", scriptSig=" + scriptSig + ", product=" + product + ", signVerificationScript=" + signVerificationScript
				+ ", longitude=" + longitude + ", latitude=" + latitude + "]";
	}
}
