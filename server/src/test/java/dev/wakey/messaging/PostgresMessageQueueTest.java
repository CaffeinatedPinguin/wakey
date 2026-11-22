package dev.wakey.messaging;

import dev.wakey.testsupport.PostgresIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Runs the shared {@link MessageQueueContract} against PostgreSQL.
 *
 * <p>The singleton {@code PostgreSQLContainer}, its service connection and the shared application
 * context live in {@link dev.wakey.testsupport.PostgresIntegrationTest}; this class only mixes in
 * the contract.
 */
class PostgresMessageQueueTest extends PostgresIntegrationTest implements MessageQueueContract {

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
