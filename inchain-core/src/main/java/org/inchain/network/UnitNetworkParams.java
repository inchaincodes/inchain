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
		packetMagic = 112288l;
		
		// 初始化单元测试的地址前缀
		this.acceptableAddressCodes = new int[] {getSystemAccountVersion(), getCertAccountVersion()};
		
		seedManager = new NodeSeedManager();
		seedManager.add(new Seed(new InetSocketAddress("127.0.0.1", Configure.PORT), true, 25000));
	}
	
	/**
	 * 单元测试的创世块
	 */
	@Override
	public BlockStore getGengsisBlock() {
		
		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("000000000000000000000000000000000000000000000000000000000000000000000000585e54e4672219d47ce41867d9b5b362406918e5eea7c0743cbedad1a105b216f1911958000000000b01010000000112117468697320612067656e67736973207478000000000100ca9a3b000000001814b804d8951f5e1a51f6ecc6a4be3482fe18cc59d976c3ac0000000000000000631b327412b0d2855945aa2dc03d5cc1684152f4573f420f0003010000001b327412b0d2855945aa2dc03d5cc1684152f457bdfbe0a66a2103f19192388289b8dbeb7649aa76996c948e9d18172969a9358af970997af40de546304402206867a2a53f3f0257062e81f617d7951757332edf0d278e4c76ef6750c6855ce802203fb2ea960b0a01fc2bb4319861ea7ef6242ba7355e214d9b013967a19a2204d5ac63c7287c88e1d18c1d8d90cf80964f80e0b15981043f420f000301000000c7287c88e1d18c1d8d90cf80964f80e0b1598104d1fbe0a66a210220c15dc89c60dc28d2009daf5db92cd9561a6507ac63f1c233177b96bea6f341463044022021a5e56ba2f35d438be35f36fbe8c55dc4c3282b564d52151c7461c61938ab54022025a98f27ddb6908ba18f4a7c51c1bba3cdc63df8b3fb3c17b2b8575421fdafe0ac630f9d9fd4f86dcba5c65fbbe0967ebec8dedd2ece3f420f0003010000000f9d9fd4f86dcba5c65fbbe0967ebec8dedd2eced4fbe0a66a2102c433e43449bf9255e5a7297675b15baca86762cf7ec7df104d787f297c5df35e46304402204fabaaf11315bf04a1c719f492ae40b2eab2c63b7b0b762998dc9806e70c593b02203b642383e5a63c88c5c582372533a0ab01810920ffc11c696cd165cce582b448ac63f71ba248fd8131d01d5bf6e6a76d90de11ef2aac3f420f000301000000f71ba248fd8131d01d5bf6e6a76d90de11ef2aacd7fbe0a66b21028699c4bae08e9ea89e7478b1b160bbeb949bf74083acf550e0b5f2b291474e1e473045022100fedfa0db2f5543fb554ce8bbfd4f0e0689eb882604445add98d484f9cb6666a40220108855aab6176c82191c0fcb5c6ef310402928e35797e61cd69128e13ea19503ac63ad700c2fd1346b58f3a6d7e6f5367f73c96b7e6a3f420f000301000000ad700c2fd1346b58f3a6d7e6f5367f73c96b7e6ad9fbe0a66b2102ebbc1be7aa396cd0aeabe41a245a0a08c8beda9c5e2f6bf95ff3bc1c64195c61473045022100fd83a0dbd416e6c8f56695e54774346914b211db5579f95024b83f332340260c02200d441aba24e8ff3aa6778714091665a0eb70738d31d5685bf3cca32ad2e2862dac"));
		
		Sha256Hash merkleHash = gengsisBlock.getBlock().buildMerkleHash();
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("585e54e4672219d47ce41867d9b5b362406918e5eea7c0743cbedad1a105b216".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getBlock().getHash());
		}
		Utils.checkState("0d438118c28d4b3644779d18032db8af3a5dfac6d2d004212e90473380e0cb62".equals(Hex.encode(gengsisBlock.getBlock().getHash().getBytes())), "the gengsis block hash is error");
		
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

	@Override
	public byte[] getCommunityManagerHash160() {
		return null;
	}
	/**
	 * 单元测试，普通地址以u开头
	 */
	@Override
	public int getSystemAccountVersion() {
		return 130;
	}


	/**
	 * 单元测试，认证地址以r开头
	 */
	@Override
	public int getCertAccountVersion() {
		return 122;
	}

}
