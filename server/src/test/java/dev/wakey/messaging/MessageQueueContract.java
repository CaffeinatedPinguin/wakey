package dev.wakey.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Contract that every {@link MessageQueue} implementation must satisfy, regardless of the database.
 *
 * <p>This is a JUnit 5 test interface: its {@link Test} and {@link BeforeEach} default methods run
 * for every class that implements it. Concrete suites only choose a database base class, for example
 * {@code PostgresMessageQueueTest extends PostgresIntegrationTest implements MessageQueueContract}.
 */
public interface MessageQueueContract {

    MessageQueue queue();

    PlatformTransactionManager transactions();

    JdbcClient jdbc();

    @BeforeEach
    default void deleteQueueState() {
        jdbc().sql("DELETE FROM queue_message").update();
        jdbc().sql("DELETE FROM queue_consumer_cursor").update();
        jdbc().sql("UPDATE queue_sequence SET last_cursor = 0 WHERE id = 1").update();
    }

    @Test
    default void pollReturnsMessagesInCursorOrderUpToTheLimit() {
        // given
        publish("order", "one", "payload-1");
        publish("order", "two", "payload-2");
        publish("order", "three", "payload-3");

        // when
        List<QueuedMessage> firstBatch = queue().poll("consumer", "order", 2);
        QueuedMessage lastOfFirstBatch = firstBatch.get(1);
        queue().ack("consumer", "order", lastOfFirstBatch.cursor());
        List<QueuedMessage> secondBatch = queue().poll("consumer", "order", 2);

        // then
        assertThat(firstBatch).extracting(QueuedMessage::key).containsExactly("one", "two");
        assertThat(firstBatch).extracting(QueuedMessage::cursor).isSorted();
        assertThat(secondBatch).extracting(QueuedMessage::key).containsExactly("three");
    }

    @Test
    default void unacknowledgedMessagesAreDeliveredAgain() {
        // given
        publish("redelivery", "one", "payload-1");

        // when
        List<QueuedMessage> firstPoll = queue().poll("consumer", "redelivery", 10);
        List<QueuedMessage> secondPoll = queue().poll("consumer", "redelivery", 10);

        // then
        assertThat(firstPoll).extracting(QueuedMessage::key).containsExactly("one");
        assertThat(secondPoll).extracting(QueuedMessage::key).containsExactly("one");
    }

    @Test
    default void acknowledgingAnOlderCursorDoesNotMoveItBack() {
        // given
        publish("ack-order", "one", "payload-1");
        publish("ack-order", "two", "payload-2");
        List<QueuedMessage> messages = queue().poll("consumer", "ack-order", 10);
        QueuedMessage first = messages.get(0);
        QueuedMessage second = messages.get(1);
        queue().ack("consumer", "ack-order", second.cursor());
        queue().ack("consumer", "ack-order", first.cursor());

        // when
        List<QueuedMessage> remaining = queue().poll("consumer", "ack-order", 10);

        // then
        assertThat(remaining).isEmpty();
    }

    @Test
    default void consumersAndTopicsAreIndependent() {
        // given
        publish("independent", "one", "payload-1");
        publish("other-topic", "two", "payload-2");
        List<QueuedMessage> firstConsumerMessages = queue().poll("consumer-a", "independent", 10);
        QueuedMessage message = firstConsumerMessages.get(0);
        queue().ack("consumer-a", "independent", message.cursor());

        // when
        List<QueuedMessage> secondConsumerMessages = queue().poll("consumer-b", "independent", 10);
        List<QueuedMessage> firstConsumerOtherTopic = queue().poll("consumer-a", "other-topic", 10);

        // then
        assertThat(secondConsumerMessages).extracting(QueuedMessage::key).containsExactly("one");
        assertThat(firstConsumerOtherTopic).extracting(QueuedMessage::key).containsExactly("two");
    }

    @Test
    default void rolledBackTransactionDoesNotPublish() {
        // given
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            queue().publish("rollback", "one", "payload-1");
            status.setRollbackOnly();
        });

        // then
        List<QueuedMessage> messages = queue().poll("consumer", "rollback", 10);
        assertThat(messages).isEmpty();
    }

    @Test
    default void publishingOutsideTransactionFails() {
        // given
        String topic = "outside-transaction";

        // when
        assertThatThrownBy(() -> queue().publish(topic, "one", "payload-1")).isInstanceOf(IllegalStateException.class);
        List<QueuedMessage> messages = queue().poll("consumer", topic, 10);

        // then
        assertThat(messages).isEmpty();
    }

    @Test
    default void olderAndRepeatedAcksWithPublishCommitTogether() {
        // given
        publish("ack-transaction", "one", "payload-1");
        publish("ack-transaction", "two", "payload-2");
        List<QueuedMessage> messages = queue().poll("consumer", "ack-transaction", 10);
        QueuedMessage first = messages.get(0);
        QueuedMessage second = messages.get(1);
        long firstCursor = first.cursor();
        long secondCursor = second.cursor();
        queue().ack("consumer", "ack-transaction", secondCursor);
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            queue().ack("consumer", "ack-transaction", firstCursor);
            queue().ack("consumer", "ack-transaction", secondCursor);
            queue().publish("ack-transaction", "three", "payload-3");
        });

        // then
        List<QueuedMessage> remaining = queue().poll("consumer", "ack-transaction", 10);
        assertThat(remaining).extracting(QueuedMessage::key).containsExactly("three");
    }

    @Test
    default void firstAckInsideTransactionCreatesTheCursorAndLaterAcksNeverMoveItBack() {
        // given
        publish("first-ack", "one", "payload-1");
        publish("first-ack", "two", "payload-2");
        List<QueuedMessage> messages = queue().poll("new-consumer", "first-ack", 10);
        long firstCursor = messages.get(0).cursor();
        long secondCursor = messages.get(1).cursor();
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            queue().ack("new-consumer", "first-ack", secondCursor);
            queue().ack("new-consumer", "first-ack", firstCursor);
            queue().ack("new-consumer", "first-ack", secondCursor);
        });

        // then
        assertThat(queue().poll("new-consumer", "first-ack", 10)).isEmpty();
    }

    @Test
    default void concurrentPublishersWaitForEachOther() throws Exception {
        // given
        CountDownLatch firstPublished = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> publishAndWait("serialized", "one", firstPublished, releaseFirst));
            assertThat(firstPublished.await(30, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                publish("serialized", "two", "payload-2");
            });
            assertThat(secondStarted.await(30, TimeUnit.SECONDS)).isTrue();

            // when
            Thread.sleep(300);
            boolean secondWaited = !second.isDone();
            releaseFirst.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);

            // then
            assertThat(secondWaited).isTrue();
            List<QueuedMessage> messages = queue().poll("consumer", "serialized", 10);
            assertThat(messages).extracting(QueuedMessage::key).containsExactly("one", "two");
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private void publish(String topic, String key, String payload) {
        TransactionTemplate template = transactionTemplate();
        template.executeWithoutResult(status -> queue().publish(topic, key, payload));
    }

    private void publishAndWait(String topic, String key, CountDownLatch published, CountDownLatch release) {
        TransactionTemplate template = transactionTemplate();
        template.executeWithoutResult(status -> {
            queue().publish(topic, key, "payload-" + key);
            published.countDown();
            await(release);
        });
    }

    private TransactionTemplate transactionTemplate() {
        PlatformTransactionManager transactionManager = transactions();
        return new TransactionTemplate(transactionManager);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the parallel publisher");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the parallel publisher", exception);
        }
    }
}
