package dev.wakey.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
        List<AuditRecord> history = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));

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
        List<AuditRecord> history = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));

        // then
        assertThat(rejection).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(history).isEmpty();
    }

    @Test
    void recordingWithoutATransactionFails() {
        // given
        String objectId = uniqueObjectId();
        AuditEntry entry = entry(objectId, "{}");

        // when
        Throwable rejection = catchThrowable(() -> auditLog().record(entry));

        // then
        assertThat(rejection).isInstanceOf(IllegalTransactionStateException.class);
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
        List<AuditRecord> history = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));

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
        List<AuditRecord> history = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));

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
        List<AuditRecord> firstPage = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 2));
        AuditRecord lastOfFirstPage = firstPage.get(1);
        Instant occurredAt = lastOfFirstPage.occurredAt();
        UUID id = lastOfFirstPage.id();
        AuditCursor cursor = new AuditCursor(occurredAt, id);
        List<AuditRecord> secondPage = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(cursor, 10));

        // then
        assertThat(firstPage).extracting(AuditRecord::details).containsExactly("{\"step\":5}", "{\"step\":4}");
        assertThat(secondPage)
                .extracting(AuditRecord::details)
                .containsExactly("{\"step\":3}", "{\"step\":2}", "{\"step\":1}");
    }

    @Test
    void entriesCannotBeChangedThroughThePort() {
        // when
        Set<String> methodNames = Arrays.stream(AuditLog.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

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
        List<AuditRecord> history = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));

        // then
        assertThat(history).hasSize(1);
        AuditRecord stored = history.get(0);
        assertThat(stored.outcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(stored.action()).isEqualTo("access.denied");
    }

    @Test
    void auditHistoryIsScopedBySpace() {
        // given
        UUID ownSpace = UUID.randomUUID();
        UUID foreignSpace = UUID.randomUUID();
        String objectId = uniqueObjectId();
        record(entryFor(ownSpace, objectId, "{\"space\":\"own\"}"));
        record(entryFor(foreignSpace, objectId, "{\"space\":\"foreign\"}"));
        record(entryFor(null, objectId, "{\"space\":\"none\"}"));

        // when
        List<AuditRecord> own = auditLog().history(ownSpace, OBJECT_TYPE, objectId, page(null, 10));
        List<AuditRecord> foreign = auditLog().history(foreignSpace, OBJECT_TYPE, objectId, page(null, 10));
        List<AuditRecord> withoutSpace = auditLog().history(null, OBJECT_TYPE, objectId, page(null, 10));

        // then
        assertThat(own).extracting(AuditRecord::details).containsExactly("{\"space\":\"own\"}");
        assertThat(foreign).extracting(AuditRecord::details).containsExactly("{\"space\":\"foreign\"}");
        assertThat(withoutSpace).extracting(AuditRecord::details).containsExactly("{\"space\":\"none\"}");
    }

    @Test
    void historyIsScopedToTheGivenSpace() {
        // given
        UUID ownSpace = UUID.randomUUID();
        UUID foreignSpace = UUID.randomUUID();
        String objectId = uniqueObjectId();
        record(entryFor(ownSpace, objectId, "{\"space\":\"own\"}"));
        record(entryFor(foreignSpace, objectId, "{\"space\":\"foreign\"}"));

        // when
        List<AuditRecord> own = auditLog().history(ownSpace, OBJECT_TYPE, objectId, page(null, 10));

        // then
        assertThat(own).extracting(AuditRecord::details).containsExactly("{\"space\":\"own\"}");
    }

    @Test
    void detailsLongerThanTheLimitAreRejected() {
        // given
        String details = "x".repeat(AuditEntry.MAX_DETAILS_LENGTH + 1);

        // when
        Throwable rejection = catchThrowable(() -> entry(uniqueObjectId(), details));

        // then
        assertThat(rejection).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAuditEventCannotBeUpdatedOrDeleted() {
        // given
        String objectId = uniqueObjectId();
        record(entry(objectId, "{}"));
        List<AuditRecord> recorded = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));
        UUID id = recorded.getFirst().id();

        // when
        Throwable update = catchThrowable(() -> jdbc().sql("UPDATE audit_event SET action = :action WHERE id = :id")
                .param("action", "changed")
                .param("id", id)
                .update());
        Throwable delete = catchThrowable(() -> jdbc().sql("DELETE FROM audit_event WHERE id = :id")
                .param("id", id)
                .update());

        // then
        assertThat(update).isInstanceOf(DataAccessException.class);
        assertThat(delete).isInstanceOf(DataAccessException.class);
        List<AuditRecord> history = auditLog().history(SPACE_ID, OBJECT_TYPE, objectId, page(null, 10));
        assertThat(history).hasSize(1);
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
        return entryFor(SPACE_ID, objectId, details);
    }

    private static AuditEntry entryFor(@Nullable UUID spaceId, String objectId, String details) {
        return new AuditEntry(
                "space.put",
                OBJECT_TYPE,
                objectId,
                AuditOutcome.SUCCESS,
                AuditSource.UI,
                new AuditActor(USER_SUBJECT, ActorKind.USER),
                spaceId,
                details);
    }

    private static AuditPage page(@Nullable AuditCursor after, int limit) {
        return new AuditPage(after, limit);
    }

    private static String uniqueObjectId() {
        return UUID.randomUUID().toString();
    }
}
