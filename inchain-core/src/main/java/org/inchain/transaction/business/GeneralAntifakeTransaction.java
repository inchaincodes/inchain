package org.inchain.transaction.business;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Account;
import org.inchain.core.Definition;
import org.inchain.core.Product;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.crypto.Sha256Hash;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.script.ScriptOpCodes;
import org.inchain.utils.RandomUtil;
import org.inchain.utils.Utils;

/**
 * 普通防伪交易流程，商家生产的防伪码，验证之前不会广播到链上，在用户验证时才会收录进区块链
 * @author ln
 *
 */
public class GeneralAntifakeTransaction extends CommonlyTransaction {
	
	/** 关联产品，商家产品信息事先广播到连上时，直接进行关联，也可以不事先创建，具体方式根据商家所选择的方式进行判断 **/
	protected Sha256Hash productTx;
	/** 产品信息，和关联产品不能同时存在 **/
	protected Product product;
	
	/** 商品编号，类似于商品身份证，用户防止生成重复的防伪码 **/
	protected long nonce;
	/** 商品验证密码，商家产生的防伪码信息并不会立即广播到链上，会首先存储在应用里面，
	 *  届时存储数据会用该密码进行aes加密存储，用户验证时输入密码解密然后广播 **/
	protected long password;
	
	/** 商家签名信息，验证该产品确实是对应商家生产的，该签名是对product与nonce、password进行的签名 **/
	protected byte[] signVerification;
	protected Script signVerificationScript;
	
	/** 经度，用户验证时的地理位置，不参与防伪码id 的hash，仅用户商家防窜货分析 **/
	protected double longitude;
	/** 纬度，用户验证时的地理位置，不参与防伪码id 的hash，仅用户商家防窜货分析 **/
	protected double latitude;
 
	public GeneralAntifakeTransaction(NetworkParams network, Product product) throws ProtocolException {
		this(network, product, RandomUtil.randomLong(), RandomUtil.randomLong());
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, byte[] payloadBytes) throws ProtocolException {
		this(network, payloadBytes, 0);
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, byte[] payloadBytes, int offset) throws ProtocolException {
		super(network, payloadBytes, offset);
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, Product product, long nonce, long password) throws ProtocolException {
		super(network);
		
		this.product = product;
		this.nonce = nonce;
		this.password = password;
		
		type = Definition.TYPE_GENERAL_ANTIFAKE;
	}
	
	public GeneralAntifakeTransaction(NetworkParams network, Sha256Hash productTx, long nonce, long password) throws ProtocolException {
		super(network);
		
		this.productTx = productTx;
		this.nonce = nonce;
		this.password = password;
		
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
		
		if(scriptBytes == null) {
			throw new VerificationException("签名信息不存在");
		}
		
		if(signVerificationScript == null) {
			signVerificationScript = new Script(signVerification);
		}
		
		if(!signVerificationScript.isCertAccount()) {
			throw new VerificationException("不合法的商家签名");
		}
		
		if(scriptSig.isCertAccount()) {
			throw new VerificationException("不合法的商家签名");
		}
	}
	
	@Override
	public void verfifyScript() {
		super.verfifyScript();
		//验证商家签名
		try {
			signVerificationScript.runVerify(getAntifakeHashWithoutSign());
		} catch (IOException e) {
			throw new VerificationException(e.getMessage());
		}
	}
	
	/**
	 * 除转帐交易外的其它交易，通用的签名方法
	 * 如果账户已加密的情况，则需要先解密账户
	 * @param account
	 * @throws IOException 
	 */
	public void makeSign(Account account) throws VerificationException, IOException {
		
		//必须是认证账户进行签名生产
		if(account == null || !account.isCertAccount()) {
			throw new VerificationException("错误的账户");
		}
		
		//认证账户
		if(account.getAccountTransaction() == null) {
			throw new VerificationException("签名失败，认证账户没有对应的庄户信息交易");
		}
		
		ECKey[] keys = account.getTrEckeys();
		
		if(keys == null) {
			throw new VerificationException("账户没有解密？");
		}
		
		//要签名的内容，product + nonce + password
		Sha256Hash antifakeHash = getAntifakeHashWithoutSign();
		
		ECDSASignature ecSign = keys[0].sign(antifakeHash);
		byte[] sign1 = ecSign.encodeToDER();
		
		ecSign = keys[1].sign(antifakeHash);
		byte[] sign2 = ecSign.encodeToDER();
		
		signVerificationScript = ScriptBuilder.createCertAccountScript(ScriptOpCodes.OP_VERTR, account.getAccountTransaction().getHash(), account.getAddress().getHash160(), sign1, sign2);
		signVerification = signVerificationScript.getProgram();
	}
	
	/**
	 * 获取防伪码的内容，排除签名信息
	 * @return byte[]
	 * @throws IOException
	 */
	public byte[] getAntifakeConentWithoutSign() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			stream.write(type);
			Utils.uint32ToByteStreamLE(version, stream);
			//时间
			Utils.int64ToByteStreamLE(time, stream);
			
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

			Utils.int64ToByteStreamLE(nonce, stream);
			Utils.int64ToByteStreamLE(password, stream);
			
			return stream.toByteArray();
		} finally {
			stream.close();
		}
	}

	/**
	 * 获取防伪码的内容，包含签名信息，最终的防伪码会是这个内容的hash值
	 * @return byte[]
	 * @throws IOException
	 */
	public byte[] getAntifakeConent() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			Utils.checkNotNull(signVerification);
			
			stream.write(getAntifakeConentWithoutSign());
			stream.write(signVerification);
			
			return stream.toByteArray();
		} finally {
			stream.close();
		}
	}
	
	/**
	 * 获取防伪内容的hash， 这个hash会充当防伪码
	 * @return Sha256Hash
	 * @throws IOException 
	 */
	public Sha256Hash getAntifakeHash() throws IOException {
		return Sha256Hash.twiceOf(getAntifakeConent());
	}
	
	/**
	 * 获取防伪内容的签名前的hash，用户验证认证账户的前面
	 * @return Sha256Hash
	 * @throws IOException 
	 */
	public Sha256Hash getAntifakeHashWithoutSign() throws IOException {
		return Sha256Hash.twiceOf(getAntifakeConentWithoutSign());
	}

	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {

		if(signVerification == null && scriptBytes == null) {
			throw new VerificationException("没有签名");
		}
		
		stream.write(type);
		Utils.uint32ToByteStreamLE(version, stream);
		//时间
		Utils.int64ToByteStreamLE(time, stream);
		
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

		Utils.int64ToByteStreamLE(nonce, stream);
		Utils.int64ToByteStreamLE(password, stream);
		
		stream.write(new VarInt(signVerification.length).encode());
		stream.write(signVerification);
		
		Utils.doubleToByteStream(longitude, stream);
		Utils.doubleToByteStream(latitude, stream);
		
		//签名
		if(scriptBytes != null) {
			stream.write(new VarInt(scriptBytes.length).encode());
			stream.write(scriptBytes);
		}
	}
	
	@Override
	protected void parse() throws ProtocolException {

		type = readBytes(1)[0] & 0XFF;
		
		version = readUint32();
		time = readInt64();
		
		int productType = readBytes(1)[0] & 0xff;
		if(productType == 1) {
			productTx = readHash();
		} else {
			product = new Product(readBytes((int)readVarInt()));
		}
		nonce = readInt64();
		password = readInt64();
		
		signVerification = readBytes((int)readVarInt());
		signVerificationScript = new Script(signVerification);
		
		longitude = readDouble();
		latitude = readDouble();
		
		if(hasMoreBytes()) {
			scriptBytes = readBytes((int)readVarInt());
			scriptSig = new Script(this.scriptBytes);
		}
		
		length = cursor - offset;
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
	
	public Sha256Hash getProductTx() {
		return productTx;
	}

	public void setProductTx(Sha256Hash productTx) {
		this.productTx = productTx;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
	}

	public long getPassword() {
		return password;
	}

	public void setPassword(long password) {
		this.password = password;
	}

	public byte[] getSignVerification() {
		return signVerification;
	}

	public void setSignVerification(byte[] signVerification) {
		this.signVerification = signVerification;
	}

	public Script getSignVerificationScript() {
		return signVerificationScript;
	}

	public void setSignVerificationScript(Script signVerificationScript) {
		this.signVerificationScript = signVerificationScript;
	}

	@Override
	public String toString() {
		return "GeneralAntifakeTransaction [version=" + version + ", time=" + time + ", scriptSig=" + scriptSig
				+ ", productTx=" + productTx + ", product=" + product + ", nonce=" + nonce + ", password=" + password
				+ ", signVerificationScript=" + signVerificationScript + ", longitude=" + longitude + ", latitude=" + latitude + "]";
	}
	
}
