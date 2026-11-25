package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Import(SpaceListQueryCountTest.CountingDataConfiguration.class)
class SpaceListQueryCountTest extends PostgresIntegrationTest {

    private static final SpacePrincipal ADMIN = new SpacePrincipal("query-count-admin", true);

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

    private static void seed(SpaceService service, String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = prefix + "-" + suffix;
        service.put(ADMIN, slug, "Page space");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CountingDataConfiguration {

        @Bean
        QueryCountingDataSource dataSource(JdbcConnectionDetails connectionDetails) {
            @Nullable String username = connectionDetails.getUsername();
            @Nullable String password = connectionDetails.getPassword();
            DriverManagerDataSource delegate = new DriverManagerDataSource();
            delegate.setDriverClassName(connectionDetails.getDriverClassName());
            delegate.setUrl(connectionDetails.getJdbcUrl());
            delegate.setUsername(username == null ? "" : username);
            delegate.setPassword(password == null ? "" : password);
            return new QueryCountingDataSource(delegate);
        }
    }
}
