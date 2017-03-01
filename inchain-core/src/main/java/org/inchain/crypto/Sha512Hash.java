package org.inchain.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author ln
 *
 */
public class Sha512Hash {

	public static byte[] hash(byte[] input) {
        return hash(input, 0, input.length);
    }
	
	public static byte[] hash(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest();
    }
	
	public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }
}
