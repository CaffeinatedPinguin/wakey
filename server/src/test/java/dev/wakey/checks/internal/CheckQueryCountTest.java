package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckActor;
import dev.wakey.checks.CheckAuditLog;
import dev.wakey.checks.CheckBulkAction;
import dev.wakey.checks.CheckBulkCommand;
import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckPlan;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.EgressPolicy;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.testsupport.QueryCountingConfiguration;
import dev.wakey.testsupport.QueryCountingDataSource;
import dev.wakey.web.CurrentUser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Query;

@Import(QueryCountingConfiguration.class)
class CheckQueryCountTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "queries-admin", true);

    private static final CheckActor ACTOR = new CheckActor("queries-test");

    @Test
    void theListLoadsTheTagsOfAPageWithOneQuery() {
        // given
        UUID spaceId = seedSpaceWithTaggedChecks(10);
        QueryCountingDataSource dataSource = bean(QueryCountingDataSource.class);
        CheckService service = bean(CheckService.class);

        // when
        dataSource.reset();
        List<Check> single = service.list(spaceId, CheckFilter.all(), null, 1);
        int singleQueries = dataSource.statementCount();
        dataSource.reset();
        List<Check> whole = service.list(spaceId, CheckFilter.all(), null, 10);
        int wholeQueries = dataSource.statementCount();

        // then
        assertThat(single).hasSize(1);
        assertThat(whole).hasSize(10);
        assertThat(single).allSatisfy(check -> assertThat(check.tags()).isNotEmpty());
        assertThat(whole).allSatisfy(check -> assertThat(check.tags()).isNotEmpty());
        assertThat(singleQueries).isEqualTo(2);
        assertThat(wholeQueries).isEqualTo(2);
    }

    @Test
    void aCheckPageQueryFetchesNoMoreRowsThanRequested() {
        // given
        UUID spaceId = seedSpaceWithTaggedChecks(10);
        AtomicInteger fetchedRows = new AtomicInteger();
        JdbcAggregateOperations aggregates = recordingLimit(bean(JdbcAggregateTemplate.class), fetchedRows);
        CheckService service = serviceWith(bean(CheckTagRepository.class), aggregates);

        // when
        List<Check> page = service.list(spaceId, CheckFilter.all(), null, 2);

        // then
        assertThat(page).hasSize(2);
        assertThat(fetchedRows).hasValue(2);
    }

    @Test
    void aBulkActionUsesAConstantNumberOfQueries() {
        // given
        UUID smallSpace = seedSpaceWithTaggedChecks(2);
        UUID largeSpace = seedSpaceWithTaggedChecks(10);
        CheckService service = bean(CheckService.class);
        QueryCountingDataSource dataSource = bean(QueryCountingDataSource.class);
        List<UUID> smallIds = idsOf(service, smallSpace);
        List<UUID> largeIds = idsOf(service, largeSpace);

        // when
        dataSource.reset();
        int smallAffected = service.bulk(smallSpace, deleteCommand(smallIds), ACTOR);
        int smallQueries = dataSource.statementCount();
        dataSource.reset();
        int largeAffected = service.bulk(largeSpace, deleteCommand(largeIds), ACTOR);
        int largeQueries = dataSource.statementCount();

        // then
        assertThat(smallAffected).isEqualTo(2);
        assertThat(largeAffected).isEqualTo(10);
        assertThat(smallQueries).isEqualTo(largeQueries);
    }

    @Test
    void theTagFilterReadsTagsOnlyFromTheSpace() {
        // given
        SpaceService spaces = bean(SpaceService.class);
        CheckService creator = bean(CheckService.class);
        UUID spaceA = createSpace(spaces, "tag-scope-a");
        UUID spaceB = createSpace(spaces, "tag-scope-b");
        CheckDraft spaceADraft = draft("a-check", List.of("shared"));
        CheckDraft spaceBDraft = draft("b-check", List.of("shared", "foreign"));
        Check inSpaceA = creator.create(spaceA, spaceADraft, ACTOR);
        creator.create(spaceB, spaceBDraft, ACTOR);
        List<UUID> queriedSpaces = new ArrayList<>();
        CheckTagRepository tags = recordingSpaces(bean(CheckTagRepository.class), queriedSpaces);
        CheckService service = serviceWith(tags, bean(JdbcAggregateTemplate.class));

        // when
        List<Check> shared = service.list(spaceA, tagFilter("shared"), null, 100);
        List<Check> foreign = service.list(spaceA, tagFilter("foreign"), null, 100);

        // then
        assertThat(shared).extracting(Check::id).containsExactly(inSpaceA.id());
        assertThat(foreign).isEmpty();
        assertThat(queriedSpaces).containsExactly(spaceA, spaceA);
    }

    @Test
    void theResultIndexCoversTheLatestOrdering() {
        // when
        String definition = jdbc().sql("""
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE indexname = 'ix_check_result_check_checked_id'
                        """).query(String.class).single();
        long oldIndexes = jdbc().sql("""
                        SELECT COUNT(*)
                        FROM pg_indexes
                        WHERE indexname = 'ix_check_result_check_checked'
                        """).query(Long.class).single();

        // then
        assertThat(definition).contains("(check_id, checked_at DESC, id DESC)");
        assertThat(oldIndexes).isZero();
    }

    private CheckService serviceWith(CheckTagRepository tags, JdbcAggregateOperations aggregates) {
        CheckQueries queries = new CheckQueries(tags, aggregates);
        CheckStore store =
                new CheckStore(bean(CheckRepository.class), queries, bean(CheckResultStore.class), bean(Clock.class));
        return new CheckService(
                store,
                bean(CheckAuditLog.class),
                bean(SpaceService.class),
                bean(CheckPlan.class),
                bean(EgressPolicy.class));
    }

    private UUID seedSpaceWithTaggedChecks(int count) {
        SpaceService spaces = bean(SpaceService.class);
        CheckService checks = bean(CheckService.class);
        UUID spaceId = createSpace(spaces, "queries");
        for (int index = 1; index <= count; index++) {
            String name = "check-" + index;
            CheckDraft draft = draft(name, List.of("tag-" + index, "shared"));
            checks.create(spaceId, draft, ACTOR);
        }
        return spaceId;
    }

    private UUID createSpace(SpaceService spaces, String prefix) {
        String slug = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        return spaces.put(ADMIN, slug, "Queries " + prefix, null).space().id();
    }

    private static List<UUID> idsOf(CheckService service, UUID spaceId) {
        List<Check> checks = service.list(spaceId, CheckFilter.all(), null, 100);
        return checks.stream().map(Check::id).toList();
    }

    private static CheckBulkCommand deleteCommand(List<UUID> ids) {
        return new CheckBulkCommand(CheckBulkAction.DELETE, ids, null, null);
    }

    private static CheckFilter tagFilter(String tag) {
        return new CheckFilter(null, null, null, tag, null, null);
    }

    private static JdbcAggregateOperations recordingLimit(JdbcAggregateOperations delegate, AtomicInteger fetchedRows) {
        Object proxy = Proxy.newProxyInstance(
                JdbcAggregateOperations.class.getClassLoader(),
                new Class<?>[] {JdbcAggregateOperations.class},
                (_, method, args) -> {
                    if ("findAll".equals(method.getName()) && args != null && args[0] instanceof Query query) {
                        fetchedRows.set(query.getLimit());
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getTargetException();
                    }
                });
        return JdbcAggregateOperations.class.cast(proxy);
    }

    private static CheckTagRepository recordingSpaces(CheckTagRepository delegate, List<UUID> queriedSpaces) {
        Object proxy = Proxy.newProxyInstance(
                CheckTagRepository.class.getClassLoader(),
                new Class<?>[] {CheckTagRepository.class},
                (_, method, args) -> {
                    if ("findBySpaceIdAndKeyTag".equals(method.getName()) && args != null) {
                        queriedSpaces.add((UUID) args[0]);
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getTargetException();
                    }
                });
        return CheckTagRepository.class.cast(proxy);
    }

    private static CheckDraft draft(String name, List<String> tags) {
        return new CheckDraft(name, CheckType.HTTP, "https://" + name + ".test", 60, 10, 1, null, null, null, tags);
    }
}
