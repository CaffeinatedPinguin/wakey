package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceSlug;
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

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private static final String SPACE_ROLE_REFERENCE = "#/components/schemas/" + SpaceRole.class.getSimpleName();

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
        JsonNode spaceRole = schemas.path("SpaceRole");

        // when
        JsonNode role = space.path("properties").path("role");
        JsonNode nextCursorSchema =
                cursorPage.path("properties").path("nextCursor").path("type");
        List<String> spaceRequired = valuesOf(space.path("required"));
        List<String> cursorPageRequired = valuesOf(cursorPage.path("required"));
        List<String> roleTypes = unionTypes(role);
        List<String> spaceRoleType = valuesOf(spaceRole.path("type"));
        List<String> nextCursorType = valuesOf(nextCursorSchema);

        // then
        assertThat(spaceRequired).containsExactlyInAnyOrder("slug", "name", "version", "role");
        assertThat(cursorPageRequired).containsExactlyInAnyOrder("items", "nextCursor");
        String roleReference = referenceOf(role);
        assertThat(roleReference).isEqualTo(SPACE_ROLE_REFERENCE);
        assertThat(roleTypes).contains("null");
        assertThat(spaceRoleType).contains("string");
        assertThat(nextCursorType).contains("string", "null");
    }

    @Test
    void spaceRoleIsANamedSchemaInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode schemas = document.path("components").path("schemas");
        JsonNode role = schemas.path("SpaceResponse").path("properties").path("role");
        JsonNode spaceRole = schemas.path("SpaceRole");

        // when
        List<String> roleValues = valuesOf(spaceRole.path("enum"));
        String roleReference = referenceOf(role);

        // then
        assertThat(roleValues).containsExactly("EDITOR", "VIEWER");
        assertThat(roleReference).isEqualTo(SPACE_ROLE_REFERENCE);
    }

    @Test
    void theResponseRoleIsNullableWhileTheSpaceRoleIsNot() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode schemas = document.path("components").path("schemas");
        JsonNode role = schemas.path("SpaceResponse").path("properties").path("role");
        JsonNode spaceRole = schemas.path("SpaceRole");

        // when
        String roleReference = referenceOf(role);
        List<String> spaceRoleType = valuesOf(spaceRole.path("type"));
        List<String> roleTypes = unionTypes(role);

        // then
        assertThat(roleReference).isEqualTo(SPACE_ROLE_REFERENCE);
        assertThat(spaceRoleType).containsExactly("string");
        assertThat(roleTypes).contains("null");
    }

    @Test
    void aRoleInARequestIsNotNullable() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode schemas = document.path("components").path("schemas");
        JsonNode spaceRole = schemas.path("SpaceRole");
        JsonNode addMember = schemas.path("AddSpaceMemberRequest");
        JsonNode changeRole = schemas.path("ChangeSpaceMemberRoleRequest");

        // when
        List<String> spaceRoleType = valuesOf(spaceRole.path("type"));
        List<String> addRequired = valuesOf(addMember.path("required"));
        List<String> changeRequired = valuesOf(changeRole.path("required"));
        JsonNode addRole = addMember.path("properties").path("role");
        JsonNode changeRoleRole = changeRole.path("properties").path("role");

        // then
        assertThat(spaceRoleType).containsExactly("string");
        assertThat(addRequired).contains("role");
        assertThat(changeRequired).contains("role");
        assertThat(referenceOf(addRole)).isEqualTo(SPACE_ROLE_REFERENCE);
        assertThat(referenceOf(changeRoleRole)).isEqualTo(SPACE_ROLE_REFERENCE);
        assertThat(unionTypes(addRole)).doesNotContain("null");
        assertThat(unionTypes(changeRoleRole)).doesNotContain("null");
    }

    @Test
    void theSlugPathParameterHasThePatternInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode parameters =
                document.path("paths").path("/api/v1/spaces/{slug}").path("get").path("parameters");

        // when
        JsonNode slug = parameterNamed(parameters, "slug");

        // then
        JsonNode slugSchema = slug.path("schema");
        String pattern = slugSchema.path("pattern").asString();
        assertThat(pattern).isEqualTo(SpaceSlug.PATTERN);
    }

    @Test
    void limitHasBoundsInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode parameters =
                document.path("paths").path("/api/v1/spaces").path("get").path("parameters");
        int maxPageSize = bean(ApiProperties.class).maxPageSize();

        // when
        JsonNode schema = parameterNamed(parameters, "limit").path("schema");

        // then
        int minimum = schema.path("minimum").asInt();
        int maximum = schema.path("maximum").asInt();
        assertThat(minimum).isEqualTo(1);
        assertThat(maximum).isEqualTo(maxPageSize);
    }

    @Test
    void theContractHasOneSpacesTagAndNoPreconditionHeaders() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode paths = document.path("paths");
        Set<String> spacesTags = new LinkedHashSet<>();
        int spacesOperations = 0;

        // when
        for (String path : paths.propertyNames()) {
            if (!path.startsWith("/api/v1/spaces")) {
                continue;
            }
            for (String method : HTTP_METHODS) {
                JsonNode operation = paths.path(path).path(method);
                if (!operation.isMissingNode()) {
                    spacesOperations++;
                    JsonNode tags = operation.path("tags");
                    spacesTags.addAll(valuesOf(tags));
                    assertNoPreconditionHeader(operation);
                }
            }
        }

        // then
        assertThat(spacesOperations).isEqualTo(8);
        assertThat(spacesTags).containsExactly("spaces");
    }

    @Test
    void everyApiOperationAnswers401InTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode paths = document.path("paths");
        int checked = 0;

        // when
        for (String path : paths.propertyNames()) {
            for (String method : HTTP_METHODS) {
                JsonNode operation = paths.path(path).path(method);
                if (!operation.isMissingNode()) {
                    checked++;
                    boolean answersUnauthorized = operation.path("responses").has("401");
                    assertThat(answersUnauthorized).as(path + " " + method).isTrue();
                }
            }
        }

        // then
        assertThat(checked).isPositive();
    }

    @Test
    void everyOperationDocumentsItsErrorResponses() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode paths = document.path("paths");
        JsonNode listSpaces = paths.path("/api/v1/spaces").path("get");
        JsonNode getSpace = paths.path("/api/v1/spaces/{slug}").path("get");
        JsonNode createSpace = paths.path("/api/v1/spaces").path("post");
        JsonNode putSpace = paths.path("/api/v1/spaces/{slug}").path("put");
        JsonNode listMembers = paths.path("/api/v1/spaces/{slug}/members").path("get");
        JsonNode addMember = paths.path("/api/v1/spaces/{slug}/members").path("post");
        JsonNode changeRole = paths.path("/api/v1/spaces/{slug}/members/role").path("put");
        JsonNode removeMember = paths.path("/api/v1/spaces/{slug}/members").path("delete");

        // when
        // then
        assertErrorResponses(listSpaces, "GET /api/v1/spaces", "400", "401");
        assertErrorResponses(getSpace, "GET /api/v1/spaces/{slug}", "400", "401", "404");
        assertErrorResponses(createSpace, "POST /api/v1/spaces", "400", "401", "403", "409");
        assertErrorResponses(putSpace, "PUT /api/v1/spaces/{slug}", "400", "401", "403", "409");
        assertErrorResponses(listMembers, "GET /api/v1/spaces/{slug}/members", "400", "401", "404");
        assertErrorResponses(addMember, "POST /api/v1/spaces/{slug}/members", "400", "401", "403", "404");
        assertErrorResponses(changeRole, "PUT /api/v1/spaces/{slug}/members/role", "400", "401", "403", "404");
        assertErrorResponses(removeMember, "DELETE /api/v1/spaces/{slug}/members", "400", "401", "403", "404");
    }

    @Test
    void theContractHasTheMembershipOperations() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode members = document.path("paths").path("/api/v1/spaces/{slug}/members");
        JsonNode role = document.path("paths").path("/api/v1/spaces/{slug}/members/role");

        // when
        String listId = members.path("get").path("operationId").asString();
        String addId = members.path("post").path("operationId").asString();
        String changeId = role.path("put").path("operationId").asString();
        String removeId = members.path("delete").path("operationId").asString();
        JsonNode changeParameters = role.path("put").path("parameters");
        String issuerLocation =
                parameterNamed(changeParameters, "issuer").path("in").asString();
        String subjectLocation =
                parameterNamed(changeParameters, "subject").path("in").asString();

        // then
        assertThat(listId).isEqualTo("listSpaceMembers");
        assertThat(addId).isEqualTo("addSpaceMember");
        assertThat(changeId).isEqualTo("changeSpaceMemberRole");
        assertThat(removeId).isEqualTo("removeSpaceMember");
        assertThat(issuerLocation).isEqualTo("query");
        assertThat(subjectLocation).isEqualTo("query");
    }

    @Test
    void changingARoleAndRemovingAMemberRequireIssuerAndSubjectInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode changeParameters = document.path("paths")
                .path("/api/v1/spaces/{slug}/members/role")
                .path("put")
                .path("parameters");
        JsonNode removeParameters = document.path("paths")
                .path("/api/v1/spaces/{slug}/members")
                .path("delete")
                .path("parameters");

        // when
        JsonNode changeIssuer = parameterNamed(changeParameters, "issuer");
        JsonNode changeSubject = parameterNamed(changeParameters, "subject");
        JsonNode removeIssuer = parameterNamed(removeParameters, "issuer");
        JsonNode removeSubject = parameterNamed(removeParameters, "subject");

        // then
        assertThat(isRequired(changeIssuer)).isTrue();
        assertThat(isRequired(changeSubject)).isTrue();
        assertThat(isRequired(removeIssuer)).isTrue();
        assertThat(isRequired(removeSubject)).isTrue();
        assertThat(maxLengthOf(changeIssuer)).isEqualTo(SpaceMember.ISSUER_MAX_LENGTH);
        assertThat(maxLengthOf(changeSubject)).isEqualTo(SpaceMember.SUBJECT_MAX_LENGTH);
        assertThat(maxLengthOf(removeIssuer)).isEqualTo(SpaceMember.ISSUER_MAX_LENGTH);
        assertThat(maxLengthOf(removeSubject)).isEqualTo(SpaceMember.SUBJECT_MAX_LENGTH);
    }

    @Test
    void listSpacesProducesJsonInTheContract() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode operation = document.path("paths").path("/api/v1/spaces").path("get");

        // when
        JsonNode content = operation.path("responses").path("200").path("content");

        // then
        assertThat(content.has(MediaType.APPLICATION_JSON_VALUE)).isTrue();
        assertThat(content.has(MediaType.ALL_VALUE)).isFalse();
    }

    @Test
    void thePutDescriptionMentionsAConcurrentConflict() throws Exception {
        // given
        JsonNode document = MAPPER.readTree(generatedContract());
        JsonNode operation =
                document.path("paths").path("/api/v1/spaces/{slug}").path("put");
        JsonNode version = document.path("components")
                .path("schemas")
                .path("PutSpaceRequest")
                .path("properties")
                .path("version");

        // when
        String operationDescription = operation.path("description").asString();
        String versionDescription = version.path("description").asString();

        // then
        assertThat(operationDescription).contains("version", "concurrent", "version_conflict");
        assertThat(versionDescription).contains("version", "concurrent", "409");
    }

    private static void assertErrorResponses(JsonNode operation, String label, String... statuses) {
        for (String status : statuses) {
            JsonNode response = operation.path("responses").path(status);
            JsonNode mediaType = response.path("content").path(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            String reference = mediaType.path("schema").path("$ref").asString();
            assertThat(response.isMissingNode()).as(label + " " + status).isFalse();
            assertThat(reference).as(label + " " + status).isEqualTo(ERROR_SCHEMA_REF);
        }
    }

    private static void assertNoPreconditionHeader(JsonNode operation) {
        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asString();
            assertThat(name).isNotEqualTo("If-None-Match");
            assertThat(name).isNotEqualTo("If-Match");
        }
    }

    private static JsonNode parameterNamed(JsonNode parameters, String name) {
        for (JsonNode parameter : parameters) {
            String parameterName = parameter.path("name").asString();
            if (name.equals(parameterName)) {
                return parameter;
            }
        }
        throw new AssertionError("Missing parameter " + name);
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

    private static List<String> unionTypes(JsonNode schema) {
        JsonNode type = schema.path("type");
        List<String> types = new ArrayList<>(valuesOf(type));
        for (JsonNode alternative : schema.path("anyOf")) {
            JsonNode alternativeType = alternative.path("type");
            types.addAll(valuesOf(alternativeType));
        }
        return types;
    }

    private static String referenceOf(JsonNode schema) {
        if (schema.has("$ref")) {
            return schema.path("$ref").asString();
        }
        for (JsonNode alternative : schema.path("anyOf")) {
            if (alternative.has("$ref")) {
                return alternative.path("$ref").asString();
            }
        }
        return "";
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
        assertThat(code).isEqualTo(CommonError.UNAUTHENTICATED.getCode());
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

    private static boolean isRequired(JsonNode parameter) {
        return parameter.path("required").asBoolean();
    }

    private static int maxLengthOf(JsonNode parameter) {
        JsonNode schema = parameter.path("schema");
        return schema.path("maxLength").asInt();
    }
}
