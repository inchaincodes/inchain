package org.inchain.network;

import org.inchain.Configure;
import org.inchain.message.MessageSerializer;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStore;
import org.inchain.store.BlockStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 网络参数，网络协议参数配置都在本类下面
 * 主要有3个实现，主网、测试网络、单元测试
 * @author ln
 *
 */
public abstract class NetworkParams {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
    public static final String ID_MAINNET = "org.inchain.production";
    public static final String ID_TESTNET = "org.inchain.test";
	
	protected String id;
	
	//p2p网络端口
	protected int port = Configure.PORT;
	//网络协议魔法参数
	protected long packetMagic;
	
    //允许的地址前缀
    protected int[] acceptableAddressCodes = {};

	//种子管理器
	protected SeedManager seedManager;
	
	//消息序列化工具
	protected transient MessageSerializer defaultSerializer = null;
	
	@Autowired
	protected BlockStoreProvider blockStoreProvider;
	
	//网络最新高度
	protected long bestHeight;
	
	/**
	 * 获取默认的消息序列化工具
	 * @return {@link MessageSerializer}
	 */
    public final MessageSerializer getDefaultSerializer() {
    	//简单的单例
        if (null == this.defaultSerializer) {
            synchronized(this) {
            	//没有初始化，那么现在开始初始化
                if (null == this.defaultSerializer) {
                    this.defaultSerializer = getSerializer(false);
                }
            }
        }
        return defaultSerializer;
    }
	
	/**
	 * 得到创世块
	 * @return {@link BlockStore}
	 */
	public abstract BlockStore getGengsisBlock();
	
    /**
     * 不同的网络可能用到不同的消息序列化工具，这里交给具体的子类去实现
     * @param parseRetain
     * @return {@link MessageSerializer}
     */
    public abstract MessageSerializer getSerializer(boolean parseRetain);
	
    /**
     * 获取协议的版本号
     * @param version 协议版本 {@link ProtocolVersion}
     * @return int
     */
	public abstract int getProtocolVersionNum(final ProtocolVersion version);

	/**
	 * 获取该网络的认证管理账号的hash160
	 * @return byte[]
	 */
	public abstract byte[] getCertAccountManagerHash160();
	
	public static enum ProtocolVersion {
        CURRENT(1);

        private final int version;

        ProtocolVersion(final int version) {
            this.version = version;
        }

        public int getVersion() {
            return version;
        }
    }
    
	/**
	 * 获取最新区块高度
	 * @return long
	 */
	public long getBestBlockHeight() {
		BlockHeaderStore blockHeader = blockStoreProvider.getBestBlockHeader();
		if(blockHeader == null) {
			return 0l;
		} else {
			return blockHeader.getBlockHeader().getHeight();
		}
	}
	
	/**
	 * 获取最新区块头信息
	 * @return long
	 */
	public BlockHeaderStore getBestBlockHeader() {
		return blockStoreProvider.getBestBlockHeader();
	}
	
    /**
     * 运行的地址前缀
     * @return int[]
     */
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }
	
	/**
	 * 得到普通账户的地址版本号
	 * @return int
	 */
	public abstract int getSystemAccountVersion();
	
	/**
	 * 得到认证账户的地址版本号
	 * @return int
	 */
	public abstract int getCertAccountVersion();
    
	public int getPort() {
		return port;
	}
	
	public SeedManager getSeedManager() {
		return seedManager;
	}
	
	public long getPacketMagic() {
        return packetMagic;
    }
    
    public String getId() {
		return id;
	}

	public void setBlockStoreProvider(BlockStoreProvider blockStoreProvider) {
		this.blockStoreProvider = blockStoreProvider;
	}

	public long getBestHeight() {
		return bestHeight;
	}

	public void setBestHeight(long bestHeight) {
		this.bestHeight = bestHeight;
	}
}
