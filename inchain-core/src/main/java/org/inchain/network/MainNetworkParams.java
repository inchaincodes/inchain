package org.inchain.network;

import org.inchain.message.DefaultMessageSerializer;
import org.inchain.message.MessageSerializer;
import org.inchain.store.BlockStore;

/**
 * 主网各参数
 * @author ln
 *
 */
public class MainNetworkParams extends NetworkParams {
	
	private static MainNetworkParams instance;
    public static synchronized MainNetworkParams get() {
        if (instance == null) {
            instance = new MainNetworkParams();
        }
        return instance;
    }
    
    public MainNetworkParams() {
    	
    	
    	this.seedManager = new RemoteSeedManager();
    	
    	init();
	}

	public MainNetworkParams(SeedManager seedManager, int port) {
    	this.seedManager = seedManager;
    	this.port = port;
    	
    	init();
	}

	private void init() {
    	this.packetMagic = 86022698l;
    	this.acceptableAddressCodes = new int[] {getSystemAccountVersion(), getCertAccountVersion()};
	}
    
	@Override
	public int getProtocolVersionNum(ProtocolVersion version) {
		return version.getVersion();
	}

	@Override
	public MessageSerializer getSerializer(boolean parseRetain) {
		return new DefaultMessageSerializer(this);
	}

	@Override
	public BlockStore getGengsisBlock() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 主网，普通地址以i开头
	 */
	@Override
	public int getSystemAccountVersion() {
		return 102;
	}


	/**
	 * 主网，认证地址以V开头
	 */
	@Override
	public int getCertAccountVersion() {
		return 70;
	}

}
