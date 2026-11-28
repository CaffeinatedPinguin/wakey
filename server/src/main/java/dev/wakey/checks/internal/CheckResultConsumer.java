package dev.wakey.checks.internal;

import dev.wakey.checks.CheckResultMessage;
import dev.wakey.checks.CheckResults;
import dev.wakey.messaging.MessageQueue;
import dev.wakey.messaging.QueuedMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Consumes check results: poll, store and acknowledge a batch in one transaction. */
@Slf4j
@Component
@RequiredArgsConstructor
class CheckResultConsumer {

    static final String CONSUMER = "check-results";

    private static final int BATCH_SIZE = 100;

    private final MessageQueue queue;

    private final CheckResults results;

    private final ObjectMapper objectMapper;

    @Transactional
    int consumeBatch() {
        List<QueuedMessage> messages = queue.poll(CONSUMER, CheckResultMessage.TOPIC, BATCH_SIZE);
        int stored = 0;
        for (QueuedMessage message : messages) {
            CheckResultMessage decoded = decode(message);
            if (decoded != null && results.recordFromMessage(decoded)) {
                stored++;
            }
        }
        if (!messages.isEmpty()) {
            QueuedMessage last = messages.getLast();
            queue.ack(CONSUMER, CheckResultMessage.TOPIC, last.cursor());
        }
        log.debug("Consumed {} of {} check result messages", stored, messages.size());
        return stored;
    }

    @Nullable
    private CheckResultMessage decode(QueuedMessage message) {
        try {
            return objectMapper.readValue(message.payload(), CheckResultMessage.class);
        } catch (JacksonException exception) {
            String type = classify(exception);
            log.warn("Skipping an unreadable check result message at cursor {} ({})", message.cursor(), type);
            return null;
        }
    }

    private static String classify(JacksonException exception) {
        return exception.getClass().getSimpleName();
    }
}
