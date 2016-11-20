package org.inchain.transaction;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.inchain.account.AccountTool;
import org.inchain.account.Address;
import org.inchain.core.Coin;
import org.inchain.core.UnsafeByteArrayOutputStream;
import org.inchain.core.VarInt;
import org.inchain.core.exception.ProtocolException;
import org.inchain.core.exception.VerificationException;
import org.inchain.crypto.ECKey;
import org.inchain.crypto.Sha256Hash;
import org.inchain.crypto.TransactionSignature;
import org.inchain.message.Message;
import org.inchain.network.NetworkParameters;
import org.inchain.script.Script;
import org.inchain.script.ScriptBuilder;
import org.inchain.script.ScriptOpCodes;
import org.inchain.utils.Utils;

/**
 * 交易
 * @author ln
 *
 */
public class Transaction extends Message {
	
	public static final long VERSION = 1;
	
	public static final int TYPE_COINBASE = 9;	//coinbase交易
	public static final int TYPE_REGISTER = 1;	//帐户注册
	public static final int TYPE_CHANGEPWD = 2;	//修改密码
	public static final int TYPE_PAY = 6;		//普通支付交易

	//锁定时间标识，小于该数表示为块数，大于则为秒级时间戳
	public static final int LOCKTIME_THRESHOLD = 500000000;
    public static final BigInteger LOCKTIME_THRESHOLD_BIG = BigInteger.valueOf(LOCKTIME_THRESHOLD);
    //允许的交易最大值
    public static final int MAX_STANDARD_TX_SIZE = 100000;
    
	//交易输入
    protected List<Input> inputs;
	//交易输出
    protected List<Output> outputs;
	
	//tx hash
	protected Sha256Hash hash;
	protected long lockTime;
	//交易版本
	protected long version;
	//交易类型
	protected int type;
	
	public enum SigHash {
        ALL(1),
        NONE(2),
        SINGLE(3),
        ANYONECANPAY(0x80), // Caution: Using this type in isolation is non-standard. Treated similar to ANYONECANPAY_ALL.
        ANYONECANPAY_ALL(0x81),
        ANYONECANPAY_NONE(0x82),
        ANYONECANPAY_SINGLE(0x83),
        UNSET(0); // Caution: Using this type in isolation is non-standard. Treated similar to ALL.

        public final int value;

        /**
         * @param value
         */
        private SigHash(final int value) {
            this.value = value;
        }

        /**
         * @return the value as a byte
         */
        public byte byteValue() {
            return (byte) this.value;
        }
    }
	
	public Transaction(NetworkParameters network) {
		super(network);
		inputs = new ArrayList<Input>();
        outputs = new ArrayList<Output>();
	}

	public Transaction(NetworkParameters params, byte[] payloadBytes) throws ProtocolException {
        super(params, payloadBytes, 0);
    }

	public Transaction(NetworkParameters params, byte[] payloadBytes, int offset) throws ProtocolException {
        super(params, payloadBytes, offset);
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
        Utils.uint32ToByteStreamLE(lockTime, stream);
    }
	
	/**
	 * 反序列化交易
	 */
	@Override
	protected void parse() throws ProtocolException {
		cursor = offset;
		
		type = readBytes(1)[0];
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
        lockTime = readUint32();
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
        
        if(getType() == Transaction.TYPE_COINBASE) {
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

        //通过公匙生成赎回脚本
        ECKey key = ECKey.fromPublicOnly(input.getScriptSig().getPubKey());

        //TODO 根据交易类型，生成对应的赎回脚本
        
		Script script = ScriptBuilder.createOutputScript(
				AccountTool.newAddressFromKey(network, (int)version, key));
		
        pre.setScript(script);
        
        return input;
	}

	/**
	 * 验证交易的合法性
	 */
	public void verfify() throws VerificationException {
		
		
	}

	/**
	 * 验证交易脚本
	 */
	public void verfifyScript() {

		Utils.checkState(inputs.size() > 0);
		Utils.checkState(outputs.size() > 0);
		
		Input input = inputs.get(0);
		Output output = outputs.get(0);
		
		//如果是coinbase交易，就不检查脚本
		if(type != TYPE_COINBASE)
			input.getScriptSig().run(this, output.getScript());
	}

	public Sha256Hash hashForSignature(int index, byte[] redeemScript, byte sigHashType) {
		try {
//            Transaction tx = this.network.getDefaultSerializer().makeTransaction(this.baseSerialize());
            Transaction tx = this;

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
            	
            } else if ((sigHashType & 0x1f) == SigHash.SINGLE.value) {
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
     * @return
     */
	public TransactionOutput addOutput(TransactionOutput output) {
		output.setParent(this);
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
        return addOutput(new TransactionOutput(this, value, script.getProgram()));
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
