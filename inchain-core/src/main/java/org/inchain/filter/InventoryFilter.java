package org.inchain.filter;

import org.inchain.utils.RandomUtil;
import org.springframework.stereotype.Component;

/**
 * 向量清单过滤器
 * @author ln
 *
 */
@Component
public class InventoryFilter {
	
	private BloomFilter filter = new BloomFilter(1000000, 0.0001, RandomUtil.randomLong());
	
	public BloomFilter getFilter() {
		return filter;
	}
	
	public void insert(byte[] object) {
		filter.insert(object);
	}
	
	public boolean contains(byte[] object) {
		return filter.contains(object);
	}
	
	public void clear() {
		filter = new BloomFilter(1000000, 0.0001, RandomUtil.randomLong());
	}
}
