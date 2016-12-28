package org.inchain.message;

import org.inchain.crypto.Sha256Hash;

/**
 * 向量清单
 * @author ln
 *
 */
public class InventoryItem {

	//单个清单长度,1 byte 的type， 32 byte的hash
	public static final int MESSAGE_LENGTH = 33;
    
    public enum Type {
        Transaction,
        Block,
        NewBlock,
    }
    //类型
    private final Type type;
    //hash
    private final Sha256Hash hash;
    
    public InventoryItem(Type type, Sha256Hash hash) {
        this.type = type;
        this.hash = hash;
    }
    
    public Type getType() {
		return type;
	}
    
    public Sha256Hash getHash() {
		return hash;
	}
    
    @Override
    public String toString() {
    	return type+":"+hash;
    }
}
