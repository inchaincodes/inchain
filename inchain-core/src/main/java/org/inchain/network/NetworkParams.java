package org.inchain.network;

import org.inchain.Configure;
import org.inchain.message.MessageSerializer;
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
	 * 得到创世块
	 * @return {@link BlockStore}
	 */
	public abstract BlockStore getGengsisBlock();
    
	/**
	 * 获取最新区块高度
	 * @return long
	 */
	public long getBestBlockHeight() {
		return blockStoreProvider.getBestBlockHeader().getHeight();
	}
	
    /**
     * 运行的地址前缀
     * @return int[]
     */
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }
    
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
}
