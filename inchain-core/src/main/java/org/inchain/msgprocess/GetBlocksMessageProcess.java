package org.inchain.msgprocess;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;

import org.inchain.core.Peer;
import org.inchain.crypto.Sha256Hash;
import org.inchain.message.DataNotFoundMessage;
import org.inchain.message.GetBlocksMessage;
import org.inchain.message.InventoryItem;
import org.inchain.message.InventoryMessage;
import org.inchain.message.Message;
import org.inchain.network.NetworkParams;
import org.inchain.store.BlockHeaderStore;
import org.inchain.store.BlockStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 区块信息获取消息处理器
 * @author ln
 *
 */
@Service
public class GetBlocksMessageProcess implements MessageProcess {
	
	public final static int MAX_COUNT = 1000;

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private NetworkParams network;
	//区块提供器
	@Autowired
	private BlockStoreProvider blockStoreProvider;
	
	public GetBlocksMessageProcess() {
	}
	
	/**
	 * 接收到区块拉取消息
	 */
	@Override
	public MessageProcessResult process(Message message, Peer peer) {
		
		if(log.isDebugEnabled()) {
			log.debug("receive get block message : {}", message);
		}
		
		GetBlocksMessage getBlockMessage = (GetBlocksMessage) message;
		//要获取的区块，从哪里开始
		Sha256Hash startHash = getBlockMessage.getStartHash();
		Sha256Hash stopHash = getBlockMessage.getStopHash();
		
		//验证
		BlockHeaderStore startBlockHeader = blockStoreProvider.getHeader(startHash.getBytes());
		//如果开始的块没有找到，则返回DataNotFound消息
		if(startBlockHeader == null) {
			try {
				peer.sendMessage(new DataNotFoundMessage(network, startHash));
			} catch (NotYetConnectedException | IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		BlockHeaderStore stopBlockHeader = null;
		
		if(!Sha256Hash.ZERO_HASH.equals(stopHash)) {
			stopBlockHeader = blockStoreProvider.getHeader(stopHash.getBytes());
		}
		
		//每次最大不能超过1000个
		if(stopBlockHeader == null || stopBlockHeader.getBlockHeader().getHeight() - startBlockHeader.getBlockHeader().getHeight() > MAX_COUNT) {
			BlockHeaderStore bestBlockHeader = blockStoreProvider.getBestBlockHeader();
			if(bestBlockHeader.getBlockHeader().getHeight() - startBlockHeader.getBlockHeader().getHeight() <= MAX_COUNT) {
				stopBlockHeader = bestBlockHeader;
			}
		}
		
		List<InventoryItem> list = new ArrayList<InventoryItem>();
		int count = 0;
		while (count < MAX_COUNT) {

			if(Sha256Hash.ZERO_HASH.equals(startBlockHeader.getNextHash()) || 
					(stopBlockHeader != null && startBlockHeader.getBlockHeader().getHash().equals(stopBlockHeader.getBlockHeader().getHash()))) {
				break;
			}
			
			list.add(new InventoryItem(InventoryItem.Type.Block, startBlockHeader.getNextHash()));
			count ++;
			startBlockHeader = blockStoreProvider.getHeader(startBlockHeader.getNextHash().getBytes());
		}
		try {
			peer.sendMessage(new InventoryMessage(peer.getNetwork(), list));
		} catch (NotYetConnectedException | IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
