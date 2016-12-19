package org.inchain.network;

import java.net.InetSocketAddress;

import org.inchain.Configure;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.DefaultMessageSerializer;
import org.inchain.message.MessageSerializer;
import org.inchain.store.BlockStore;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 单元测试本地网络
 * @author ln
 *
 */
public class UnitNetworkParams extends NetworkParams {
	
    public UnitNetworkParams() {
    	init();
	}
    

	public UnitNetworkParams(SeedManager seedManager) {
    	init();
	}
    
	/*
	 */
	private void init() {
		// 初始化单元测试的地址前缀
		int[] codes = new int[254];
		for (int i = 0; i < 254; i++) {
			codes[i] = i;
		}
		this.acceptableAddressCodes = codes;
		
		seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", Configure.PORT), true, 25000));
	}
	
	/**
	 * 单元测试的创世块
	 */
	@Override
	public BlockStore getGengsisBlock() {
		
		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("0000000000000000000000000000000000000000000000000000000000000000000000009a041ee62f2a528ef70f85b6229b668afc7a37baa3ea690d1417c257989ce66cf1911958000000000301010000000112117468697320612067656e67736973207478000000000200e1f50500000000181424de55a2b2d32ed83c87b86a381e918ada34927b76c3ac00e1f50500000000181424de55a2b2d32ed83c87b86a381e918ada34927b76c3ac00000000311b327412b0d2855945aa2dc03d5cc1684152f4573f420f0003010000001b327412b0d2855945aa2dc03d5cc1684152f457f19119586a2103f19192388289b8dbeb7649aa76996c948e9d18172969a9358af970997af40de546304402207e7553d13d0bf4fa630feb252107fc944fecba8a7e275521e1fd2dd7692c6ddc022073fd2ff40cd4c023a732dc823e979ea4464a5f2033981ba05a40106193e24504ac"));
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("9a041ee62f2a528ef70f85b6229b668afc7a37baa3ea690d1417c257989ce66c".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getHash());
		}
		Utils.checkState("f388da4f984346ea964f3e758aa405d97810d2283ccc265e1ca1574604367e28".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
		return gengsisBlock;
	}
	
	@Override
	public int getProtocolVersionNum(ProtocolVersion version) {
		return version.getVersion();
	}

	@Override
	public MessageSerializer getSerializer(boolean parseRetain) {
		return new DefaultMessageSerializer(this);
	}

}
