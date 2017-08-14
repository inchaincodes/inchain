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
 * 测试网络
 * @author ln
 *
 */
public class TestNetworkParams extends NetworkParams {

	public TestNetworkParams() {
		seedManager = new RemoteSeedManager();

//    	seedManager.add(new Seed(new InetSocketAddress("47.93.16.125", Configure.DEFAULT_PORT)));
//    	seedManager.add(new Seed(new InetSocketAddress("119.23.249.26", Configure.DEFAULT_PORT)));
//   	seedManager.add(new Seed(new InetSocketAddress("119.23.253.3", Configure.DEFAULT_PORT)));
//    	seedManager.add(new Seed(new InetSocketAddress("119.23.254.99", Configure.DEFAULT_PORT)));
		seedManager.add(new Seed(new InetSocketAddress("47.92.26.16", Configure.DEFAULT_PORT)));
		seedManager.add(new Seed(new InetSocketAddress("47.92.29.121", Configure.DEFAULT_PORT)));
		seedManager.add(new Seed(new InetSocketAddress("47.92.4.19", Configure.DEFAULT_PORT)));
		//seedManager.add(new Seed(new InetSocketAddress("192.168.1.2", Configure.DEFAULT_PORT)));
		//seedManager.add(new Seed(new InetSocketAddress("192.168.1.187", Configure.DEFAULT_PORT)));
		init();
	}


	public TestNetworkParams(SeedManager seedManager) {
		this.seedManager = seedManager;
		init();
	}

	private void init() {

		id = ID_TESTNET;

		packetMagic = 629989898L;

		this.acceptableAddressCodes = new int[] {getSystemAccountVersion(), getCertAccountVersion()};
	}

	/**
	 * 测试网络的创世块
	 */
	@Override
	public BlockStore getGengsisBlock() {
		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("000000000000000000000000000000000000000000000000000000000000000000000000468c50de3239958d3b033822885064475b6f063f311410b3a4ee71b7c70d14a61f8291590000000000001f829159822102883a9625a871e79c070e316edafc97eac2f6c97b218d0e55726f25be2a963e9276a9144237cccf6ff9a7674a8fc47c60278fdd23e9f30988463044022066c2882034761439d34de4b5cf95629cbc30232e3cc1589cffb2ca45c1211b1b02207a30573a7c6352558f8d7d0d5576f5d0d65ea1d7a8bfb41909e8a0da36378aeeac140101000000010012117468697320612067656e6773697320747800000000010080faca73f91f00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f829159000000000002010000000101e392fa7e2708247c54cb127f2f77c68fdd2080a40e4900b23ba2818f2a034586000000006b483045022100db98b98e8342cde32b577e91279adba21d78c117d3b214d6d84a4abcff5062cd0220231d77b6b82b71f2bd2c25db6691e35078182c6230c4e18b232bb42cfddf31e301210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020080c6a47e8d03000078c2481976a9144237cccf6ff9a7674a8fc47c60278fdd23e9f30988ac00003426f56b1c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f829159000000000006010000001f829159832102883a9625a871e79c070e316edafc97eac2f6c97b218d0e55726f25be2a963e9276a9144237cccf6ff9a7674a8fc47c60278fdd23e9f309884730450221009c9c96fe2c835da5351fe928c42ec9d8f6a2316f17d368555d411faafabdabae02201a2f9da36392061fcc1754a7701597034dae30e2fea6343a7dc9a2f77d9d20c7ac4237cccf6ff9a7674a8fc47c60278fdd23e9f309010000000000000000000000000000000000000000000000000000000000000000000000000000000000020100000001012ffa7c6d4e14561bd941da7d3dfe6ea08b91818817b4274be20d489e8e1ff883010000006b483045022100b062ac75877889f1a8d2126471962ba8b9cb1be5fa458813c1cdb045255fc83402205403f029defbc0a0e45fba42984b2eed3faffcf9ee8b40cc4bcb421324b39bc601210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a9144237cccf6ff9a7674a8fc47c60278fdd23e9f30988ac00f08e510c6b1c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f8291590000000000030100000000001f8291590000000000832102883a9625a871e79c070e316edafc97eac2f6c97b218d0e55726f25be2a963e9276a9144237cccf6ff9a7674a8fc47c60278fdd23e9f30988473045022100f75b0ddb41ffe3fabc6f90432aefc56187f96ce8c74c6f9b4a5f128f9b7e9ca702201ebc98e1ba09a51bd334052801fb4fd8d4183226f83583b940076cbc4e0b2cd4acaf6663e04237cccf6ff9a7674a8fc47c60278fdd23e9f30906010000001f829159832103a7db4a854e3f2ae3a30aa759bfd39045f6a80c967b3fd81036acebc54b557ece76a9142c1179b1e05fb8652644f6c61a033d284310091288473045022100c8dd620854008896194878ed42a93dde83a97d81815392396f51faea56eab6aa02202c4583385e640806314481b2b7436818d9cdeaa9c0a5804996c8ca96909f0e87ac2c1179b1e05fb8652644f6c61a033d2843100912010000000000000000000000000000000000000000000000000000000000000000000000000000000000020100000001017fcd114548df3467d1b78643670d78f244b588b0740929fc2536bc5a66a451a9010000006a47304402207037b76411ba35aa2fe20d14234703981100fbced19e1ea4486f84a7abb9abd9022044db4954e30ed563af36a52a9caa6dc5c0ffc16909a4d072ffaddad20358448001210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a9142c1179b1e05fb8652644f6c61a033d284310091288ac00e0e97c236a1c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f8291590000000000030100000000001f8291590000000000822103a7db4a854e3f2ae3a30aa759bfd39045f6a80c967b3fd81036acebc54b557ece76a9142c1179b1e05fb8652644f6c61a033d284310091288463044022057a02a57b5a8c41214c6d8f9ddb5c6e5045e334cb5a575f95d90cc3ac53ff40f0220797e09a1f9ee3c0f30cdd741b5c7f8e162a7963607ec5f80e96ebb40dcb478fdacbe6663e02c1179b1e05fb8652644f6c61a033d284310091206010000001f8291598221033e371b2f3d42dd525833bcce12cb0d404fb73898bdea3e6794bd8ec552c9ba9c76a914b3fcd687a5c2b2617de0d504cfa7d12db93bb00c884630440220397041b637e650d2f1eefcb5dbd01561c30770807b15e3423356803b1a6f4064022003378a2e1f8bd34335b657494995050e2048c8dc806def7b8dce608ae6672161acb3fcd687a5c2b2617de0d504cfa7d12db93bb00c010000000000000000000000000000000000000000000000000000000000000000000000000000000000020100000001012323b1edca12d46b92456990336616404b243fe63a0f9dc1c03dc50545efedc6010000006b483045022100b69a5b08d125d8736a659409ecd1c03187b59f0edb281f5b34c6ab04eea2b9cf02201f06c6f9357f343014c34f445739d4e2902cc2381beacdc1ab45c476e64c1e1e01210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a914b3fcd687a5c2b2617de0d504cfa7d12db93bb00c88ac00d044a83a691c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f8291590000000000030100000000001f82915900000000008321033e371b2f3d42dd525833bcce12cb0d404fb73898bdea3e6794bd8ec552c9ba9c76a914b3fcd687a5c2b2617de0d504cfa7d12db93bb00c88473045022100cad29ba66f3fb54978d2dc4ea704801f75a98c380243370046fdfcfec60d502902204166ccfe357dfe38b8862598b3ee6921de187840a1f8f45b5c1b9d22006e6bbbacc66663e0b3fcd687a5c2b2617de0d504cfa7d12db93bb00c06010000001f829159832103bf7759e9b81f8ff1b99732b3ee36aea0abff68c818b8839c318ad350e9642cb076a914c3bfdb8a67f35b6e4ea1ee3ae7b91eff58ec81a888473045022100d63e1d9dff8cd465541dfcf1dcaa7110e600ea9bcda55a38729b0e55d8949c2102203875bd570b0a7123e3397c83241cb7057186b472e9c1aadf0880c5f465ce5568acc3bfdb8a67f35b6e4ea1ee3ae7b91eff58ec81a801000000000000000000000000000000000000000000000000000000000000000000000000000000000002010000000101cedee5b8bc5515a8410f8897ebbac2da81d9db470a688b679f6eab5b9c06cd28010000006b48304502210096784d475314dcfc3a44edc399b83c932956fc486409167c55a2fcce6cc05ef3022032095039f24a16a2eafb08d11cb2d30e8c1a2115a48e400a2b0e06afdf5352ef01210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a914c3bfdb8a67f35b6e4ea1ee3ae7b91eff58ec81a888ac00c09fd351681c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f829159000000000006010000001f82915983210200d482122f8537ad2fa1af1914e088d6b308824a8ef4dc8b68fa0e0a88c484e376a9145f1a078b610d92a3110b2c1a7f880b0d2442e53088473045022100e6d31aca1666b269b68d50c9afa64b73b1cdc276fde5028807859da7f23615dc022056f9f1c01a37ab57bb4d9431f50ed1905be7297828ece0e6f93f422013fc38aeac5f1a078b610d92a3110b2c1a7f880b0d2442e5300100000000000000000000000000000000000000000000000000000000000000000000000000000000000201000000010184f78e5e16f40c1691e02161a3947df26a67312cbe305dab8ff30316a9e4fdc4010000006b483045022100aa3f8e3b1fdd484e606276be35f95588d7461002cb14780246a2f3be00df46000220223308d4506278230f3e93d106029bd70b033e77b546dc35035dc50ecc68a7b901210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a9145f1a078b610d92a3110b2c1a7f880b0d2442e53088ac00b0fafe68671c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f829159000000000006010000001f829159822103e8769d1901a4ef006054405c16716d6648fffe628385db5586c7fa18c3df5da876a914df639c6066a817a6be28b532839f8e1e85594cd4884630440220688175fa36e09a6fbf15dbe7f328a785b3de4cdbbc5d373f617ebb0b05d90ff202207fbb87c701950abd4b84b1885882bd669b2daa99906e0c2064f0b44b0381f00cacdf639c6066a817a6be28b532839f8e1e85594cd4010000000000000000000000000000000000000000000000000000000000000000000000000000000000020100000001013abd60921b4f0efffb1ca62ddc9c77a0031a66b7cb2796b9f6b6e845ddb733ee010000006a473044022019b7d2defcbd708a8789d6726ad7e4382fdf1cca30117e070fba6aee3c6cb3fa022042ea79b8a589a3b729e42e5cf5b603d24f0078d959444112aeed91243b06117c01210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a914df639c6066a817a6be28b532839f8e1e85594cd488ac00a0552a80661c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f829159000000000006010000001f829159832103368f8061e02694e37da199c07c3e2f2506cdd748efee493f0d1c52ae7494428f76a914e3453a80144f6ac24baf01f20455c6167b985485884730450221009e05367a97f06514956a8e8d09915b141e3988659ae743103f34e7e2c21cca190220416a10552dc2d1a06f6169e6f769ebfbce03debe2f047d9ba622086b0f667d2dace3453a80144f6ac24baf01f20455c6167b98548501000000000000000000000000000000000000000000000000000000000000000000000000000000000002010000000101c18604d2f0bfe109974609371e206cf99a13d15381cf3adf14afdb15b07e161d010000006a473044022074997920b1afc4196eba67e493226efa0578ca9cb1c3cd92ea77700ba258e391022069229a692b46d732224c721511cad381f70b2f36d69ac4901eacd634d5ee7b1701210276c52ff14acd4c4d3e08f9596e0127a15e98207b3d0024426364825cd724c86dffffffff020010a5d4e8000000000000001976a914e3453a80144f6ac24baf01f20455c6167b98548588ac0090b05597651c00000000001976a914bb7e4d6ffb3266a0b533b21847bef4dacce95f4688ac1f82915900000000000b0100000043da7e5982c220256303b7fa1e89761bf37c1a8415524dbcb9ec1b4033ce71d4224699e8459235c314dd1c14ee4f4642eb9c3e14cc97a3c3d3629708ea88473045022100c50a56f6bb25ea2ade3b83def48d7b55401916f711a634a8b2163b555343cef502203cda9a0c6735c72cf4599def1eaec90f32206e69ded301be46168aa2ad0286aaacdd1c14ee4f4642eb9c3e14cc97a3c3d3629708eadd1c14ee4f4642eb9c3e14cc97a3c3d3629708ea01fd23022b046e616d6506e5908de7a7b01ee9878de5ba86e58db0e993bee7a791e68a80e69c89e99990e585ace58fb822076164647265737306e59cb0e59d8012e9878de5ba86e5b882e58d97e5b2b8e58cba44046c6f676f06e59bbee7898737687474703a2f2f66696c652e696e636861696e2e6f72672f696d616765732f696e636861696e5f6c6f676f5f313030783130302e706e672b0a637265646974436f64650ce4bfa1e794a8e4bba3e7a0811239313530303130384d41355542333248334e1a0570686f6e6506e794b5e8af9d0c3032332d383633333130363927077765627369746506e5ae98e7bd911768747470733a2f2f7777772e696e636861696e2e6f7267fd1d0108646573637269707406e68f8fe8bfb0fd0a01e9878de5ba86e58db0e993bee7a791e68a80e69c89e99990e585ace58fb8e698afe4b880e5aeb6e4bba5e58cbae59d97e993bee68a80e69cafe9a9b1e58aa8e79a84e5889be696b0e59e8be4bc81e4b89aefbc8ce585b6e4b8bbe5afbce79a84e58cbae59d97e993bee7a4bee58cbae9a1b9e79bae496e636861696e2de58db0e993bee698afe4b880e4b8aae4bba5e998b2e4bcaae4b8bae59fbae7a180e4b89ae58aa1e79a84e585ace5bc80e5b9b3e58fb0efbc8ce4b8bae7a4bee4bc9ae59084e4bc81e4b89ae38081e69cbae69e84e38081e889bae69cafe5aeb6e7ad89e68f90e4be9be59381e7898ce38081e79fa5e8af86e4baa7e69d83e4bf9de68aa4e69c8de58aa1e380820221023cbfda1ae93a41187634206177a0482ee2c890540bd907a7f6fbdde9e5a31c3b2103489e3933f41327c2463b664776a4355860cf5b093c106d21f0253f83dda76465012103df243c24dfe4ea420c776e8d46fbc2a86c6e439648119f64b583a38a0c7d84fd00"));

		Sha256Hash merkleHash = gengsisBlock.getBlock().buildMerkleHash();

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("468c50de3239958d3b033822885064475b6f063f311410b3a4ee71b7c70d14a6".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getBlock().getHash());
		}
		Utils.checkState("94af383968be80f2942b04b5e69f5105f74dafb508f8c54c5dd28c19c54d43a9".equals(Hex.encode(gengsisBlock.getBlock().getHash().getBytes())), "the gengsis block hash is error");

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


		//return Hex.decode("8e168bb4963f0523b9ee1c734d3c142976581930");//root
		//return Hex.decode("b326aeb9c6710864441b7c6a282ce924b4c31749");//manager
		return Hex.decode("481e05c83604abf86cca029c4260674c45c85c58");//cer_manager_1
	}

	@Override
	public byte[] getCommunityManagerHash160() {
		return Hex.decode("481e05c83604abf86cca029c4260674c45c85c58");//cer_manager_1
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
