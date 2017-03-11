package org.inchain.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.inchain.Configure;
import org.inchain.service.CreditCollectionService;
import org.springframework.stereotype.Service;

/**
 * 信用采集服务
 * //TODO 定时清理过期的资源，以免造成OOM
 * @author ln
 *
 */
@Service
public class CreditCollectionServiceImpl implements CreditCollectionService {
	
	private Map<Integer, Map<ByteHash, Long>> container = new HashMap<Integer, Map<ByteHash, Long>>();

	@Override
	public boolean verification(int type, byte[] hash160, long time) {
		Map<ByteHash, Long> map = container.get(type);
		if(map == null) {
			return true;
		}
		Long lastTime = map.get(new ByteHash(hash160));
		if(lastTime == null) {
			return true;
		}
		if(time - lastTime >= Configure.CERT_CHANGE_PAY_INTERVAL) {
			return true;
		}
		return false;
	}

	@Override
	public boolean addCredit(int type, byte[] hash160, long time) {
		Map<ByteHash, Long> map = container.get(type);
		if(map == null) {
			map = new HashMap<ByteHash, Long>();
			container.put(type, map);
		}
		map.put(new ByteHash(hash160), time);
		return true;
	}

	@Override
	public boolean onload() {
		// TODO Auto-generated method stub
		return false;
	}

	static class ByteHash {
		byte[] hash;

		public ByteHash(byte[] hash) {
			this.hash = hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof ByteHash)) {
				return false;
			}
			ByteHash temp = (ByteHash)obj;
			if(hash == null || temp.hash == null) {
				return false;
			}
			return Arrays.equals(hash, temp.hash);
		}
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(hash);
		}
	}
}
