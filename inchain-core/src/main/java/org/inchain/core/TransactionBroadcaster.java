package org.inchain.core;

import org.inchain.transaction.Transaction;

public interface TransactionBroadcaster {
    TransactionBroadcast broadcastTransaction(final Transaction tx);
}
