package org.inchain.core;

import org.inchain.message.Message;
import org.inchain.transaction.Transaction;

public interface Broadcaster {
    TransactionBroadcast broadcastTransaction(final Transaction tx);
    boolean broadcastMessage(Message message);
}
