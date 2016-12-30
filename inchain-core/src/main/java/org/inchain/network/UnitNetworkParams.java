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
		
		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("00000000000000000000000000000000000000000000000000000000000000000000000038f9e80369e6dbae8acdcd17a1ab1a881bebaca67ebca6a4ba29c8928abf6becf1911958000000000c01010000000112117468697320612067656e67736973207478000000000100ca9a3b000000001814b804d8951f5e1a51f6ecc6a4be3482fe18cc59d976c3ac0000000000000000631b327412b0d2855945aa2dc03d5cc1684152f4573f420f0003010000001b327412b0d2855945aa2dc03d5cc1684152f457aead184f6b2103f19192388289b8dbeb7649aa76996c948e9d18172969a9358af970997af40de5473045022100dadceaae46f0967dd8864ee4b6de0beabfe2c092738a05c6e77ac177c3d884b602201d9320f051fef9603a100f96febd06ca5737a82ce6304edf8fa540184c18e5daac63c7287c88e1d18c1d8d90cf80964f80e0b15981043f420f000301000000c7287c88e1d18c1d8d90cf80964f80e0b1598104b9ad184f6b210220c15dc89c60dc28d2009daf5db92cd9561a6507ac63f1c233177b96bea6f341473045022100a9010def84eba59639a797608759bd69e339bf2a3975047b37fa99c12e42581202200bae381940afef294f8f4e8d044bf700da0863b7a95e2ca031f9a90c7783fdbfac630f9d9fd4f86dcba5c65fbbe0967ebec8dedd2ece3f420f0003010000000f9d9fd4f86dcba5c65fbbe0967ebec8dedd2ecebcad184f6a2102c433e43449bf9255e5a7297675b15baca86762cf7ec7df104d787f297c5df35e463044022043616bfbeb808868c3ae614df6998c2f20351494fad3511a5338e61f535ed25c02201111a3de918f515f11a0469756503a48a94f363d5b47ecb68f43b4893c4c8334ac63f71ba248fd8131d01d5bf6e6a76d90de11ef2aac3f420f000301000000f71ba248fd8131d01d5bf6e6a76d90de11ef2aacbead184f6a21028699c4bae08e9ea89e7478b1b160bbeb949bf74083acf550e0b5f2b291474e1e463044022045ae80343edf6a29ce7c0f5f1f00c42c08f56e7fec987c46848d833dcbd382bc02207af0ca295502a46e274337823b77c7ab640fce4c223442f0d3f5caf04da74163ac63ad700c2fd1346b58f3a6d7e6f5367f73c96b7e6a3f420f000301000000ad700c2fd1346b58f3a6d7e6f5367f73c96b7e6ac1ad184f6a2102ebbc1be7aa396cd0aeabe41a245a0a08c8beda9c5e2f6bf95ff3bc1c64195c6146304402204002c8a85fc96c72d94001beeaba19d5754f1008723ae787405dad3c7329fe2d02201f3eb16b7cc75cf4ed0db1f57c314929f75e5493505ca8e151c8dbdf893935c5ac0b01000000000000000000000098ba9559d02ae15f34b0209a87377f1e59c501730100022103ebe369f63421457abbca40b3295a3980db5488ee34d56ebe8d488f1d5d301f8321022700a96f3fd3b0d082abfd8bd758502f2e7e881eeaa5c69662c8eac7ade6d4330221028b3106d4cac5218388d2249503abab63c9b5de10525d13299d0423ab6f455a402103ca686fce8b25c1dd648e5dcbed9a8c95d8d5ec28baaefdbd7af2117fc22a6286c9c1200000000000000000000000000000000000000000000000000000000000000000c3140000000000000000000000000000000000000000874630440220296e7127545692d5580fc89aa84060374fb733facb91709fc2d8591b746e4baf022040cf22ff7ca342528890d867050473d861f3266d17119e705c68b950c0ffea4e4730450221008075c85feeee35d99e83a2678919904ff0b15283c017531fd5900f47efb65a47022056419266e203ec18e12fd3d77d8c2b68477f91a5f14ffee38b6e7878968a5621ac"));
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("38f9e80369e6dbae8acdcd17a1ab1a881bebaca67ebca6a4ba29c8928abf6bec".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getHash());
		}
		Utils.checkState("8c4364ea0c48c23d27f0cfdb3c0a0ae6afa878a1e56eb64e40692ed815f01848".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
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
	
	/**
	 * 获取该网络的认证管理账号的hash160
	 * @return byte[]
	 */
	@Override
	public byte[] getCertAccountManagerHash160() {
		return Hex.decode("98ba9559d02ae15f34b0209a87377f1e59c50173");
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
