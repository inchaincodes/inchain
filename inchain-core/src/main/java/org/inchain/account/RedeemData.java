package org.inchain.account;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.inchain.crypto.ECKey;
import org.inchain.script.Script;
import org.inchain.utils.Utils;

/**
 * 交易赎回信息，包含赎回脚步和接收的公匙，以及使用该笔交易的私匙
 * @author ln
 *
 */
public class RedeemData {
	//赎回脚本
    public final Script redeemScript;
    
    public final List<ECKey> keys;

    private RedeemData(List<ECKey> keys, Script redeemScript) {
        this.redeemScript = redeemScript;
        List<ECKey> sortedKeys = new ArrayList<ECKey>(keys);
        this.keys = sortedKeys;
    }

    public static RedeemData of(List<ECKey> keys, Script redeemScript) {
        return new RedeemData(keys, redeemScript);
    }

    public static RedeemData of(ECKey key, Script program) {
        Utils.checkNotNull(program.isSentToAddress() || program.isSentToRawPubKey());
        return key != null ? new RedeemData(Collections.singletonList(key), program) : null;
    }

    /**
     * Returns the first key that has private bytes
     */
    public ECKey getFullKey() {
        for (ECKey key : keys)
            if (key.hasPrivKey())
                return key;
        return null;
    }
}
