package org.inchain.signers;

import org.inchain.crypto.ECKey;
import org.inchain.crypto.ECKey.ECDSASignature;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.ConsensusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共识消息签名
 * @author ln
 *
 */
public final class ConsensusSigner {

	private static final Logger log = LoggerFactory.getLogger(ConsensusSigner.class);
	
	/**
	 * 共识消息签名
	 */
	public static void sign(ConsensusMessage message, ECKey key) {
		
		try {
			Sha256Hash hash = Sha256Hash.twiceOf(message.getBodyBytes());
			ECDSASignature signature = key.sign(hash);
			
	        byte[] sign = signature.encodeToDER();
	        message.setSign(sign);
		} catch (Exception e) {
			log.error("共识消息签名出错", e);
		}
	}
}
