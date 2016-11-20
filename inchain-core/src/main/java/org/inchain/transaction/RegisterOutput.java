package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.account.Account;
import org.inchain.core.VarInt;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.utils.Utils;

public class RegisterOutput extends TransactionOutput {

	private Account account;
	
	public RegisterOutput(Account account) {
		this.account = account;
		this.setScript(ScriptBuilder.createRegisterOutScript(account.getAddress().getHash160(), 
				account.getMgPubkeys(), account.getTrPubkeys()));
	}
	
	public RegisterOutput(Script script) {
		this.setScript(script);
	}
	
	/**
	 * 序列化交易输出
	 * @param stream
	 * @throws IOException 
	 */
	public void serialize(OutputStream stream) throws IOException {
		Utils.checkNotNull(account);
		
		setScript(ScriptBuilder.createRegisterOutScript(account.getAddress().getHash160(), 
				account.getMgPubkeys(), account.getTrPubkeys()));
		byte[] sb = getScriptBytes();
		stream.write(new VarInt(sb.length).encode());
		stream.write(sb);
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
}
