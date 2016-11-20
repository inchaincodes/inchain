package org.inchain.transaction;

import java.io.IOException;
import java.io.OutputStream;

import org.inchain.script.Script;

public interface Output {

	/**
	 * 序列化交易输出
	 * @param stream
	 * @throws IOException 
	 */
	public void serialize(OutputStream stream) throws IOException;
	
	public byte[] getScriptBytes();
	
	public void setScriptBytes(byte[] scriptBytes);
	
	void setScript(Script scriptSig);
	
	Script getScript();
}
