package org.inchain.rpc;

import java.io.File;

import org.inchain.Configure;
import org.inchain.account.Address;
import org.inchain.kits.AccountKit;
import org.inchain.kits.PeerKit;
import org.inchain.network.MainNetParams;
import org.inchain.network.NetworkParameters;
import org.inchain.network.TestNetworkParameters;
import org.inchain.store.BlockStoreProvider;
import org.inchain.utils.Hex;
import org.iq80.leveldb.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPCServiceImpl implements RPCService {
	private final static Logger log = LoggerFactory.getLogger(RPCServiceImpl.class);

	private NetworkParameters network = MainNetParams.get();
	private BlockStoreProvider storeProvider =BlockStoreProvider.getInstace(Configure.DATA_BLOCK, network);
	private PeerKit peerKit = new PeerKit(network);
	private AccountKit accountKit =  AccountKit.getInstace(network, peerKit);


	//获取区块的数量
	@Override
	public String getblockcount() {
		return String.valueOf(storeProvider.getBestBlockHeader().getHeight());
	}
	//获取最新区块的高度 
	@Override
	public String getnewestblockheight() {
	;
		return String.valueOf(storeProvider.getBestBlockHeader().getHeight());
	}
	//获取最新区块的hash
	@Override
	public String getnewestblockhash() {
		return String.valueOf(storeProvider.getBestBlockHeader().getHash());
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
		Address accountInfo=null;
		try {
			 accountInfo= accountKit.createNewAccount(mgpw, trpw);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  e.getMessage();
		}
		return accountInfo.getHash().toString();

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
