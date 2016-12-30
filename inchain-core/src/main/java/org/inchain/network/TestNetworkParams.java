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
    	this.seedManager = new RemoteSeedManager();
    	init();
	}
    

	public TestNetworkParams(SeedManager seedManager) {
    	this.seedManager = seedManager;
    	init();
	}
    
	private void init() {
		
		packetMagic = 66926688l;
		
		this.acceptableAddressCodes = new int[] {getSystemAccountVersion(), getCertAccountVersion()};
	}
	
	/**
	 * 测试网络的创世块
	 */
	@Override
	public BlockStore getGengsisBlock() {
		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("000000000000000000000000000000000000000000000000000000000000000000000000f738fa8c5e988d2dfb40188e68c5cdc3edf73488ee78fd452969dff335897b3df1911958000000001001010000000112117468697320612067656e67736973207478000000000100ca9a3b000000001814bb7e4d6ffb3266a0b533b21847bef4dacce95f4676c3ac0000000000000000634237cccf6ff9a7674a8fc47c60278fdd23e9f3093f420f0003010000004237cccf6ff9a7674a8fc47c60278fdd23e9f3090f75024f6b2102883a9625a871e79c070e316edafc97eac2f6c97b218d0e55726f25be2a963e92473045022100e9b62a99317cc3b788cb96223df36bbd223216c4c5ed6ecac9ab4e7c45e3cf12022000d67e12ffae64023bd5dd6f3e70aec77631b9cfd820f5881a677a3f62aaee56ac632c1179b1e05fb8652644f6c61a033d28431009123f420f0003010000002c1179b1e05fb8652644f6c61a033d28431009121875024f6b2103a7db4a854e3f2ae3a30aa759bfd39045f6a80c967b3fd81036acebc54b557ece473045022100c56c8ac22403f465ec7aaf1b78de9510c0207f9211545558e8c35222dda292e202202e7fe77b76c5da762e6c240dc486a4a5b1ff18efa9c89e085e6cd85ff985bccbac63b3fcd687a5c2b2617de0d504cfa7d12db93bb00c3f420f000301000000b3fcd687a5c2b2617de0d504cfa7d12db93bb00c1b75024f6a21033e371b2f3d42dd525833bcce12cb0d404fb73898bdea3e6794bd8ec552c9ba9c463044022012b1009b08e2b7366299709799991b4d648600b683835783b69acc178909c7a702201368547c30559d2c8b57934c22256e6b12c5682cb62c0f9e4b8db006b3598890ac63c3bfdb8a67f35b6e4ea1ee3ae7b91eff58ec81a83f420f000301000000c3bfdb8a67f35b6e4ea1ee3ae7b91eff58ec81a81d75024f6a2103bf7759e9b81f8ff1b99732b3ee36aea0abff68c818b8839c318ad350e9642cb046304402201690d057082aa822d0b0a9038d0fd7cfca538753209176191ad6dd13cd2008b80220126bb31edf60901a737e1e3ce3d629a0a087ea5e224ecb38bb54ead53456c1b2ac635f1a078b610d92a3110b2c1a7f880b0d2442e5303f420f0003010000005f1a078b610d92a3110b2c1a7f880b0d2442e5301f75024f6b210200d482122f8537ad2fa1af1914e088d6b308824a8ef4dc8b68fa0e0a88c484e3473045022100d1bd5ea7a9d87dc731573773711cec536242ecbac16d3e1466819094be109c37022041187143d2db3452991c601d036b595f955be33f912ee4594a68819b3ba4d542ac63df639c6066a817a6be28b532839f8e1e85594cd43f420f000301000000df639c6066a817a6be28b532839f8e1e85594cd42175024f6a2103e8769d1901a4ef006054405c16716d6648fffe628385db5586c7fa18c3df5da846304402207ddd5d0a67c007c370a9a33784d1a0611d83fa46c7b11327f39d1cfa45b55d2c02206f98d61609a319ef0d1b7c2786c16a4298caacf1e0075cd5a584e7461cb254abac63e3453a80144f6ac24baf01f20455c6167b9854853f420f000301000000e3453a80144f6ac24baf01f20455c6167b9854852275024f6a2103368f8061e02694e37da199c07c3e2f2506cdd748efee493f0d1c52ae7494428f4630440220052736d82208ff4e72597197b4b3ba745ce2e2eaf832c98ab8fbd8e85fd89659022044e313cf4f2350ee48a3b4067008e0e7dd88e6a6547b583d59281bc927a8c349ac0b010000000000000000000000209f25d38efcf3b9a1832e6480fffbe7ccb49ba41ee9878de5ba86e58db0e993bee7a791e68a80e69c89e99990e585ace58fb8022102d06b679c33838c27fd4315376618d97b000fdd467f19c21c8f7f86f4ebe8b0b321032d171ed5eca13309eb134a60eb889bff26a754141ba55c9602d94cb237c74fe602210300777b64f7b8282065524ef442b783441a09f2d45cb8f6fddf4984fc99e4a48021020172e17f39d5397f135b162b28fd54f001e7693c05136d87310839004b3b791ac9c1200000000000000000000000000000000000000000000000000000000000000000c314000000000000000000000000000000000000000087463044022046862e4536c87bf63c39dc62faea52fae94ea822047ff287eae2f42997c03eec02204e7b07d044b419c23dd18410fecceecc71accf30bb4470fe78cfe60a0eb0964d473045022100b8fdb880ae94a2d19c655fe71db609da0c3c74ea841176c348475168f5000a5a022070d22edefb14cca8813ca44f4b8cde53b7ac904662b0c61f6eb0615975bb17ccac"));
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("f738fa8c5e988d2dfb40188e68c5cdc3edf73488ee78fd452969dff335897b3d".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getHash());
		}
		Utils.checkState("6861c3eef5c5f0c8efdcb6179b9a40070cc5076e40b99336025d733c773615d8".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
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
		return Hex.decode("209f25d38efcf3b9a1832e6480fffbe7ccb49ba4");
	}
	
	/**
	 * 测试网络，普通地址以t开头
	 */
	@Override
	public int getSystemAccountVersion() {
		return 128;
	}

	/**
	 * 测试网络，认证地址以c开头
	 */
	@Override
	public int getCertAccountVersion() {
		return 88;
	}
}
