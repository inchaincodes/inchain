package org.inchain.core;

import org.inchain.crypto.Sha256Hash;
import org.inchain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 *  资产信息
 * Created by v.chou on 2017/7/12.
 */
public class Assets {

    private static Logger log = LoggerFactory.getLogger(Assets.class);

    //资产代码
    private byte[] code;
    //代码长度
    public static int CODE_LENGTH = 32;
    //资产余额
    private Long balance;

    public Assets(byte[] code, Long balance) {
        this.code = code;
        this.balance = balance;
    }

    public Assets(byte[] content) {
        this.parse(content);
    }

    /**
     * 序列化资产信息
     * @return
     */
    public final byte[] serialize() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(code);
            Utils.int64ToByteStreamLE(this.balance,bos);
            return bos.toByteArray();
        }catch (Exception e) {
            log.error(e.getMessage(), e);
        }finally {
            try {
                bos.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return new byte[0];
    }

    /**
     * 反序列化资产信息
     * @param content
     */
    public void parse(byte[] content) {
        if(content == null || content.length == 0) {
            return;
        }
        int cursor = 0;
        byte[] code = new byte[CODE_LENGTH];
        System.arraycopy(content, cursor, code, 0, CODE_LENGTH);
        this.code = code;

        cursor += CODE_LENGTH;
        this.balance = Utils.readInt64(content,cursor);
    }

    public byte[] getCode() {
        return code;
    }

    public void setCode(byte[] code) {
        this.code = code;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public static void main(String[] args) {
        String code1 = "abcd";
        String code2 = "gjekl";
        String code3 = "llsdsjsdj";
        String code4 = "gfdgdfdgf";
        byte[] bb1 =Sha256Hash.hash(code1.getBytes(Utils.UTF_8));
        byte[] bb2 =Sha256Hash.hash(code2.getBytes(Utils.UTF_8));
        byte[] bb3 =Sha256Hash.hash(code3.getBytes(Utils.UTF_8));
        byte[] bb4 =Sha256Hash.hash(code4.getBytes(Utils.UTF_8));

        Assets assets1 = new Assets(bb1, 10000L);
        Assets assets2 = new Assets(bb2, 31L);
        Assets assets3 = new Assets(bb3, 1059L);
        Assets assets4 = new Assets(bb4, 482L);

        byte[] b1 = assets1.serialize();
        byte[] b2 = assets2.serialize();
        byte[] b3 = assets3.serialize();
        byte[] b4 = assets4.serialize();

        byte[] myAssets = new byte[ 4 * Assets.CODE_LENGTH  + 4 * 8];
        System.arraycopy(b1,0, myAssets, 0, b1.length);
        System.arraycopy(b2,0, myAssets, b1.length, b2.length);
        System.arraycopy(b3,0, myAssets, 2 * b1.length, b3.length);
        System.arraycopy(b4,0, myAssets, 3 * b1.length, b4.length);

        for(int j = 0; j < myAssets.length; j += Assets.CODE_LENGTH + 8) {
            byte[] current = new byte[Assets.CODE_LENGTH + 8];
            System.arraycopy(myAssets, j, current, 0, Assets.CODE_LENGTH + 8);
            Assets assets = new Assets(current);
            if(Arrays.equals(assets.getCode(), bb3)) {
                assets.setBalance(5000L);

                //如果存在，则在以前的资产上添加，然后重新保存到列表中
                byte [] before = new byte[j];
                System.arraycopy(myAssets, 0, before, 0, j);

                byte [] end = new byte[myAssets.length - j - Assets.CODE_LENGTH - 8 ];
                System.arraycopy(myAssets, j + Assets.CODE_LENGTH + 8, end, 0, end.length);

                byte [] newAsset = new byte[myAssets.length];
                System.arraycopy(before, 0, newAsset, 0, before.length);
                System.arraycopy(assets.serialize(), 0, newAsset, before.length, Assets.CODE_LENGTH + 8);
                System.arraycopy(end, 0, newAsset, before.length + Assets.CODE_LENGTH + 8, end.length);

            }
        }
    }

}
