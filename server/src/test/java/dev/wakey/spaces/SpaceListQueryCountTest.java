package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.testsupport.QueryCountingConfiguration;
import dev.wakey.testsupport.QueryCountingDataSource;
import dev.wakey.web.CurrentUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

@Import(QueryCountingConfiguration.class)
class SpaceListQueryCountTest extends PostgresIntegrationTest {

    private static final CurrentUser ADMIN = new CurrentUser("https://issuer.example.com", "query-count-admin", true);

    @Test
    void listingAPageRunsAtMostTwoQueries() {
        // given
        SpaceService service = bean(SpaceService.class);
        QueryCountingDataSource dataSource = bean(QueryCountingDataSource.class);
        seed(service, "page-query");
        seed(service, "page-query");
        seed(service, "page-query");

        // when
        dataSource.reset();
        List<SpaceView> page = service.listVisible(ADMIN, null, 2);
        int queries = dataSource.statementCount();

        // then
        assertThat(page).hasSize(2);
        assertThat(queries).isLessThanOrEqualTo(2);
    }

    @Test
    void readingThePlanNameRunsOneQuery() {
        // given
        SpaceService service = bean(SpaceService.class);
        QueryCountingDataSource dataSource = bean(QueryCountingDataSource.class);
        String slug = prefix("plan-query");
        UUID spaceId = service.put(ADMIN, slug, "Plan space", null).space().id();

        // when
        dataSource.reset();
        Optional<String> planName = service.planNameOf(spaceId);
        int queries = dataSource.statementCount();

        // then
        assertThat(planName).isEmpty();
        assertThat(queries).isEqualTo(1);
    }

    private static void seed(SpaceService service, String text) {
        String slug = prefix(text);
        service.put(ADMIN, slug, "Page space", null);
    }

    private static String prefix(String text) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return text + "-" + suffix;
    }
}
