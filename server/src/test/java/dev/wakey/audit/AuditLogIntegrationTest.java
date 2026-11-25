package dev.wakey.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(AuditLogIntegrationTest.SteppingClockConfiguration.class)
class AuditLogIntegrationTest extends PostgresIntegrationTest {

    private static final String OBJECT_TYPE = "space";

    private static final String USER_SUBJECT = "fixture-user";

    private static final UUID SPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void recordedEntryIsReadBackWithAllFields() {
        // given
        String objectId = uniqueObjectId();
        AuditEntry entry = entry(objectId, "{\"name\":\"after\"}");

        // when
        record(entry);
        List<AuditRecord> history = auditLog().history(OBJECT_TYPE, objectId, null, 10);

        // then
        assertThat(history).hasSize(1);
        AuditRecord stored = history.get(0);
        assertThat(stored.id()).isNotNull();
        assertThat(stored.occurredAt()).isNotNull();
        assertThat(stored.action()).isEqualTo(entry.action());
        assertThat(stored.objectType()).isEqualTo(entry.objectType());
        assertThat(stored.objectId()).isEqualTo(entry.objectId());
        assertThat(stored.outcome()).isEqualTo(entry.outcome());
        assertThat(stored.source()).isEqualTo(entry.source());
        assertThat(stored.actor()).isEqualTo(entry.actor());
        assertThat(stored.spaceId()).isEqualTo(entry.spaceId());
        assertThat(stored.details()).isEqualTo(entry.details());
    }

    @Test
    void recordWithoutATransactionIsRejected() {
        // given
        String objectId = uniqueObjectId();
        AuditEntry entry = entry(objectId, "{}");

        // when
        Throwable rejection = catchThrowable(() -> auditLog().record(entry));
        List<AuditRecord> history = auditLog().history(OBJECT_TYPE, objectId, null, 10);

        // then
        assertThat(rejection).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(history).isEmpty();
    }

    @Test
    void rolledBackChangeLeavesNoEntry() {
        // given
        String objectId = uniqueObjectId();
        AuditEntry entry = entry(objectId, "{}");
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            auditLog().record(entry);
            status.setRollbackOnly();
        });
        List<AuditRecord> history = auditLog().history(OBJECT_TYPE, objectId, null, 10);

        // then
        assertThat(history).isEmpty();
    }

    @Test
    void historyReturnsOneObjectNewestFirst() {
        // given
        String objectId = uniqueObjectId();
        record(entry(objectId, "{\"step\":1}"));
        record(entry(objectId, "{\"step\":2}"));
        record(entry(objectId, "{\"step\":3}"));

        // when
        List<AuditRecord> history = auditLog().history(OBJECT_TYPE, objectId, null, 10);

        // then
        assertThat(history)
                .extracting(AuditRecord::details)
                .containsExactly("{\"step\":3}", "{\"step\":2}", "{\"step\":1}");
    }

    @Test
    void historyResumesAfterACursor() {
        // given
        String objectId = uniqueObjectId();
        record(entry(objectId, "{\"step\":1}"));
        record(entry(objectId, "{\"step\":2}"));
        record(entry(objectId, "{\"step\":3}"));
        record(entry(objectId, "{\"step\":4}"));
        record(entry(objectId, "{\"step\":5}"));

        // when
        List<AuditRecord> firstPage = auditLog().history(OBJECT_TYPE, objectId, null, 2);
        AuditRecord lastOfFirstPage = firstPage.get(1);
        Instant occurredAt = lastOfFirstPage.occurredAt();
        UUID id = lastOfFirstPage.id();
        AuditCursor cursor = new AuditCursor(occurredAt, id);
        List<AuditRecord> secondPage = auditLog().history(OBJECT_TYPE, objectId, cursor, 10);

        // then
        assertThat(firstPage).extracting(AuditRecord::details).containsExactly("{\"step\":5}", "{\"step\":4}");
        assertThat(secondPage)
                .extracting(AuditRecord::details)
                .containsExactly("{\"step\":3}", "{\"step\":2}", "{\"step\":1}");
    }

    @Test
    void entriesCannotBeChangedThroughThePort() {
        // when
        Set<String> methodNames =
                Arrays.stream(AuditLog.class.getMethods()).map(Method::getName).collect(Collectors.toSet());

        // then
        assertThat(methodNames).containsExactlyInAnyOrder("record", "history");
    }

    @Test
    void deniedOutcomeIsStored() {
        // given
        String objectId = uniqueObjectId();
        AuditEntry entry = new AuditEntry(
                "access.denied",
                OBJECT_TYPE,
                objectId,
                AuditOutcome.DENIED,
                AuditSource.API,
                new AuditActor(USER_SUBJECT, ActorKind.TOKEN),
                SPACE_ID,
                "{\"reason\":\"missing role\"}");

        // when
        record(entry);
        List<AuditRecord> history = auditLog().history(OBJECT_TYPE, objectId, null, 10);

        // then
        assertThat(history).hasSize(1);
        AuditRecord stored = history.get(0);
        assertThat(stored.outcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(stored.action()).isEqualTo("access.denied");
    }

    private AuditLog auditLog() {
        return bean(AuditLog.class);
    }

    private void record(AuditEntry entry) {
        TransactionTemplate template = transactionTemplate();
        template.executeWithoutResult(status -> auditLog().record(entry));
    }

    private TransactionTemplate transactionTemplate() {
        PlatformTransactionManager manager = bean(PlatformTransactionManager.class);
        return new TransactionTemplate(manager);
    }

    private static AuditEntry entry(String objectId, String details) {
        return new AuditEntry(
                "space.put",
                OBJECT_TYPE,
                objectId,
                AuditOutcome.SUCCESS,
                AuditSource.UI,
                new AuditActor(USER_SUBJECT, ActorKind.USER),
                SPACE_ID,
                details);
    }

    private static String uniqueObjectId() {
        return UUID.randomUUID().toString();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SteppingClockConfiguration {

        @Bean
        @Primary
        Clock steppingClock() {
            return new SteppingClock();
        }
    }

    private static final class SteppingClock extends Clock {

        private final AtomicReference<Instant> current = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new UnsupportedOperationException("Only UTC is supported: " + zone);
            }
            return this;
        }

        @Override
        public Instant instant() {
            return current.getAndUpdate(previous -> previous.plusSeconds(1));
        }
    }
}
