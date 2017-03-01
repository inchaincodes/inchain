package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.core.Definition;
import org.inchain.core.Product;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;

/**
 * 认证账户产品管理
 * @author ln
 *
 */
public class ProductTransaction extends CommonlyTransaction {
	
	/** 产品信息 **/
	protected Product product;

	public ProductTransaction(NetworkParams network) throws ProtocolException {
		super(network);
		this.setType(Definition.TYPE_CREATE_PRODUCT);
	}
	
	public ProductTransaction(NetworkParams network, Product product) throws ProtocolException {
		super(network);
		this.product = product;
		this.setType(Definition.TYPE_CREATE_PRODUCT);
	}
	
	public ProductTransaction(NetworkParams network, byte[] payloadBytes) throws ProtocolException {
		this(network, payloadBytes, 0);
	}
	
	public ProductTransaction(NetworkParams network, byte[] payloadBytes, int offset) throws ProtocolException {
		super(network, payloadBytes, offset);
	}
	
	@Override
	public void verfify() throws VerificationException {
		super.verfify();
		
		if(product == null) {
			new VerificationException("商品信息不存在");
		}
		
		//是否认证用户
		if(!isCertAccount()) {
			new VerificationException("普通账户不支持创建商品");
		}
	}
	
	@Override
	public void verfifyScript() {
		super.verfifyScript();
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		super.serializeToStream(stream);
		
		byte[] productBytes = product.serialize();
		stream.write(new VarInt(productBytes.length).encode());
		stream.write(productBytes);
	}
	
	@Override
	protected void parse() throws ProtocolException {
		super.parse();
		
		product = new Product(readBytes((int)readVarInt()));
		
		length = cursor - offset;
	}
	
	@Override
	protected boolean isCompatible() {
		return true;
	}

	@Override
	public String toString() {
		return "ProductTransaction [time=" + time + ", scriptSig=" + scriptSig + ", product=" + product + "]";
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}
	
}
