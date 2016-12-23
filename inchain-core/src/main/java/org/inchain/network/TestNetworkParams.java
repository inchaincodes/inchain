package org.inchain.network;

import org.inchain.crypto.Sha256Hash;
import org.inchain.message.DefaultMessageSerializer;
import org.inchain.message.MessageSerializer;
import org.inchain.store.BlockStore;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 测试网络
 * @author ln
 *
 */
public class TestNetworkParams extends NetworkParams {
	
    public TestNetworkParams() {
    	this.seedManager = new NodeSeedManager();
    	init();
	}
    

	public TestNetworkParams(SeedManager seedManager) {
    	this.seedManager = seedManager;
    	init();
	}
    
	private void init() {
		int[] codes = new int[254];
		for (int i = 0; i < 254; i++) {
			codes[i] = i;
		}
		this.acceptableAddressCodes = codes;
	}
	
	/**
	 * 测试网络的创世块
	 */
	@Override
	public BlockStore getGengsisBlock() {
		
		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("00000000000000000000000000000000000000000000000000000000000000000000000074b42d11fff7e4d92cc851e6047a4359c6f3862990d58e53db76c886d69eda59f1911958000000000b01010000000112117468697320612067656e67736973207478000000000200e1f50500000000181424de55a2b2d32ed83c87b86a381e918ada34927b76c3ac00e1f50500000000181424de55a2b2d32ed83c87b86a381e918ada34927b76c3ac00000000311b327412b0d2855945aa2dc03d5cc1684152f4573f420f0003010000001b327412b0d2855945aa2dc03d5cc1684152f457f19119586a2103f19192388289b8dbeb7649aa76996c948e9d18172969a9358af970997af40de546304402207e7553d13d0bf4fa630feb252107fc944fecba8a7e275521e1fd2dd7692c6ddc022073fd2ff40cd4c023a732dc823e979ea4464a5f2033981ba05a40106193e24504ac31c7287c88e1d18c1d8d90cf80964f80e0b15981043f420f000301000000c7287c88e1d18c1d8d90cf80964f80e0b1598104f19119586a210220c15dc89c60dc28d2009daf5db92cd9561a6507ac63f1c233177b96bea6f34146304402202627888137d82d4c4112fd8b71c4cf8cc15ac54d3fabb8fbea388f1b1521c8ac02205c204cebbff0517ff8ddf0fa8ff8c7ab65fa45464f21b471173655cbed5eac3eac310f9d9fd4f86dcba5c65fbbe0967ebec8dedd2ece3f420f0003010000000f9d9fd4f86dcba5c65fbbe0967ebec8dedd2ecef19119586a2102c433e43449bf9255e5a7297675b15baca86762cf7ec7df104d787f297c5df35e463044022046e58836fa2710d9d7b3e33d9e0e5b84b8e318ed2d9dac9af251db2a661c791f02201bf58820523f47a16eaa8fd826a70ab7587182736e2e9961d9a323d97e8626c8ac31f71ba248fd8131d01d5bf6e6a76d90de11ef2aac3f420f000301000000f71ba248fd8131d01d5bf6e6a76d90de11ef2aacf19119586b21028699c4bae08e9ea89e7478b1b160bbeb949bf74083acf550e0b5f2b291474e1e473045022100f16947c23a128cd0a5f666dfe96233e2bf866ad108c0e1881c04f897d6e7b238022009fa9506aedeeae5df177f943bb2bb2fe0b1535c933f4e6aa4f45836bbbe3a71ac31ad700c2fd1346b58f3a6d7e6f5367f73c96b7e6a3f420f000301000000ad700c2fd1346b58f3a6d7e6f5367f73c96b7e6af19119586a2102ebbc1be7aa396cd0aeabe41a245a0a08c8beda9c5e2f6bf95ff3bc1c64195c6146304402203b6782987fc35edf56ec4960bbf953880a901d532666caffbe7cdcea93fdc216022010e8fb0b92f7fc7526467cb60491fbb16527fa8aedcb64929b33b317243c2fcaac"));
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("74b42d11fff7e4d92cc851e6047a4359c6f3862990d58e53db76c886d69eda59".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getHash());
		}
		Utils.checkState("05ede060c6027ef70bdcff81878a22530f18e998622500922d54a3913e9526bb".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
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
