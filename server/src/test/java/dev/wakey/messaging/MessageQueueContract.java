package dev.wakey.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Contract that every {@link MessageQueue} implementation must satisfy, regardless of the database.
 *
 * <p>This is a JUnit 5 test interface: its {@link Test @Test} default methods run for every class
 * that implements it. Concrete suites only choose a database base class.
 */
public interface MessageQueueContract {

    <T> T bean(Class<T> type);

    default MessageQueue queue() {
        return bean(MessageQueue.class);
    }

    default PlatformTransactionManager transactions() {
        return bean(PlatformTransactionManager.class);
    }

    @Test
    default void pollReturnsMessagesInCursorOrderUpToTheLimit() {
        // given
        String topic = unique("order");
        String consumer = unique("consumer");
        publish(topic, "one", "payload-1");
        publish(topic, "two", "payload-2");
        publish(topic, "three", "payload-3");

        // when
        List<QueuedMessage> firstBatch = queue().poll(consumer, topic, 2);
        QueuedMessage lastOfFirstBatch = firstBatch.get(1);
        queue().ack(consumer, topic, lastOfFirstBatch.cursor());
        List<QueuedMessage> secondBatch = queue().poll(consumer, topic, 2);

        // then
        assertThat(firstBatch).extracting(QueuedMessage::key).containsExactly("one", "two");
        assertThat(firstBatch).extracting(QueuedMessage::cursor).isSorted();
        assertThat(secondBatch).extracting(QueuedMessage::key).containsExactly("three");
    }

    @Test
    default void unacknowledgedMessagesAreDeliveredAgain() {
        // given
        String topic = unique("redelivery");
        String consumer = unique("consumer");
        publish(topic, "one", "payload-1");

        // when
        List<QueuedMessage> firstPoll = queue().poll(consumer, topic, 10);
        List<QueuedMessage> secondPoll = queue().poll(consumer, topic, 10);

        // then
        assertThat(firstPoll).extracting(QueuedMessage::key).containsExactly("one");
        assertThat(secondPoll).extracting(QueuedMessage::key).containsExactly("one");
    }

    @Test
    default void acknowledgingAnOlderCursorDoesNotMoveItBack() {
        // given
        String topic = unique("ack-order");
        String consumer = unique("consumer");
        publish(topic, "one", "payload-1");
        publish(topic, "two", "payload-2");
        List<QueuedMessage> messages = queue().poll(consumer, topic, 10);
        QueuedMessage first = messages.get(0);
        QueuedMessage second = messages.get(1);
        queue().ack(consumer, topic, second.cursor());
        queue().ack(consumer, topic, first.cursor());

        // when
        List<QueuedMessage> remaining = queue().poll(consumer, topic, 10);

        // then
        assertThat(remaining).isEmpty();
    }

    @Test
    default void consumersAndTopicsAreIndependent() {
        // given
        String topic = unique("independent");
        String otherTopic = unique("other-topic");
        String consumerA = unique("consumer-a");
        String consumerB = unique("consumer-b");
        publish(topic, "one", "payload-1");
        publish(otherTopic, "two", "payload-2");
        List<QueuedMessage> firstConsumerMessages = queue().poll(consumerA, topic, 10);
        QueuedMessage message = firstConsumerMessages.get(0);
        queue().ack(consumerA, topic, message.cursor());

        // when
        List<QueuedMessage> secondConsumerMessages = queue().poll(consumerB, topic, 10);
        List<QueuedMessage> firstConsumerOtherTopic = queue().poll(consumerA, otherTopic, 10);

        // then
        assertThat(secondConsumerMessages).extracting(QueuedMessage::key).containsExactly("one");
        assertThat(firstConsumerOtherTopic).extracting(QueuedMessage::key).containsExactly("two");
    }

    @Test
    default void rolledBackTransactionDoesNotPublish() {
        // given
        String topic = unique("rollback");
        String consumer = unique("consumer");
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            queue().publish(topic, "one", "payload-1");
            status.setRollbackOnly();
        });

        // then
        List<QueuedMessage> messages = queue().poll(consumer, topic, 10);
        assertThat(messages).isEmpty();
    }

    @Test
    default void publishingOutsideTransactionFails() {
        // given
        String topic = unique("outside-transaction");
        String consumer = unique("consumer");

        // when
        assertThatThrownBy(() -> queue().publish(topic, "one", "payload-1")).isInstanceOf(IllegalStateException.class);
        List<QueuedMessage> messages = queue().poll(consumer, topic, 10);

        // then
        assertThat(messages).isEmpty();
    }

    @Test
    default void olderAndRepeatedAcksWithPublishCommitTogether() {
        // given
        String topic = unique("ack-transaction");
        String consumer = unique("consumer");
        publish(topic, "one", "payload-1");
        publish(topic, "two", "payload-2");
        List<QueuedMessage> messages = queue().poll(consumer, topic, 10);
        long firstCursor = messages.get(0).cursor();
        long secondCursor = messages.get(1).cursor();
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            queue().ack(consumer, topic, secondCursor);
            queue().ack(consumer, topic, firstCursor);
            queue().ack(consumer, topic, secondCursor);
            queue().publish(topic, "three", "payload-3");
        });

        // then
        List<QueuedMessage> remaining = queue().poll(consumer, topic, 10);
        assertThat(remaining).extracting(QueuedMessage::key).containsExactly("three");
    }

    @Test
    default void concurrentPublishersWaitForEachOther() throws Exception {
        // given
        String topic = unique("serialized");
        String consumer = unique("consumer");
        CountDownLatch firstPublished = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> publishAndWait(topic, "one", firstPublished, releaseFirst));
            assertThat(firstPublished.await(30, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                publish(topic, "two", "payload-2");
            });
            assertThat(secondStarted.await(30, TimeUnit.SECONDS)).isTrue();

            // when
            boolean secondWaited = didNotFinishWithin(second, Duration.ofSeconds(1));
            releaseFirst.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);

            // then
            assertThat(secondWaited).isTrue();
            List<QueuedMessage> messages = queue().poll(consumer, topic, 10);
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

    private static boolean didNotFinishWithin(Future<?> future, Duration timeout)
            throws InterruptedException, ExecutionException {
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return false;
        } catch (TimeoutException unusedTimeout) {
            return true;
        }
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
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
