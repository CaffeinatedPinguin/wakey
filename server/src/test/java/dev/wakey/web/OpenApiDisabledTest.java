package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import dev.wakey.testsupport.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class OpenApiDisabledTest extends PostgresIntegrationTest {

    private WebApplicationContext webApplicationContext;

    @Autowired
    void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @Test
    void docsEndpointIsNotServedByDefault() throws Exception {
        // given
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // when
        MvcResult result = mvc.perform(get("/v3/api-docs/public")).andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(404);
    }
}
