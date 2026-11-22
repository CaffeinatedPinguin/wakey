package dev.wakey.messaging;

import dev.wakey.testsupport.HsqldbIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Runs the shared {@link MessageQueueContract} against the HSQLDB in-memory database.
 *
 * <p>The datasource properties and the shared application context live in
 * {@link dev.wakey.testsupport.HsqldbIntegrationTest}; this class only mixes in the contract.
 */
class HsqldbMessageQueueTest extends HsqldbIntegrationTest implements MessageQueueContract {

    private MessageQueue messageQueue;

    private PlatformTransactionManager transactionManager;

    @Autowired
    void setQueue(MessageQueue messageQueue, PlatformTransactionManager transactionManager) {
        this.messageQueue = messageQueue;
        this.transactionManager = transactionManager;
    }

    @Override
    public MessageQueue queue() {
        return messageQueue;
    }

    @Override
    public PlatformTransactionManager transactions() {
        return transactionManager;
    }
}
