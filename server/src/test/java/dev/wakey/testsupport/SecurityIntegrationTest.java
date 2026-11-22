package dev.wakey.testsupport;

import jakarta.servlet.Filter;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** A MockMvc that runs the real filter chains and the Spring Session repository filter. */
public abstract class SecurityIntegrationTest extends HsqldbIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    void createMockMvc(WebApplicationContext webApplicationContext) {
        Filter sessionRepositoryFilter = webApplicationContext.getBean("springSessionRepositoryFilter", Filter.class);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(sessionRepositoryFilter)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    protected MockMvc mockMvc() {
        return Objects.requireNonNull(mockMvc);
    }
}
