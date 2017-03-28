package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.inchain.script.Script;

public interface Input {

	/**
	 * 序列化交易输入
	 * @param stream
	 * @throws IOException 
	 */
	public void serialize(OutputStream stream) throws IOException;
	
	public byte[] getScriptBytes();
	
	public void setScriptBytes(byte[] scriptBytes);
	
	void clearScriptBytes();
	void setScriptSig(Script scriptSig);
	Script getScriptSig();

	Script getFromScriptSig();
	
	boolean addFrom(TransactionOutput from);
	List<TransactionOutput> getFroms();
	void setFroms(List<TransactionOutput> froms);
}
