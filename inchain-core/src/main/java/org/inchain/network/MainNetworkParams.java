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
    	this.acceptableAddressCodes = new int[]{0x00,0x01,0x02,0x3,0x9};
	}
    
    public MainNetworkParams(SeedManager seedManager, int port) {
    	this.seedManager = seedManager;
    	this.port = port;
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

	@Override
	public int getBestBlockHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

}
