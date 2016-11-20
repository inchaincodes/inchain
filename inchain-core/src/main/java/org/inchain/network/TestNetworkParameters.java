package org.inchain.network;

import java.util.ArrayList;
import java.util.List;

import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.DefaultMessageSerializer;
import org.inchain.message.MessageSerializer;
import org.inchain.script.ScriptBuilder;
import org.inchain.store.BlockStore;
import org.inchain.store.TransactionStore;
import org.inchain.transaction.RegisterTransaction;
import org.inchain.transaction.Transaction;
import org.inchain.transaction.TransactionInput;
import org.inchain.utils.Hex;
import org.inchain.utils.Utils;

/**
 * 测试网络
 * @author ln
 *
 */
public class TestNetworkParameters extends NetworkParameters {
	
	private static TestNetworkParameters instance;
    public static synchronized TestNetworkParameters get() {
        if (instance == null) {
            instance = new TestNetworkParameters();
        }
        return instance;
    }
    
    public TestNetworkParameters() {
    	this.seedManager = new RemoteSeedManager();
    	this.port = 8322; 
    	init();
	}
    

	public TestNetworkParameters(SeedManager seedManager, int port) {
    	this.seedManager = seedManager;
    	this.port = port;
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
		BlockStore gengsisBlock = new BlockStore(this);
		
		gengsisBlock.setPreHash(Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")));
		gengsisBlock.setHeight(0);
		gengsisBlock.setTime(1478070769l);

		//交易列表
		List<TransactionStore> txs = new ArrayList<TransactionStore>();
		
		//产出货币总量
		Transaction coinBaseTx = new Transaction(this);
		coinBaseTx.setVersion(Transaction.VERSION);
		coinBaseTx.setType(Transaction.TYPE_COINBASE);
		
		TransactionInput input = new TransactionInput();
		coinBaseTx.addInput(input);
		input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));
		
		coinBaseTx.addOutput(Coin.valueOf(100000000l), Address.fromBase58(this, "ThYf64mTNLSCKhEW1KVyVkAECRanhWeJC"));
		coinBaseTx.verfify();
		coinBaseTx.verfifyScript();
		
		txs.add(new TransactionStore(this, coinBaseTx));
		
		//注册创世帐户
		RegisterTransaction regTx = new RegisterTransaction(this, Hex.decode("0101000000010100a547304502210082bb8fdd903133c032a00c843254c387571ec9af6372183ecf9bffc227f89cdd02207f64221d24c866ee418fc838d96543241fe676ac65bd99a16f0af6d72e2719004630440220156d0ff9e76f8834b98899881b614720bd66e9be3404fffe9364299930c48c460220118f83f1ed6f9461087a6daa991c1e4cebd572e1d5ae6e6d9ded223ffa71019051145a29d0959ae0445f837f3534304cde744a87a9e801a3145a29d0959ae0445f837f3534304cde744a87a9e888c16321035a734e4998d176a4c0b94016333e70a61a8c470ae3f854e718da34c95d960bb42102334ec39ecc5454f6899444d78f20025e68d6a0c2356c1cee84c23feb9bd658d86721021b32e71d92968bcb4c63b5adece8e1181d1a7f60c8649d35ecb9e6fc8aeeff6021029e9a848cc3c3bd112b45770d77db058a9c4d17c5fdb353d046c682b3d07f223168ac00000000"));
		regTx.verfify();
		regTx.verfifyScript();
		
		txs.add(new TransactionStore(this, regTx));
		
		gengsisBlock.setTxs(txs);
		gengsisBlock.setTxCount(txs.size());
		
		Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("86c346bda7710779081dd0a89205ad72240c20da44578362d62076a683e94533".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");
		
		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getHash());
		}
		Utils.checkState("59a03c5f24966e6b438e7f1d699d240fa74329f58ad10f992780b796e9e39b73".equals(Hex.encode(gengsisBlock.getHash().getBytes())), "the gengsis block hash is error");
		
		return gengsisBlock;
	}
	
	public static void main(String[] args) {
		TestNetworkParameters network = get();
		network.getGengsisBlock();
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
