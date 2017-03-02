package org.inchain.message;

import org.inchain.core.exception.ProtocolException;
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
        Consensus,
    	Unknown;

    	public static Type from(int code) {
			Type[] types = Type.values();
			for (Type type : types) {
				if(type.ordinal() == code) {
					return type;
				}
			}
			Type res = Type.Unknown;
			res.setCode(code);
			return res;
		}

    	int code;
		private void setCode(int code) {
			this.code = code;
		}
		public int getCode() {
			if(code > 0) {
				return code;
			} else {
				return ordinal();
			}
		}
    }
    //类型
    private final Type type;
    //hash
    private final Sha256Hash hash;
    
    public InventoryItem(Type type, Sha256Hash hash) {
    	if(type == Type.Unknown) {
            throw new ProtocolException("Unknown inv type: " + type.getCode());
    	}
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
