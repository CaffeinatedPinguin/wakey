package dev.wakey.leadership;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:hsqldb:mem:wakey-leader-lease-contract",
            "spring.datasource.username=sa",
            "spring.datasource.password="
        })
class HsqldbLeaderLeaseTest extends AbstractLeaderLeaseTest {

    @Autowired
    HsqldbLeaderLeaseTest(JdbcClient jdbc) {
        super(jdbc);
    }
}
