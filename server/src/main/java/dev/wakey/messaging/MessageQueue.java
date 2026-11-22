package dev.wakey.messaging;

import java.util.List;

public interface MessageQueue {

    /**
     * Publishes a message as part of the caller's transaction. Publishing requires an active
     * transaction and the message is stored only if that transaction commits. Call it as the last
     * step of the producer's transaction, because the shared cursor row stays locked until commit.
     *
     * @param topic the topic that consumers poll
     * @param key the key that keeps related messages ordered
     * @param payload the self-contained message body
     * @throws IllegalStateException when there is no active transaction
     */
    void publish(String topic, String key, String payload);

    /**
     * Returns messages of the topic after the consumer's cursor, in ascending cursor order, at
     * most {@code limit}. Messages stay available until acknowledged, so delivery is at least once.
     *
     * @param consumer the consumer whose cursor is used
     * @param topic the topic to poll
     * @param limit the maximum number of messages to return
     * @return the next messages for the consumer, in ascending cursor order
     */
    List<QueuedMessage> poll(String consumer, String topic, int limit);

    /**
     * Acknowledges a cursor for the consumer and topic. Pass the cursor of a {@link QueuedMessage}
     * returned by {@link #poll}: an older cursor changes nothing, while a cursor larger than the
     * one returned by the last poll would skip messages.
     *
     * @param consumer the consumer whose cursor is advanced
     * @param topic the topic the cursor belongs to
     * @param cursor the last processed cursor
     */
    void ack(String consumer, String topic, long cursor);
}
