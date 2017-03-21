package org.inchain.transaction;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.SpringContextUtils;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.Definition;
import org.inchain.core.TimeService;
import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.crypto.TransactionSignature;
import org.inchain.mempool.MempoolContainer;
import org.inchain.message.Message;
import org.inchain.network.NetworkParams;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.script.ScriptOpCodes;
import org.inchain.store.BlockStoreProvider;
import org.inchain.store.TransactionStore;
import org.inchain.utils.Utils;

/**
 * 交易
 * @author ln
 *
 */
public class Transaction extends Message {

	//锁定时间标识，小于该数表示为块数，大于则为秒级时间戳
	public static final int LOCKTIME_THRESHOLD = 500000000;
    public static final BigInteger LOCKTIME_THRESHOLD_BIG = BigInteger.valueOf(LOCKTIME_THRESHOLD);
    //允许的交易最大值
    public static final int MAX_STANDARD_TX_SIZE = 100000;
    
    //tx hash
    protected Sha256Hash hash;
	//交易输入
    protected List<Input> inputs;
	//交易输出
    protected List<Output> outputs;
	//交易时间
	protected long time;
	//锁定时间，小于0永久锁定，大于等于0为锁定的时间或者区块高度
	protected long lockTime;
	//交易版本
	protected long version;
	//交易类型
	protected int type;
	
	/**
	 * 签名类型
	 * @author ln
	 *
	 */
	public enum SigHash {
		//对整个交易签名
        ALL(1),
        //只签名输入部分
        SING_INPUT(2),
        
        NONE(3);

        public final int value;

        private SigHash(final int value) {
            this.value = value;
        }

        public byte byteValue() {
            return (byte) this.value;
        }
    }
	
	public Transaction(NetworkParams network) {
		super(network);
		inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
        time = TimeService.currentTimeMillis();
        version = Definition.VERSION;
	}

	public Transaction(NetworkParams params, byte[] payloadBytes) throws ProtocolException {
        super(params, payloadBytes, 0);
    }

	public Transaction(NetworkParams params, byte[] payloadBytes, int offset) throws ProtocolException {
        super(params, payloadBytes, offset);
    }
	
	/**
	 * 该协议是否新增协议，用于支持旧版本，就版本会解析成为UnkonwTransaction
	 * 当发布第一个版本之后，后面所有新增的协议，需覆盖该方法，并返回true
	 * 当需要兼容时，会在type后面带上长度
	 */
	public boolean isCompatible() {
		return false;
	}

	/**
	 * 序列化
	 */
	protected void serializeToStream(OutputStream stream) throws IOException {
		stream.write(type);
		Utils.uint32ToByteStreamLE(version, stream);
        stream.write(new VarInt(inputs.size()).encode());
        for (Input in : inputs)
            in.serialize(stream);
        stream.write(new VarInt(outputs.size()).encode());
        for (Output out : outputs)
            out.serialize(stream);
        Utils.int64ToByteStreamLE(time, stream);
        Utils.int64ToByteStreamLE(lockTime, stream);
    }
	
	/**
	 * 反序列化交易
	 */
	@Override
	protected void parse() throws ProtocolException {
		cursor = offset;
		
		type = readBytes(1)[0] & 0XFF;
		
		if(isCompatible()) {
			length = (int) readUint32();
		}
		version = readUint32();
		
		//交易输入数量
        long numInputs = readVarInt();
        inputs = new ArrayList<Input>((int) numInputs);
        for (int i = 0; i < numInputs; i++) {
        	Input input = parseInput();
            inputs.add(input);
        }

		//交易输出数量
        long numOutputs = readVarInt();
        outputs = new ArrayList<Output>((int) numOutputs);
        for (int i = 0; i < numOutputs; i++) {
        	TransactionOutput output = parseOutput();
        	output.setIndex(i);
            outputs.add(output);
        }
        time = readInt64();
        lockTime = readInt64();
        length = cursor - offset;
	}
	
	/**
	 * 反序列化交易的输出部分
	 * @return TransactionOutput
	 */
	protected TransactionOutput parseOutput() {
		TransactionOutput output = new TransactionOutput();
        output.setParent(this);
        output.setValue(readInt64());
        output.setLockTime(readInt64());
        //赎回脚本名的长度
        int signLength = (int)readVarInt();
        output.setScriptBytes(readBytes(signLength));
        return output;
	}
	
	/**
	 * 反序列化交易的输入部分
	 * @return Input
	 */
	protected <T extends Input> Input parseInput() {
		TransactionInput input = new TransactionInput();
        input.setParent(this);
        
        if(getType() == Definition.TYPE_COINBASE) {
        	int signLength = (int) readVarInt();
        	byte[] msg = readBytes(signLength);
        	//TODO
        	byte[] signMsg = new byte[msg.length -1];
        	System.arraycopy(msg, 1, signMsg, 0, signMsg.length);
        	
        	input.setScriptSig(ScriptBuilder.createCoinbaseInputScript(signMsg));
            input.setSequence(readUint32());
        	return input;
        }
        
    	//上笔交易的引用
        //TODO 这里上笔交易，必须查询？ 
        //验证脚本的时候再设置
        
        TransactionOutput pre = new TransactionOutput();
        Transaction t = new Transaction(network);
        pre.setParent(t);
        pre.getParent().setHash(Sha256Hash.wrap(readBytes(32)));
        pre.setIndex((int)readUint32());
        input.setFrom(pre);
    
        //输入签名的长度
        int signLength = (int)readVarInt();
        input.setScriptBytes(readBytes(signLength));
        input.setSequence(readUint32());

        //通过输入签名生成对应的赎回脚本
//        Script sc = input.getScriptSig();
//        if(sc.getChunks().size() == 2) {
//        	//普通账户
//        	ECKey key = ECKey.fromPublicOnly(sc.getPubKey());
//        	Script script = ScriptBuilder.createOutputScript(
//    				AccountTool.newAddressFromKey(network, network.getSystemAccountVersion(), key));
//    		
//            pre.setScript(script);
//        } else if(sc.getChunks().size() == 5) {
//        	//认证账户
//        	byte[] hash160 = sc.getChunks().get(4).data;
//        	Script script = ScriptBuilder.createOutputScript(
//    				new Address(network, network.getCertAccountVersion(), hash160));
//    		
//            pre.setScript(script);
//        } else {
//        	new VerificationException("错误的输入脚本");
//        }
        
        
        
        return input;
	}

//	/**
//	 * 验证交易的合法性
//	 */
//	public void verfify() throws VerificationException {
//		
//		if(type == TransactionDefinition.TYPE_COINBASE) {
//			return;
//		}
//		//是否引用了不可用的输出
//		for (int i = 0; i < inputs.size(); i++) {
//			Input input = inputs.get(i);
//			TransactionOutput fromOutput = input.getFrom();
//			if(fromOutput == null || fromOutput.getParent() == null) {
//				throw new VerificationException("交易引用不存在");
//			}
//			
//			BlockStoreProvider blockStoreProvider = SpringContextUtils.getBean(BlockStoreProvider.class);
//			TransactionStore txs = blockStoreProvider.getTransaction(fromOutput.getParent().getHash().getBytes());
//			Transaction tx = null;
//			if(txs == null) {
//				//区块里没有，则去内存池查看是否存在
//				tx = MempoolContainerMap.getInstace().get(fromOutput.getParent().getHash());
//				if(tx == null) {
//					throw new VerificationException("引用了不存在的交易");
//				}
//			} else {
//				tx = txs.getTransaction();
//			}
//			if(tx == null) {
//				throw new VerificationException("引用不存在的交易");
//			} else if(tx.getLockTime() == -1) {
//				throw new VerificationException("引用了不可用的交易");
//			}
//			TransactionOutput output = (TransactionOutput) tx.getOutput(fromOutput.getIndex());
//			if(output.getLockTime() == -1) {
//				throw new VerificationException("引用了不可用的交易");
//			}
//			input.setFrom(output);
//		}
//	}

	/**
	 * 验证交易脚本
	 */
	public void verifyScript() {

		//验证交易的输入脚本是否正确
		if(type == Definition.TYPE_COINBASE) {
			return;
		}
		if(!isPaymentTransaction()) {
			throw new VerificationException("交易类型不正确");
		}
		if(inputs != null) {
			for (int i = 0; i < inputs.size(); i++) {
				Input input = inputs.get(i);
				
				TransactionInput txInput = (TransactionInput) input;
				TransactionOutput fromOutput = txInput.getFrom();
				
				if(fromOutput.getScript() == null) {
					BlockStoreProvider blockStoreProvider = SpringContextUtils.getBean(BlockStoreProvider.class);
					TransactionStore txs = blockStoreProvider.getTransaction(fromOutput.getParent().getHash().getBytes());
					Transaction tx = null;
					if(txs == null) {
						//区块里没有，则去内存池查看是否存在
						tx = MempoolContainer.getInstace().get(fromOutput.getParent().getHash());
						if(tx == null) {
							throw new VerificationException("引用了不存在的交易");
						}
					} else {
						tx = txs.getTransaction();
					}
					if(tx == null) {
						throw new VerificationException("引用不存在的交易");
					} else if(tx.getLockTime() == -1) {
						throw new VerificationException("引用了不可用的交易");
					}
					TransactionOutput output = (TransactionOutput) tx.getOutput(fromOutput.getIndex());
					if(output.getLockTime() == -1) {
						throw new VerificationException("引用了不可用的交易");
					}
					input.setFrom(output);
				}
				
				Script outputScript = input.getFromScriptSig();
				if(outputScript == null) {
					throw new VerificationException("交易脚本验证失败，输入应用脚本不存在");
				}
				input.getScriptSig().run(this, i, outputScript);
			}
		}
		
	}

	public Sha256Hash hashForSignature(int index, byte[] redeemScript, byte sigHashType) {
		try {
            Transaction tx = this.network.getDefaultSerializer().makeTransaction(this.baseSerialize());
//            Transaction tx = this;

            //清空输入脚本
            for (int i = 0; i < tx.inputs.size(); i++) {
                tx.getInputs().get(i).clearScriptBytes();
            }
            //清除上次交易脚本里的操作码
            redeemScript = Script.removeAllInstancesOfOp(redeemScript, ScriptOpCodes.OP_CODESEPARATOR);

            Input input = tx.inputs.get(index);
            input.setScriptBytes(redeemScript);

            if ((sigHashType & 0x1f) == SigHash.NONE.value) {
            	//TODO
            	
            }
            
            ByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(tx.length == UNKNOWN_LENGTH ? 256 : tx.length + 4);
            tx.serializeToStream(bos);
            //把hash的类型加到最后
            Utils.uint32ToByteStreamLE(0x000000ff & sigHashType, bos);
            //计算交易内容的sha256 hash
            Sha256Hash hash = Sha256Hash.twiceOf(bos.toByteArray());
            bos.close();
            return hash;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}
	
	public Sha256Hash hashForSignature(int inputIndex, byte[] redeemScript, SigHash type) {
		byte sigHashType = (byte) TransactionSignature.calcSigHashValue(type);
		return hashForSignature(inputIndex, redeemScript, sigHashType);
	}
	
	/**
	 * 签名交易
	 * @param inputIndex
	 * @param key			密匙
	 * @param redeemScript	上次交易的赎回脚本
	 * @param hashType		hash类型
	 * @return TransactionSignature
	 */
	public TransactionSignature calculateSignature(int inputIndex, ECKey key, byte[] redeemScript, SigHash hashType) {
		Sha256Hash hash = hashForSignature(inputIndex, redeemScript, hashType);
		return new TransactionSignature(key.sign(hash), hashType);
	}
	
	/**
	 * 添加输入
	 * @param output
	 */
	public TransactionInput addInput(TransactionOutput output) {
		return addInput(new TransactionInput(output));
	}

	/**
	 * 添加输入
	 * @param input
	 */
    public TransactionInput addInput(TransactionInput input) {
        input.setParent(this);
        inputs.add(input);
        return input;
    }
    
    /**
     * 添加输出
     * @param output
     * @return TransactionOutput
     */
	public TransactionOutput addOutput(TransactionOutput output) {
		output.setParent(this);
		output.setIndex(outputs.size());
		outputs.add(output);
        return output;
	}
	
	/**
	 * 输出到指定地址
	 * @param value
	 * @param address
	 * @return TransactionOutput
	 */
	public TransactionOutput addOutput(Coin value, Address address) {
        return addOutput(new TransactionOutput(this, value, address));
    }
	
	/**
	 * 输出到指定地址
	 * @param value
	 * @param address
	 * @return TransactionOutput
	 */
	public TransactionOutput addOutput(Coin value, long lockTime, Address address) {
		return addOutput(new TransactionOutput(this, value, lockTime, address));
	}

	/**
	 * 输出到pubkey
	 * @param value
	 * @param pubkey
	 * @return TransactionOutput
	 */
	public TransactionOutput addOutput(Coin value, ECKey pubkey) {
        return addOutput(new TransactionOutput(this, value, pubkey));
    }

	/**
	 * 输出到脚本
	 * @param value
	 * @param script
	 * @return TransactionOutput
	 */
    public TransactionOutput addOutput(Coin value, Script script) {
        return addOutput(new TransactionOutput(this, value, 0l, script.getProgram()));
    }
    
    /**
     * 是否有代币相关的交易，比如转账时单纯的代币交易，也可能包含很多业务流程有使用到代币的情况，比如验证奖励
     * @return boolean
     */
	public boolean isPaymentTransaction() {
		return Definition.isPaymentTransaction(type);
	}

	/**
	 * 获取交易的交易费
	 * @return Coin
	 */
	public Coin getFee() {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public String toString() {
    	return "tx: " +getHash() + " inputSize:" + (inputs == null ? 0:inputs.size()) + " outputSize:" + (outputs == null ? 0:outputs.size());
    }
    
    public Input getInput(int index) {
        return inputs.get(index);
    }

    public Output getOutput(int index) {
        return outputs.get(index);
    }
    
    public List<Input> getInputs() {
		return inputs;
	}
    
    public List<Output> getOutputs() {
		return outputs;
	}

    public long getLockTime() {
		return lockTime;
	}
    public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}
    public long getTime() {
		return time;
	}
    public void setTime(long time) {
		this.time = time;
	}

	public Sha256Hash getHash() {
		if (hash == null) {
            hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(unsafeBitcoinSerialize()));
        }
		return hash;
	}
	public void setHash(Sha256Hash hash) {
		this.hash = hash;
	}
	public void setVersion(long version) {
		this.version = version;
	}
	public long getVersion() {
		return version;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
