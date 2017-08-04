package org.inchain.transaction.business;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.AccountBody;
import org.inchain.account.Address;
import org.inchain.core.TimeService;
import org.inchain.core.Definition;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ContentErrorExcetption;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 认证账户注册交易
 * @author ln
 *
 */
public class CertAccountRegisterTransaction extends CertAccountTransaction {

	//帐户主体
	private AccountBody body;
	
	public CertAccountRegisterTransaction(NetworkParams network, byte[] hash160, byte[][] mgPubkeys, byte[][] trPubkeys, AccountBody body,byte[] superhash160,int superlevel) {
		super(network);
		this.setVersion(Definition.VERSION);
		this.setType(Definition.TYPE_CERT_ACCOUNT_REGISTER);
		this.hash160 = hash160;
		this.mgPubkeys = mgPubkeys;
		this.trPubkeys = trPubkeys;
		this.body = body;
		this.superhash160 = superhash160;
		this.level = superlevel+1;
	}
	
	public CertAccountRegisterTransaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
		this(params, payloadBytes, 0);
    }
	
	public CertAccountRegisterTransaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
		super(params, payloadBytes, offset);
	}
	
	/**
	 * 反序列化交易
	 */
	protected void parseBody(){
		hash160 = readBytes(Address.LENGTH);
		superhash160 = readBytes(Address.LENGTH);
		level = readBytes(1)[0] & 0xff;
		//主体
		try {
			body = new AccountBody(readBytes((int) readVarInt()));
		}catch (ContentErrorExcetption e){
			body = AccountBody.empty();
		}
		//账户管理公钥
		int mgPubkeysCount = readBytes(1)[0] & 0xff;
		mgPubkeys = new byte[mgPubkeysCount][];
		for (int i = 0; i < mgPubkeysCount; i++) {
			mgPubkeys[i] = readBytes((int) readVarInt());
		}
		
		//交易公匙
		int trPubkeysCount = readBytes(1)[0] & 0xff;
		trPubkeys = new byte[trPubkeysCount][];
		for (int i = 0; i < trPubkeysCount; i++) {
			trPubkeys[i] = readBytes((int) readVarInt());
		}
	}
	
	@Override
	protected void serializeBodyToStream(OutputStream stream) throws IOException {
		//hash 160
		Utils.checkNotNull(hash160);
		stream.write(hash160);

		//superhash 160
		Utils.checkNotNull(superhash160);
		stream.write(superhash160);

		stream.write(level);
		
		//主体
		Utils.checkNotNull(body);
		byte[] bodyContent = body.serialize();
        stream.write(new VarInt(bodyContent.length).encode());
		stream.write(bodyContent);
		
		//帐户管理公匙
		Utils.checkNotNull(mgPubkeys);
		//公钥个数
        stream.write(mgPubkeys.length);
		for (byte[] bs : mgPubkeys) {
			//公钥长度
	        stream.write(new VarInt(bs.length).encode());
			stream.write(bs);
		}
		
		//交易公匙
		Utils.checkNotNull(trPubkeys);
        stream.write(trPubkeys.length);
		for (byte[] bs : trPubkeys) {
			//公钥长度
	        stream.write(new VarInt(bs.length).encode());
			stream.write(bs);
		}
	}
	
	/**
	 * 验证交易的合法性
	 */
	@Override
	public void verify() throws VerificationException {
		super.verify();
		if(body == null || body.serialize().length > MAX_BODY_LENGTH) {
			throw new VerificationException("主体信息错误");
		}
	}

	public AccountBody getBody() {
		return body;
	}

	public void setBody(AccountBody body) {
		this.body = body;
	}

	public void setLevel(int level){this.level = level;}

	public int getLevel(){return this.level;}

	public void setSuperhash160(byte[] supervisor){this.superhash160 = supervisor;}

	public byte[] getSuperhash160(){return this.superhash160;}
	
}
