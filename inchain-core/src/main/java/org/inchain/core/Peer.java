package org.inchain.core;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.inchain.listener.BlockDownendListener;
import org.inchain.message.Message;
import org.inchain.message.PingMessage;
import org.inchain.message.VersionMessage;
import org.inchain.msgprocess.DefaultMessageProcessFactory;
import org.inchain.msgprocess.MessageProcess;
import org.inchain.msgprocess.MessageProcessFactory;
import org.inchain.msgprocess.MessageProcessResult;
import org.inchain.network.NetworkParams;
import org.inchain.utils.RandomUtil;
import org.slf4j.LoggerFactory;

public class Peer extends PeerSocketHandler {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Peer.class);
	
	//异步顺序执行所有接收到的消息，以免有处理时间较长的线程阻塞，影响性能
	private ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	//消息处理器工厂
	private static MessageProcessFactory messageProcessFactory = DefaultMessageProcessFactory.getInstance();

	//监听器 bengin
	private BlockDownendListener blockDownendListener;
	//监听器 end
	
	private NetworkParams network;

	//节点版本信息
	private VersionMessage peerVersionMessage;
	//节点握手完成
	private boolean handshake = false;
	
	public Peer(NetworkParams network, InetSocketAddress address) {
		this(network, new PeerAddress(address));
	}
	
	public Peer(NetworkParams network, PeerAddress peerAddress) {
		super(network, peerAddress);
		this.network = network;
		this.peerAddress = peerAddress;
	}

	@Override
	protected void processMessage(final Message message) throws Exception {
		final MessageProcess messageProcess = messageProcessFactory.getFactory(message);
		executorService.submit(new Thread(){
			public void run() {
				MessageProcessResult result = messageProcess.process(message, Peer.this);
			};
		});
	}

	@Override
	public int getMaxMessageSize() {
		return Message.MAX_SIZE;
	}
	
	@Override
	public void connectionClosed() {
		log.info("connectionClosed");
	}

	@Override
	public void connectionOpened() {
		log.info("connectionOpened {}", this);
		//发送版本信息
		sendMessage(new VersionMessage(network, network.getBestBlockHeight(), getPeerAddress()));
	}

	public void ping() {
		sendMessage(new PingMessage(RandomUtil.randomLong()));
	}
	
	public PeerAddress getPeerAddress() {
		return peerAddress;
	}

	public NetworkParams getNetwork() {
		return network;
	}
	
	public VersionMessage getPeerVersionMessage() {
		return peerVersionMessage;
	}
	public void setPeerVersionMessage(VersionMessage peerVersionMessage) {
		this.peerVersionMessage = peerVersionMessage;
	}

	public boolean isHandshake() {
		return handshake;
	}

	public void setHandshake(boolean handshake) {
		this.handshake = handshake;
	}
	
	public void setBlockDownendListener(BlockDownendListener blockDownendListener) {
		this.blockDownendListener = blockDownendListener;
	}
	
	public BlockDownendListener getBlockDownendListener() {
		return blockDownendListener;
	}
}
