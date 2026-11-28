package dev.wakey.settings.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import dev.wakey.checks.CheckSettingError;
import dev.wakey.settings.SettingError;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.SecurityIntegrationTest;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SettingsControllerIntegrationTest extends SecurityIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String SPACES_PATH = "/api/v1/spaces";

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private static final String ISSUER = "https://issuer.example.com";

    private static final String PROBLEM_JSON = "application/problem+json";

    @Test
    void aNewSpaceUsesTheAssignedPlan() throws Exception { // given
        String admin = uniqueSubject("default-plan");
        String slug = uniqueSlug("default-plan");
        createSpace(slug);

        // when
        MvcResult result = getSettings(admin, true, slug);

        // then
        JsonNode body = jsonOf(result);
        JsonNode interval = requiredSetting(settingsOf(body), "check.interval");
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(planNamesOf(body)).containsOnly("small");
        assertThat(text(interval, "value")).isEqualTo("60");
        assertThat(text(interval, "source")).isEqualTo("INSTANCE");
    }

    @Test
    void adminAssignsAPlanIdempotently() throws Exception {
        // given
        String admin = uniqueSubject("assign-plan");
        String slug = uniqueSlug("assign-plan");
        createSpace(slug);
        long initialVersion = versionOf(getSpace(admin, true, slug));

        // when
        MvcResult first = putPlan(admin, true, slug, "large");
        long afterFirstVersion = versionOf(getSpace(admin, true, slug));
        MvcResult second = putPlan(admin, true, slug, "large");
        long afterSecondVersion = versionOf(getSpace(admin, true, slug));
        JsonNode settingsBody = jsonOf(getSettings(admin, true, slug));

        // then
        assertThat(statusOf(first)).isEqualTo(204);
        assertThat(statusOf(second)).isEqualTo(204);
        assertThat(afterFirstVersion).isEqualTo(initialVersion + 1);
        assertThat(afterSecondVersion).isEqualTo(afterFirstVersion);
        assertThat(planNamesOf(settingsBody)).containsOnly("large");
    }

    @Test
    void nonAdminCannotAssignAPlan() throws Exception {
        // given
        String editor = uniqueSubject("non-admin-plan");
        String slug = uniqueSlug("non-admin-plan");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult result = putPlan(editor, false, slug, "large");
        JsonNode settingsBody = jsonOf(getSettings(editor, false, slug));

        // then
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(codeOf(result)).isEqualTo(CommonError.FORBIDDEN.getCode());
        assertThat(planNamesOf(settingsBody)).containsOnly("small");
    }

    @Test
    void aNonAdminCannotProbePlanNames() throws Exception {
        // given
        String editor = uniqueSubject("non-admin-probe");
        String slug = uniqueSlug("non-admin-probe");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult existingPlan = putPlan(editor, false, slug, "large");
        MvcResult unknownPlan = putPlan(editor, false, slug, "enormous");

        // then
        assertThat(statusOf(existingPlan)).isEqualTo(403);
        assertThat(codeOf(existingPlan)).isEqualTo(CommonError.FORBIDDEN.getCode());
        assertThat(statusOf(unknownPlan)).isEqualTo(403);
        assertThat(codeOf(unknownPlan)).isEqualTo(CommonError.FORBIDDEN.getCode());
    }

    @Test
    void changingThePlanIsRecordedInTheAuditLog() throws Exception {
        // given
        String admin = uniqueSubject("plan-audit");
        String slug = uniqueSlug("plan-audit");
        createSpace(slug);
        UUID spaceId = bean(SpaceService.class).view(fixtureAdmin(), slug).id();
        AuditLog auditLog = bean(AuditLog.class);

        // when
        MvcResult result = putPlan(admin, true, slug, "large");
        List<AuditRecord> history = auditLog.history(spaceId, "space", slug, new AuditPage(null, 10));

        // then
        assertThat(statusOf(result)).isEqualTo(204);
        AuditRecord change = history.stream()
                .filter(record -> record.action().equals("space.plan"))
                .findFirst()
                .orElseThrow();
        assertThat(change.action()).isEqualTo("space.plan");
        assertThat(change.details()).isEqualTo("{\"old\":\"small\",\"new\":\"large\"}");
    }

    @Test
    void assigningTheSamePlanAgainRecordsNoSecondAuditEntry() throws Exception {
        // given
        String admin = uniqueSubject("plan-audit-once");
        String slug = uniqueSlug("plan-audit-once");
        createSpace(slug);
        UUID spaceId = bean(SpaceService.class).view(fixtureAdmin(), slug).id();
        AuditLog auditLog = bean(AuditLog.class);

        // when
        putPlan(admin, true, slug, "large");
        putPlan(admin, true, slug, "large");
        List<AuditRecord> history = auditLog.history(spaceId, "space", slug, new AuditPage(null, 10));

        // then the second, idempotent assignment adds no audit entry
        long planChanges = history.stream()
                .filter(record -> record.action().equals("space.plan"))
                .count();
        assertThat(planChanges).isEqualTo(1L);
    }

    @Test
    void unknownPlanIsRejected() throws Exception {
        // given
        String admin = uniqueSubject("unknown-plan");
        String slug = uniqueSlug("unknown-plan");
        createSpace(slug);

        // when
        MvcResult result = putPlan(admin, true, slug, "enormous");

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(contentTypeOf(result)).isEqualTo(PROBLEM_JSON);
        assertThat(codeOf(result)).isEqualTo(SettingError.UNKNOWN_PLAN.getCode());
    }

    @Test
    void anUnknownPlanAnswersItsCodeAndParams() throws Exception {
        // given
        String admin = uniqueSubject("unknown-plan-params");
        String slug = uniqueSlug("unknown-plan-params");
        createSpace(slug);

        // when
        MvcResult result = putPlan(admin, true, slug, "enormous");
        JsonNode body = jsonOf(result);
        JsonNode params = body.path("params");

        // then
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(text(body, "code")).isEqualTo(SettingError.UNKNOWN_PLAN.getCode());
        assertThat(text(params, "plan")).isEqualTo("enormous");
    }

    @Test
    void editorSetsASettingWithinThePlan() throws Exception {
        // given
        String editor = uniqueSubject("editor-set");
        String slug = uniqueSlug("editor-set");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult result = putSetting(editor, false, slug, "check.interval", "45");
        JsonNode settingsBody = jsonOf(getSettings(editor, false, slug));

        // then
        JsonNode interval = requiredSetting(settingsOf(settingsBody), "check.interval");
        assertThat(statusOf(result)).isEqualTo(204);
        assertThat(text(interval, "value")).isEqualTo("45");
        assertThat(text(interval, "source")).isEqualTo("SPACE");
        assertThat(text(interval, "planName")).isEqualTo("small");
    }

    @Test
    void settingsCatalogRejectsAZeroIntervalAndTimeout() throws Exception {
        // given
        String admin = uniqueSubject("catalog-zero");
        String slug = uniqueSlug("catalog-zero");
        createSpace(slug);
        putPlan(admin, true, slug, "open");

        // when
        MvcResult interval = putSetting(admin, true, slug, "check.interval", "0");
        MvcResult timeout = putSetting(admin, true, slug, "check.timeout", "0");

        // then
        assertThat(statusOf(interval)).isEqualTo(400);
        assertThat(codeOf(interval)).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(statusOf(timeout)).isEqualTo(400);
        assertThat(codeOf(timeout)).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
    }

    @Test
    void settingsCatalogRejectsMoreThanTenRetries() throws Exception {
        // given
        String admin = uniqueSubject("catalog-retries");
        String slug = uniqueSlug("catalog-retries");
        createSpace(slug);
        putPlan(admin, true, slug, "open");

        // when
        MvcResult rejected = putSetting(admin, true, slug, "check.retries", "11");
        MvcResult accepted = putSetting(admin, true, slug, "check.retries", "10");

        // then
        assertThat(statusOf(rejected)).isEqualTo(400);
        assertThat(codeOf(rejected)).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(statusOf(accepted)).isEqualTo(204);
    }

    @Test
    void aValueOutsideThePlanIsRejectedWithItsBound() throws Exception {
        // given
        String editor = uniqueSubject("outside-plan");
        String slug = uniqueSlug("outside-plan");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult result = putSetting(editor, false, slug, "check.interval", "10");

        // then
        JsonNode params = jsonOf(result).path("params");
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(codeOf(result)).isEqualTo(SettingError.OUT_OF_PLAN.getCode());
        assertThat(number(params, "bound")).isEqualTo(30L);
        assertThat(text(params, "planName")).isEqualTo("small");
        assertThat(text(params, "key")).isEqualTo("check.interval");
    }

    @Test
    void aSettingOutsideThePlanAnswers409WithItsParams() throws Exception {
        // given
        String editor = uniqueSubject("outside-retries");
        String slug = uniqueSlug("outside-retries");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);

        // when
        MvcResult result = putSetting(editor, false, slug, "check.retries", "4");
        JsonNode body = jsonOf(result);
        JsonNode params = body.path("params");

        // then the out-of-plan rule is a validation error (400); the name keeps the review wording.
        int expectedStatus = SettingError.OUT_OF_PLAN.getStatus().value();
        assertThat(statusOf(result)).isEqualTo(expectedStatus);
        assertThat(text(body, "code")).isEqualTo(SettingError.OUT_OF_PLAN.getCode());
        assertThat(number(params, "bound")).isEqualTo(3L);
        assertThat(text(params, "planName")).isEqualTo("small");
        assertThat(text(params, "key")).isEqualTo("check.retries");
    }

    @Test
    void effectiveSettingsReportTheirSource() throws Exception {
        // given
        String editor = uniqueSubject("sources");
        String slug = uniqueSlug("sources");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);
        putSetting(editor, false, slug, "check.interval", "45");

        // when
        MvcResult result = getSettings(editor, false, slug);
        JsonNode settingsBody = jsonOf(result);
        Map<String, JsonNode> settings = settingsOf(settingsBody);

        // then
        JsonNode interval = requiredSetting(settings, "check.interval");
        JsonNode timezone = requiredSetting(settings, "timezone");
        JsonNode timeout = requiredSetting(settings, "check.timeout");
        JsonNode language = requiredSetting(settings, "language");
        assertThat(text(interval, "source")).isEqualTo("SPACE");
        assertThat(text(timezone, "source")).isEqualTo("INSTANCE");
        assertThat(text(timezone, "value")).isEqualTo("Europe/Warsaw");
        assertThat(text(timeout, "source")).isEqualTo("INSTANCE");
        assertThat(text(timeout, "value")).isEqualTo("10");
        assertThat(text(language, "source")).isEqualTo("INSTANCE");
        assertThat(text(language, "value")).isEqualTo("pl");
    }

    @Test
    void everySettingDescribesItsTypeUnitOptionsBoundsAndGroup() throws Exception {
        // given
        String admin = uniqueSubject("descriptor");
        String slug = uniqueSlug("descriptor");
        createSpace(slug);

        // when
        MvcResult result = getSettings(admin, true, slug);
        Map<String, JsonNode> settings = settingsOf(jsonOf(result));

        // then the plan bounds of "small" are min interval 30, max timeout 20 and max retries 3
        JsonNode timezone = requiredSetting(settings, "timezone");
        JsonNode language = requiredSetting(settings, "language");
        JsonNode interval = requiredSetting(settings, "check.interval");
        JsonNode timeout = requiredSetting(settings, "check.timeout");
        JsonNode retries = requiredSetting(settings, "check.retries");
        JsonNode languageOptions = language.path("options");
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(text(timezone, "type")).isEqualTo("STRING");
        assertThat(text(timezone, "group")).isEqualTo("GENERAL");
        assertNullField(timezone, "unit");
        assertNullField(timezone, "options");
        assertNullField(timezone, "min");
        assertNullField(timezone, "max");
        assertThat(text(language, "type")).isEqualTo("ENUM");
        assertThat(valuesOf(languageOptions)).containsExactly("pl", "en");
        assertThat(text(language, "group")).isEqualTo("GENERAL");
        assertThat(text(interval, "type")).isEqualTo("INTEGER");
        assertThat(text(interval, "unit")).isEqualTo("s");
        assertThat(number(interval, "min")).isEqualTo(30L);
        assertNullField(interval, "max");
        assertThat(text(interval, "group")).isEqualTo("DEFAULTS");
        assertThat(text(timeout, "unit")).isEqualTo("s");
        assertThat(number(timeout, "min")).isEqualTo(1L);
        assertThat(number(timeout, "max")).isEqualTo(20L);
        assertThat(text(timeout, "group")).isEqualTo("DEFAULTS");
        assertNullField(retries, "unit");
        assertThat(number(retries, "min")).isEqualTo(0L);
        assertThat(number(retries, "max")).isEqualTo(3L);
        assertThat(text(retries, "group")).isEqualTo("DEFAULTS");
    }

    @Test
    void viewerReadsButCannotChangeSettings() throws Exception {
        // given
        String viewer = uniqueSubject("viewer-settings");
        String slug = uniqueSlug("viewer-settings");
        createSpace(slug);
        addMember(slug, viewer, SpaceRole.VIEWER);

        // when
        MvcResult read = getSettings(viewer, false, slug);
        MvcResult write = putSetting(viewer, false, slug, "check.interval", "45");
        MvcResult reset = deleteSetting(viewer, false, slug, "check.interval");
        MvcResult resetAll = deleteAllSettings(viewer, false, slug);
        JsonNode readBody = jsonOf(read);

        // then
        assertThat(statusOf(read)).isEqualTo(200);
        assertThat(planNamesOf(readBody)).containsOnly("small");
        assertThat(statusOf(write)).isEqualTo(403);
        assertThat(statusOf(reset)).isEqualTo(403);
        assertThat(statusOf(resetAll)).isEqualTo(403);
    }

    @Test
    void editorResetsAllSettingsInOneIdempotentRequest() throws Exception {
        // given
        String editor = uniqueSubject("editor-reset-all");
        String slug = uniqueSlug("editor-reset-all");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);
        putSetting(editor, false, slug, "check.interval", "45");
        putSetting(editor, false, slug, "timezone", "Europe/Warsaw");

        // when
        MvcResult first = deleteAllSettings(editor, false, slug);
        MvcResult second = deleteAllSettings(editor, false, slug);
        JsonNode body = jsonOf(getSettings(editor, false, slug));
        JsonNode interval = requiredSetting(settingsOf(body), "check.interval");

        // then
        assertThat(statusOf(first)).isEqualTo(204);
        assertThat(statusOf(second)).isEqualTo(204);
        assertThat(text(interval, "value")).isEqualTo("60");
        assertThat(text(interval, "source")).isEqualTo("INSTANCE");
    }

    @Test
    void strangerGetsNotFoundForTheSettingsOfAnotherSpace() throws Exception {
        // given
        String stranger = uniqueSubject("stranger-settings");
        String slug = uniqueSlug("stranger-settings");
        createSpace(slug);

        // when
        MvcResult read = getSettings(stranger, false, slug);
        MvcResult write = putSetting(stranger, false, slug, "check.interval", "45");

        // then
        assertThat(statusOf(read)).isEqualTo(404);
        assertThat(contentTypeOf(read)).isEqualTo(PROBLEM_JSON);
        assertThat(codeOf(read)).isEqualTo(CommonError.NOT_FOUND.getCode());
        assertThat(statusOf(write)).isEqualTo(404);
        assertThat(codeOf(write)).isEqualTo(CommonError.NOT_FOUND.getCode());
    }

    @Test
    void anOverrideOutsideTheCurrentPlanIsIgnored() throws Exception {
        // given
        String admin = uniqueSubject("narrowing-admin");
        String editor = uniqueSubject("narrowing-editor");
        String slug = uniqueSlug("narrowing");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);
        putPlan(admin, true, slug, "large");
        putSetting(editor, false, slug, "check.interval", "10");

        // when
        putPlan(admin, true, slug, "small");
        JsonNode narrowed = jsonOf(getSettings(editor, false, slug));
        putPlan(admin, true, slug, "large");
        JsonNode widened = jsonOf(getSettings(editor, false, slug));

        // then
        JsonNode narrowedInterval = requiredSetting(settingsOf(narrowed), "check.interval");
        JsonNode widenedInterval = requiredSetting(settingsOf(widened), "check.interval");
        assertThat(text(narrowedInterval, "value")).isEqualTo("60");
        assertThat(text(narrowedInterval, "source")).isEqualTo("INSTANCE");
        assertThat(text(widenedInterval, "value")).isEqualTo("10");
        assertThat(text(widenedInterval, "source")).isEqualTo("SPACE");
    }

    @Test
    void aPlanBoundIsNeverTheDefaultValue() throws Exception {
        // given
        String admin = uniqueSubject("bound-default");
        String slug = uniqueSlug("bound-default");
        createSpace(slug);

        // when
        JsonNode body = jsonOf(getSettings(admin, true, slug));
        Map<String, JsonNode> settings = settingsOf(body);

        // then
        JsonNode interval = requiredSetting(settings, "check.interval");
        JsonNode timeout = requiredSetting(settings, "check.timeout");
        JsonNode retries = requiredSetting(settings, "check.retries");
        assertThat(text(interval, "value")).isEqualTo("60");
        assertThat(text(interval, "source")).isEqualTo("INSTANCE");
        assertThat(text(timeout, "value")).isEqualTo("10");
        assertThat(text(timeout, "source")).isEqualTo("INSTANCE");
        assertThat(text(retries, "value")).isEqualTo("1");
        assertThat(text(retries, "source")).isEqualTo("INSTANCE");
    }

    @Test
    void timeoutCannotExceedTheIntervalInSettings() throws Exception {
        // given
        String admin = uniqueSubject("pair-admin");
        String editor = uniqueSubject("pair-editor");
        String slug = uniqueSlug("pair");
        createSpace(slug);
        addMember(slug, editor, SpaceRole.EDITOR);
        putPlan(admin, true, slug, "open");

        // when
        MvcResult timeout = putSetting(editor, false, slug, "check.timeout", "70");
        MvcResult interval = putSetting(editor, false, slug, "check.interval", "5");
        JsonNode timeoutBody = jsonOf(timeout);
        JsonNode intervalBody = jsonOf(interval);

        // then
        JsonNode timeoutError = timeoutBody.path("errors").path(0);
        JsonNode intervalError = intervalBody.path("errors").path(0);
        assertThat(statusOf(timeout)).isEqualTo(400);
        assertThat(codeOf(timeout)).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(timeoutError, "code")).isEqualTo(CheckSettingError.TIMEOUT_EXCEEDS_INTERVAL.getCode());
        assertThat(statusOf(interval)).isEqualTo(400);
        assertThat(codeOf(interval)).isEqualTo(CommonError.VALIDATION_FAILED.getCode());
        assertThat(text(intervalError, "code")).isEqualTo(CheckSettingError.TIMEOUT_EXCEEDS_INTERVAL.getCode());
    }

    @Test
    void settingValueIsCappedAndCanonical() throws Exception {
        // given
        String admin = uniqueSubject("canonical");
        String slug = uniqueSlug("canonical");
        createSpace(slug);
        putPlan(admin, true, slug, "open");

        // when
        MvcResult saved = putSetting(admin, true, slug, "check.interval", "0060");
        JsonNode read = jsonOf(getSettings(admin, true, slug));
        MvcResult tooLong = putSetting(admin, true, slug, "timezone", "x".repeat(300));
        JsonNode tooLongBody = jsonOf(tooLong);

        // then
        JsonNode interval = requiredSetting(settingsOf(read), "check.interval");
        JsonNode tooLongError = tooLongBody.path("errors").path(0);
        assertThat(statusOf(saved)).isEqualTo(204);
        assertThat(text(interval, "value")).isEqualTo("60");
        assertThat(text(interval, "source")).isEqualTo("SPACE");
        assertThat(statusOf(tooLong)).isEqualTo(400);
        assertThat(text(tooLongError, "code")).isEqualTo("size");
    }

    @Test
    void theSettingsListIsACursorPage() throws Exception {
        // given
        String admin = uniqueSubject("settings-page");
        String slug = uniqueSlug("settings-page");
        createSpace(slug);

        // when
        MvcResult result = getSettings(admin, true, slug);
        JsonNode body = jsonOf(result);
        JsonNode items = body.path("items");
        JsonNode nextCursor = body.path("nextCursor");

        // then
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isPositive();
        assertThat(nextCursor.isNull()).isTrue();
    }

    private MvcResult getSettings(String subject, boolean admin, String slug) throws Exception {
        MockHttpServletRequestBuilder request =
                get(SPACES_PATH + "/" + slug + "/settings").with(login(subject, admin));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult getSpace(String subject, boolean admin, String slug) throws Exception {
        MockHttpServletRequestBuilder request = get(SPACES_PATH + "/" + slug).with(login(subject, admin));
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult putSetting(String subject, boolean admin, String slug, String key, String value)
            throws Exception {
        String body = "{\"value\":\"" + value + "\"}";
        MockHttpServletRequestBuilder request = put(SPACES_PATH + "/" + slug + "/settings/" + key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(login(subject, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult deleteSetting(String subject, boolean admin, String slug, String key) throws Exception {
        MockHttpServletRequestBuilder request = delete(SPACES_PATH + "/" + slug + "/settings/" + key)
                .with(login(subject, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult deleteAllSettings(String subject, boolean admin, String slug) throws Exception {
        MockHttpServletRequestBuilder request = delete(SPACES_PATH + "/" + slug + "/settings")
                .with(login(subject, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private MvcResult putPlan(String subject, boolean admin, String slug, String plan) throws Exception {
        String body = "{\"plan\":\"" + plan + "\"}";
        MockHttpServletRequestBuilder request = put(SPACES_PATH + "/" + slug + "/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(login(subject, admin))
                .with(csrf());
        return mockMvc().perform(request).andReturn();
    }

    private void createSpace(String slug) {
        SpaceService spaces = bean(SpaceService.class);
        spaces.put(fixtureAdmin(), slug, "Settings space", null);
        spaces.assignPlan(fixtureAdmin(), slug, "small");
    }

    private void addMember(String slug, String subject, SpaceRole role) {
        bean(SpaceMembers.class).addMember(fixtureAdmin(), slug, new SpaceMember(ISSUER, subject, role), false);
    }

    private static RequestPostProcessor login(String subject, boolean admin) {
        if (admin) {
            return oidcLogin()
                    .idToken(token -> token.subject(subject).issuer(ISSUER))
                    .authorities(new SimpleGrantedAuthority(ADMIN_ROLE));
        }
        return oidcLogin().idToken(token -> token.subject(subject).issuer(ISSUER));
    }

    private static CurrentUser fixtureAdmin() {
        return new CurrentUser(ISSUER, "fixture-admin", true);
    }

    private static Map<String, JsonNode> settingsOf(JsonNode body) {
        Map<String, JsonNode> settings = new LinkedHashMap<>();
        for (JsonNode item : body.path("items")) {
            String key = item.path("key").asString();
            settings.put(key, item);
        }
        return settings;
    }

    private static JsonNode requiredSetting(Map<String, JsonNode> settings, String key) {
        return Objects.requireNonNull(settings.get(key), "Missing setting " + key);
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asString();
    }

    private static long number(JsonNode node, String field) {
        return node.path(field).asLong();
    }

    private static List<String> valuesOf(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asString());
        }
        return values;
    }

    private static void assertNullField(JsonNode node, String field) {
        JsonNode value = node.path(field);
        assertThat(value.isNull()).isTrue();
    }

    private static Set<String> planNamesOf(JsonNode body) {
        Set<String> planNames = new LinkedHashSet<>();
        for (JsonNode item : body.path("items")) {
            String planName = item.path("planName").asString();
            planNames.add(planName);
        }
        return planNames;
    }

    private static long versionOf(MvcResult result) throws Exception {
        return jsonOf(result).path("version").asLong();
    }

    private static String codeOf(MvcResult result) throws Exception {
        return jsonOf(result).path("code").asString();
    }

    private static int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String contentTypeOf(MvcResult result) {
        String contentType = result.getResponse().getContentType();
        return Objects.requireNonNull(contentType);
    }

    private static JsonNode jsonOf(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return MAPPER.readTree(content);
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String uniqueSubject(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
