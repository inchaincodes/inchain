package org.inchain.signers;

import org.inchain.crypto.ECKey;
import org.inchain.transaction.Transaction;

public interface TransactionSigner {

    boolean isReady();

    byte[] serialize();

    void deserialize(byte[] data);

    boolean signInputs(Transaction tx, ECKey key);

}
