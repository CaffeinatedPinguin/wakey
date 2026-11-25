package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckPage;
import dev.wakey.checks.CheckService;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PlanLimits;
import dev.wakey.spaces.SpacePrincipal;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class CheckQueryCountTest extends PostgresIntegrationTest {

    private static final SpacePrincipal ADMIN = new SpacePrincipal("queries-admin", true);

    private static final PlanLimits UNLIMITED = new PlanLimits(null, null, null, null, "default");

    @Test
    void theListLoadsTheTagsOfAPageWithOneQuery() {
        // given
        UUID spaceId = seedSpaceWithTaggedChecks();
        AtomicInteger statements = new AtomicInteger();
        JdbcClient real = bean(JdbcClient.class);
        JdbcClient counting = countingClient(real, statements);
        CheckService checks = new JdbcCheckService(new CheckSql(counting));
        statements.set(0);

        // when
        CheckPage page = checks.list(spaceId, CheckFilter.all(), null, 100);

        // then
        assertThat(page.items()).hasSize(3);
        assertThat(statements.get()).isEqualTo(2);
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

    private UUID seedSpaceWithTaggedChecks() {
        SpaceService spaces = bean(SpaceService.class);
        CheckService checks = bean(CheckService.class);
        String slug = "queries-" + UUID.randomUUID().toString().substring(0, 8);
        UUID spaceId = spaces.put(ADMIN, slug, "Queries").space().id();
        CheckDraft one = draft("one", List.of("alpha", "beta"));
        CheckDraft two = draft("two", List.of("gamma"));
        CheckDraft three = draft("three", List.of("delta", "epsilon"));
        checks.create(spaceId, one, UNLIMITED);
        checks.create(spaceId, two, UNLIMITED);
        checks.create(spaceId, three, UNLIMITED);
        return spaceId;
    }

    private static JdbcClient countingClient(JdbcClient delegate, AtomicInteger statements) {
        return (JdbcClient) Proxy.newProxyInstance(
                JdbcClient.class.getClassLoader(), new Class<?>[] {JdbcClient.class}, (proxy, method, args) -> {
                    if (method.getName().equals("sql")) {
                        statements.incrementAndGet();
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getTargetException();
                    }
                });
    }

    private static CheckDraft draft(String name, List<String> tags) {
        return new CheckDraft(name, CheckType.HTTP, "https://" + name + ".test", 60, 10, 1, null, null, null, tags);
    }
}
