package dev.wakey.messaging;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@RequiredArgsConstructor
final class JdbcMessageQueue implements MessageQueue {

    private static final long FIRST_CURSOR = 0L;

    private static final RowMapper<QueuedMessage> MESSAGE_MAPPER = (rs, unusedRowNum) -> new QueuedMessage(
            rs.getLong("message_cursor"), rs.getString("topic"), rs.getString("message_key"), rs.getString("payload"));

    private static final String ADVANCE_SEQUENCE_SQL = """
            UPDATE queue_sequence
            SET last_cursor = last_cursor + 1
            WHERE id = 1
            """;

    private static final String SELECT_SEQUENCE_SQL = """
            SELECT last_cursor
            FROM queue_sequence
            WHERE id = 1
            """;

    private static final String INSERT_MESSAGE_SQL = """
            INSERT INTO queue_message (message_cursor, topic, message_key, payload)
            VALUES (:cursor, :topic, :messageKey, :payload)
            """;

    private static final String SELECT_CONSUMER_CURSOR_SQL = """
            SELECT last_cursor
            FROM queue_consumer_cursor
            WHERE consumer = :consumer AND topic = :topic
            """;

    private static final String SELECT_MESSAGES_SQL = """
            SELECT message_cursor, topic, message_key, payload
            FROM queue_message
            WHERE topic = :topic AND message_cursor > :cursor
            ORDER BY message_cursor
            LIMIT :messageLimit
            """;

    private static final String ACK_CONSUMER_CURSOR_SQL = """
            MERGE INTO queue_consumer_cursor AS target
            USING (VALUES (
                CAST(:consumer AS VARCHAR(255)),
                CAST(:topic AS VARCHAR(255)),
                CAST(:cursor AS BIGINT))) AS incoming (consumer, topic, new_cursor)
            ON target.consumer = incoming.consumer AND target.topic = incoming.topic
            WHEN MATCHED THEN UPDATE SET last_cursor = GREATEST(target.last_cursor, incoming.new_cursor)
            WHEN NOT MATCHED THEN INSERT (consumer, topic, last_cursor)
                VALUES (incoming.consumer, incoming.topic, incoming.new_cursor)
            """;

    private final JdbcClient jdbc;

    @Override
    public void publish(String topic, String key, String payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Messages can only be published inside an active transaction");
        }
        advanceSequence();
        long cursor = currentCursor();
        jdbc.sql(INSERT_MESSAGE_SQL)
                .param("cursor", cursor)
                .param("topic", topic)
                .param("messageKey", key)
                .param("payload", payload)
                .update();
        log.debug("Published message to topic {} with key {} at cursor {}", topic, key, cursor);
    }

    @Override
    public List<QueuedMessage> poll(String consumer, String topic, int limit) {
        if (limit <= 0) {
            log.debug("Polled 0 messages for consumer {} on topic {}", consumer, topic);
            return List.of();
        }
        long cursor = consumerCursor(consumer, topic);
        List<QueuedMessage> messages = jdbc.sql(SELECT_MESSAGES_SQL)
                .param("topic", topic)
                .param("cursor", cursor)
                .param("messageLimit", limit)
                .query(MESSAGE_MAPPER)
                .list();
        log.debug("Polled {} messages for consumer {} on topic {}", messages.size(), consumer, topic);
        return messages;
    }

    @Override
    public void ack(String consumer, String topic, long cursor) {
        jdbc.sql(ACK_CONSUMER_CURSOR_SQL)
                .param("consumer", consumer)
                .param("topic", topic)
                .param("cursor", cursor)
                .update();
        log.debug("Acknowledged cursor {} for consumer {} on topic {}", cursor, consumer, topic);
    }

    private void advanceSequence() {
        int updated = jdbc.sql(ADVANCE_SEQUENCE_SQL).update();
        if (updated != 1) {
            throw new IllegalStateException("The queue sequence row with id 1 is missing");
        }
    }

    private long currentCursor() {
        return jdbc.sql(SELECT_SEQUENCE_SQL).query(Long.class).single();
    }

    private long consumerCursor(String consumer, String topic) {
        return jdbc.sql(SELECT_CONSUMER_CURSOR_SQL)
                .param("consumer", consumer)
                .param("topic", topic)
                .query(Long.class)
                .optional()
                .orElse(FIRST_CURSOR);
    }
}
