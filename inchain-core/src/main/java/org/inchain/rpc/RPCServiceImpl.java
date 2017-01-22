package org.inchain.rpc;

import org.inchain.account.Account;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.MainNetworkParams;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockStoreProvider;
import org.inchain.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPCServiceImpl implements RPCService {
	private final static Logger log = LoggerFactory.getLogger(RPCServiceImpl.class);

	private NetworkParams network = MainNetworkParams.get();
//	private BlockStoreProvider storeProvider =BlockStoreProvider.getInstace(Configure.DATA_BLOCK, network);
	private BlockStoreProvider storeProvider = null;
	private PeerKit peerKit = new PeerKit();
//	private AccountKit accountKit =  AccountKit.getInstace(network, peerKit);
	private AccountKit accountKit =  null;


	//获取区块的数量
	@Override
	public String getblockcount() {
		return String.valueOf(storeProvider.getBestBlockHeader().getBlockHeader().getHeight());
	}
	//获取最新区块的高度 
	@Override
	public String getnewestblockheight() {
	;
		return String.valueOf(storeProvider.getBestBlockHeader().getBlockHeader().getHeight());
	}
	//获取最新区块的hash
	@Override
	public String getnewestblockhash() {
		return String.valueOf(storeProvider.getBestBlockHeader().getBlockHeader().getHash());
	}
	//通过区块的hash或者高度获取区块的头信息
	@Override
	public String getblockheader(String height) {
		return String.valueOf(storeProvider.getBlockByHeight(Long.valueOf(height)));
	}
	//通过区块的hash或者高度获取区块的完整信息
	@Override
	public String getblock(String hash) {
		return String.valueOf(storeProvider.getBlock(Hex.decode(hash)));
	}
	//获取内存里的count条交易
	@Override
	public String getmempoolinfo(String count) {
		// TODO Auto-generated method stub
		return null;
	}
	//，同时必需指定帐户管理密码和交易密码
	@Override
	public String newaccount(String mgpw, String trpw) {
		try {
			Account account = accountKit.createNewCertAccount(mgpw, trpw, new byte[0]);
			return account.getAddress().getBase58();
		} catch (Exception e) {
			e.printStackTrace();
			return  e.getMessage();
		}

	}
	//获取帐户的地址
	@Override
	public String getaccountaddress() {
//		return accountKit.getAccountList().;
		return null;
	}
	//获取帐户的公钥
	@Override
	public String getaccountpubkeys() {
		// TODO Auto-generated method stub
		return null;
	}
	//备份私钥种子，同时显示帐户的hash160
	@Override
	public String dumpprivateseed() {
		// TODO Auto-generated method stub
		return null;
	}
	//获取帐户的余额
	@Override
	public String getblanace() {
		// TODO Auto-generated method stub
		return null;
	}
	//获取帐户的交易记录
	@Override
	public String gettransaction() {
		// TODO Auto-generated method stub
		return null;
	}

	

}
