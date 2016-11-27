package org.inchain.core.exception;

import java.util.Arrays;

/**
 * 网络异常，错误的网络连接会抛出该异常
 * @author ln
 *
 */
public class WrongNetworkException extends AddressFormatException {
	private static final long serialVersionUID = 8746857841857489865L;
	
	public int verCode;
    public int[] acceptableVersions;
    
    public WrongNetworkException(int verCode, int[] acceptableVersions) {
        super("Version code of address did not match acceptable versions for network: " + verCode + " not in " +
          Arrays.toString(acceptableVersions));
        this.verCode = verCode;
        this.acceptableVersions = acceptableVersions;
    }
}
