package dev.wakey.testsupport;

import org.springframework.boot.test.context.SpringBootTest;

/** HSQLDB integration tests share one in-memory datasource and one Spring context. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:hsqldb:mem:wakey-integration-tests",
            "spring.datasource.username=sa",
            "spring.datasource.password="
        })
public abstract class HsqldbIntegrationTest extends IntegrationTestSupport {}
