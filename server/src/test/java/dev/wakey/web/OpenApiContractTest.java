package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import dev.wakey.testsupport.ContractFile;
import dev.wakey.testsupport.SecurityIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiContractTest extends SecurityIntegrationTest {

    private static final String DOCS_PATH = "/v3/api-docs/public";

    private static final List<String> HTTP_METHODS =
            List.of("get", "put", "post", "delete", "patch", "head", "options", "trace");

    // Every entry needs a justification in ADR 0006 before it is added.
    private static final Set<String> PUBLIC_OPERATIONS = Set.of();

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private WebApplicationContext webApplicationContext;

    private MockMvc docsMockMvc;

    @Autowired
    void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @BeforeEach
    void createDocsMockMvc() {
        docsMockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void generatedContractMatchesTheCommittedFile() throws Exception {
        // when
        String generated = generatedContract();
        Path contract = ContractFile.path();

        // then
        if (Boolean.getBoolean("wakey.openapi.update")) {
            Path directory = Objects.requireNonNull(contract.getParent());
            Files.createDirectories(directory);
            Files.writeString(contract, generated);
            return;
        }
        String committed = Files.readString(contract);
        assertThat(generated).isEqualTo(committed);
    }

    @Test
    void responsesMarkTheirRequiredFieldsInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode schemas = document.path("components").path("schemas");
        JsonNode space = schemas.path("SpaceResponse");
        JsonNode cursorPage = schemas.path("CursorPageSpaceResponse");

        // when
        JsonNode roleSchema = space.path("properties").path("role").path("type");
        JsonNode nextCursorSchema =
                cursorPage.path("properties").path("nextCursor").path("type");
        List<String> spaceRequired = valuesOf(space.path("required"));
        List<String> cursorPageRequired = valuesOf(cursorPage.path("required"));
        List<String> roleType = valuesOf(roleSchema);
        List<String> nextCursorType = valuesOf(nextCursorSchema);

        // then
        assertThat(spaceRequired).containsExactlyInAnyOrder("slug", "name", "version", "role", "admin");
        assertThat(cursorPageRequired).containsExactlyInAnyOrder("items", "nextCursor");
        assertThat(roleType).contains("string", "null");
        assertThat(nextCursorType).contains("string", "null");
    }

    @Test
    void checkOperationsDocumentExactlyTheirOwnResponses() throws Exception {
        // when
        JsonNode paths = MAPPER.readTree(generatedContract()).path("paths");

        // then
        assertResponses(paths, "/api/v1/spaces/{slug}/checks", "get", "200", "400", "401", "404");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks/counts", "get", "200", "401", "404");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks/{id}", "get", "200", "401", "404");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks", "post", "201", "400", "401", "403", "404");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks/{id}", "put", "200", "400", "401", "403", "404", "409");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks/{id}/pause", "post", "200", "400", "401", "403", "404");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks/{id}/resume", "post", "200", "401", "403", "404");
        assertResponses(paths, "/api/v1/spaces/{slug}/checks/{id}", "delete", "204", "401", "403", "404");
    }

    private static void assertResponses(JsonNode paths, String path, String method, String... expected) {
        JsonNode operation = paths.path(path).path(method);
        JsonNode responses = operation.path("responses");
        List<String> codes = new ArrayList<>();
        responses.propertyNames().forEach(codes::add);
        assertThat(operation.isMissingNode()).isFalse();
        assertThat(codes).containsExactlyInAnyOrder(expected);
        assertMediaTypes(responses);
    }

    private static void assertMediaTypes(JsonNode responses) {
        for (String code : List.of("200", "201")) {
            assertMediaType(responses, code, "application/json");
        }
        for (String code : List.of("400", "401", "403", "404", "409")) {
            assertMediaType(responses, code, "application/problem+json");
        }
    }

    private static void assertMediaType(JsonNode responses, String code, String mediaType) {
        JsonNode response = responses.path(code);
        if (!response.isMissingNode()) {
            JsonNode content = response.path("content");
            assertThat(content.has(mediaType)).isTrue();
        }
    }

    private static List<String> valuesOf(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                values.add(item.asString());
            }
        } else if (!node.isMissingNode()) {
            values.add(node.asString());
        }
        return values;
    }

    @Test
    void tokenOperationsRequireTheSessionSchemeInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode schemes = document.path("components").path("securitySchemes");
        JsonNode paths = document.path("paths");
        String tokensPath = "/api/v1/spaces/{slug}/tokens";
        JsonNode collection = paths.path(tokensPath);
        JsonNode token = paths.path(tokensPath + "/{id}");
        JsonNode me = paths.path("/api/v1/me");

        // when
        List<String> createSecurity = securityNames(document, collection.path("post"));
        List<String> listSecurity = securityNames(document, collection.path("get"));
        List<String> revokeSecurity = securityNames(document, token.path("delete"));
        List<String> meSecurity = securityNames(document, me.path("get"));

        // then
        String sessionDescription =
                schemes.path("sessionAuth").path("description").asString();
        assertThat(schemes.has("bearerAuth")).isTrue();
        assertThat(schemes.has("sessionAuth")).isTrue();
        assertThat(sessionDescription).contains("CSRF");
        assertThat(createSecurity).containsExactly("sessionAuth");
        assertThat(listSecurity).containsExactly("sessionAuth");
        assertThat(revokeSecurity).containsExactly("sessionAuth");
        assertThat(meSecurity).containsExactlyInAnyOrder("bearerAuth", "sessionAuth");
    }

    private static List<String> securityNames(JsonNode document, JsonNode operation) {
        JsonNode security = operation.path("security");
        JsonNode effective = security.isMissingNode() ? document.path("security") : security;
        List<String> names = new ArrayList<>();
        for (JsonNode requirement : effective) {
            requirement.propertyNames().forEach(names::add);
        }
        return names;
    }

    @Test
    void everyOperationHasAUniqueOperationId() throws Exception {
        // given
        String generated = generatedContract();
        JsonNode document = MAPPER.readTree(generated);
        JsonNode paths = document.path("paths");
        Set<String> operationIds = new LinkedHashSet<>();
        int operationCount = 0;

        // when
        for (String path : paths.propertyNames()) {
            JsonNode pathItem = paths.path(path);
            for (String method : HTTP_METHODS) {
                JsonNode operation = pathItem.path(method);
                if (!operation.isMissingNode()) {
                    String operationId = operation.path("operationId").asString();
                    operationCount++;
                    assertThat(operationId).isNotBlank();
                    operationIds.add(operationId);
                }
            }
        }

        // then
        assertThat(paths.isObject()).isTrue();
        assertThat(operationCount).isPositive();
        assertThat(operationIds).hasSize(operationCount);
    }

    @Test
    void everyEndpointInTheContractRejectsMissingAuthentication() throws Exception {
        // given
        String contract = Files.readString(ContractFile.path());
        JsonNode document = MAPPER.readTree(contract);
        JsonNode paths = document.path("paths");
        int checked = 0;

        // when
        for (String path : paths.propertyNames()) {
            JsonNode pathItem = paths.path(path);
            for (String method : HTTP_METHODS) {
                JsonNode operation = pathItem.path(method);
                if (!operation.isMissingNode()) {
                    assertRejected(path, method);
                    checked++;
                }
            }
        }

        // then
        assertThat(checked).isPositive();
    }

    private void assertRejected(String path, String method) throws Exception {
        String upperMethod = method.toUpperCase(Locale.ROOT);
        String operation = upperMethod + " " + path;
        if (PUBLIC_OPERATIONS.contains(operation)) {
            return;
        }
        String concretePath = path.replaceAll("\\{[^}]+\\}", "1");
        HttpMethod httpMethod = HttpMethod.valueOf(upperMethod);
        MockHttpServletRequestBuilder request = request(httpMethod, concretePath);
        if (!isSafe(httpMethod)) {
            request = request.with(csrf());
        }
        MvcResult result = mockMvc().perform(request).andReturn();
        int status = result.getResponse().getStatus();
        String rawContentType = result.getResponse().getContentType();
        String contentType = Objects.requireNonNull(rawContentType);
        String responseBody = result.getResponse().getContentAsString();
        JsonNode body = MAPPER.readTree(responseBody);
        String code = body.path("code").asString();
        assertThat(status).isEqualTo(401);
        assertThat(contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(code).isEqualTo("unauthenticated");
    }

    private static boolean isSafe(HttpMethod method) {
        return method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS;
    }

    private String generatedContract() throws Exception {
        MvcResult result = docsMockMvc.perform(get(DOCS_PATH)).andReturn();
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);
        String body = result.getResponse().getContentAsString();
        Object document = MAPPER.readValue(body, Object.class);
        String formatted = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        return formatted.replace("\r\n", "\n") + "\n";
    }
}
