package org.inchain.consensus;

/**
 * 开采待确认交易/也就在大家常说的挖矿
 * @author ln
 *
 */
public interface Mining {
	
	void start();
	
	void stop();
	
	int status();
}
