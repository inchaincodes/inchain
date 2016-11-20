package org.inchain.network;

import org.inchain.message.DefaultMessageSerializer;
import org.inchain.message.MessageSerializer;
import org.inchain.store.BlockStore;

public class MainNetParams extends NetworkParameters {
	
	private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }
    
    public MainNetParams() {
    	this.seedManager = new RemoteSeedManager();
    	this.acceptableAddressCodes = new int[]{0x00,0x01,0x02,0x3,0x9};
	}
    
    public MainNetParams(SeedManager seedManager, int port) {
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

}
