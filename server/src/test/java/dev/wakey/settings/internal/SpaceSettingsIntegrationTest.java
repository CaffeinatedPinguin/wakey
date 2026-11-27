package dev.wakey.settings.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import dev.wakey.audit.AuditLog;
import dev.wakey.audit.AuditPage;
import dev.wakey.audit.AuditRecord;
import dev.wakey.checks.CheckDefaultsProperties;
import dev.wakey.checks.CheckSettingDefinitions;
import dev.wakey.checks.CheckSettingsConfiguration;
import dev.wakey.settings.EffectiveSetting;
import dev.wakey.settings.GeneralSettingDefinitions;
import dev.wakey.settings.InstanceDefaultsProperties;
import dev.wakey.settings.Language;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingActor;
import dev.wakey.settings.SettingAudit;
import dev.wakey.settings.SettingCatalog;
import dev.wakey.settings.SettingError;
import dev.wakey.settings.SettingSource;
import dev.wakey.settings.SettingValue;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.spaces.SpaceService;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class SpaceSettingsIntegrationTest extends PostgresIntegrationTest {

    private static final SettingActor ACTOR = new SettingActor("settings-test", false);

    private static final CurrentUser ADMIN = new CurrentUser("https://issuer.example.com", "settings-admin", true);

    private static final InstanceDefaultsProperties DEFAULTS = new InstanceDefaultsProperties("UTC", Language.PL);

    private static final CheckDefaultsProperties CHECK_DEFAULTS = new CheckDefaultsProperties(60, 10, 1);

    private static final PlanProperties SMALL = new PlanProperties(true, 30, 20, 3, 10);

    private static final PlanProperties LARGE = new PlanProperties(false, 5, 120, 10, 100);

    private static final PlanProperties LARGE_DEFAULT = new PlanProperties(true, 5, 120, 10, 100);

    @Test
    void defaultsComeFromTheCatalog() {
        // given
        UUID spaceId = createSpace();
        SpaceSettings settings = settings(Map.of(), DEFAULTS, CHECK_DEFAULTS);

        // when
        Map<String, EffectiveSetting> effective = settings.effectiveAll(spaceId);

        // then
        assertThat(effective)
                .containsEntry("timezone", new EffectiveSetting("UTC", SettingSource.INSTANCE))
                .containsEntry("language", new EffectiveSetting("pl", SettingSource.INSTANCE))
                .containsEntry("check.interval", new EffectiveSetting("60", SettingSource.INSTANCE))
                .containsEntry("check.timeout", new EffectiveSetting("10", SettingSource.INSTANCE))
                .containsEntry("check.retries", new EffectiveSetting("1", SettingSource.INSTANCE));
    }

    @Test
    void noBuiltInDefaultsLiveInCode() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(PlansConfiguration.class, CheckSettingsConfiguration.class);
        List<String> sources = new ArrayList<>();
        for (SettingSource source : SettingSource.values()) {
            sources.add(source.name());
        }

        // when no property is set
        runner.run(context -> {
            // then every default comes from the @DefaultValue annotations
            assertThat(context).hasNotFailed();
            InstanceDefaultsProperties defaults = context.getBean(InstanceDefaultsProperties.class);
            CheckDefaultsProperties check = context.getBean(CheckDefaultsProperties.class);
            assertThat(defaults.timezone()).isEqualTo("UTC");
            assertThat(defaults.language()).isEqualTo(Language.PL);
            assertThat(check.interval()).isEqualTo(60);
            assertThat(check.timeout()).isEqualTo(10);
            assertThat(check.retries()).isEqualTo(1);
        });

        // then there is no fourth, built-in source
        assertThat(sources).doesNotContain("DEFAULT");
    }

    @Test
    void anInstanceDefaultIsReportedAsInstance() {
        // given
        PlanProperties plan = new PlanProperties(true, null, null, null, null);
        SpaceSettings settings = settings(Map.of("default", plan), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        EffectiveSetting timezone = settings.effective(spaceId, "timezone");
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");

        // then
        assertThat(timezone).isEqualTo(new EffectiveSetting("UTC", SettingSource.INSTANCE));
        assertThat(interval).isEqualTo(new EffectiveSetting("60", SettingSource.INSTANCE));
    }

    @Test
    void thePlanBoundClampsTheInstanceDefault() {
        // given
        PlanProperties plan = new PlanProperties(true, 90, 5, 0, 10);
        SpaceSettings settings = settings(Map.of("default", plan), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "check.retries");

        // then the plan bound wins over an instance default outside it
        assertThat(interval).isEqualTo(new EffectiveSetting("90", SettingSource.PLAN));
        assertThat(timeout).isEqualTo(new EffectiveSetting("5", SettingSource.PLAN));
        assertThat(retries).isEqualTo(new EffectiveSetting("0", SettingSource.PLAN));
    }

    @Test
    void planValueOverridesTheInstanceDefault() {
        // given
        InstanceDefaultsProperties defaults = defaults(null, null);
        CheckDefaultsProperties checkDefaults = checkDefaults(45, 25, 5);
        PlanProperties plan = new PlanProperties(true, 30, 20, 3, 10);
        SpaceSettings settings = settings(Map.of("default", plan), defaults, checkDefaults);
        UUID spaceId = createSpace();

        // when
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "check.retries");

        // then
        assertThat(interval).isEqualTo(new EffectiveSetting("45", SettingSource.INSTANCE));
        assertThat(timeout).isEqualTo(new EffectiveSetting("20", SettingSource.PLAN));
        assertThat(retries).isEqualTo(new EffectiveSetting("3", SettingSource.PLAN));
    }

    @Test
    void spaceOverrideWinsWithinThePlan() {
        // given
        SpaceSettings settings = settings(Map.of("small", SMALL), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        set(settings, spaceId, "check.interval", "45");
        set(settings, spaceId, "check.timeout", "15");
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "check.timeout");

        // then
        assertThat(interval).isEqualTo(new EffectiveSetting("45", SettingSource.SPACE));
        assertThat(timeout).isEqualTo(new EffectiveSetting("15", SettingSource.SPACE));
    }

    @Test
    void overrideOutsideThePlanIsRejectedWithItsBound() {
        // given
        SpaceSettings settings = settings(Map.of("small", SMALL), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        Throwable thrown = catchThrowable(() -> set(settings, spaceId, "check.interval", "10"));

        // then
        assertThat(thrown).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) thrown;
        assertThat(exception.getErrorCode()).isEqualTo(SettingError.OUT_OF_PLAN);
        assertThat(exception.getParams())
                .containsEntry("key", "check.interval")
                .containsEntry("bound", 30)
                .containsEntry("planName", "small");
    }

    @Test
    void effectiveValueReportsItsSource() {
        // given
        PlanProperties plan = new PlanProperties(true, 30, 20, null, 10);
        InstanceDefaultsProperties defaults = defaults("Europe/Warsaw", Language.PL);
        SpaceSettings settings = settings(Map.of("default", plan), defaults, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        set(settings, spaceId, "language", "en");

        // when
        EffectiveSetting timezone = settings.effective(spaceId, "timezone");
        EffectiveSetting language = settings.effective(spaceId, "language");
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "check.retries");

        // then
        assertThat(timezone).isEqualTo(new EffectiveSetting("Europe/Warsaw", SettingSource.INSTANCE));
        assertThat(language).isEqualTo(new EffectiveSetting("en", SettingSource.SPACE));
        assertThat(interval).isEqualTo(new EffectiveSetting("60", SettingSource.INSTANCE));
        assertThat(timeout).isEqualTo(new EffectiveSetting("10", SettingSource.INSTANCE));
        assertThat(retries).isEqualTo(new EffectiveSetting("1", SettingSource.INSTANCE));
    }

    @Test
    void aPlanBoundIsNeverTheDefaultValue() {
        // given
        PlanProperties plan = new PlanProperties(true, 10, 30, 5, 10);
        SpaceSettings settings = settings(Map.of("default", plan), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "check.retries");

        // then
        assertThat(interval).isEqualTo(new EffectiveSetting("60", SettingSource.INSTANCE));
        assertThat(timeout).isEqualTo(new EffectiveSetting("10", SettingSource.INSTANCE));
        assertThat(retries).isEqualTo(new EffectiveSetting("1", SettingSource.INSTANCE));

        // when the instance defaults fall outside the bounds
        InstanceDefaultsProperties defaults = defaults(null, null);
        CheckDefaultsProperties checkDefaults = checkDefaults(5, 40, 8);
        SpaceSettings clamped = settings(Map.of("default", plan), defaults, checkDefaults);
        EffectiveSetting clampedInterval = clamped.effective(spaceId, "check.interval");
        EffectiveSetting clampedTimeout = clamped.effective(spaceId, "check.timeout");
        EffectiveSetting clampedRetries = clamped.effective(spaceId, "check.retries");

        // then the nearest bound wins with the PLAN source
        assertThat(clampedInterval).isEqualTo(new EffectiveSetting("10", SettingSource.PLAN));
        assertThat(clampedTimeout).isEqualTo(new EffectiveSetting("30", SettingSource.PLAN));
        assertThat(clampedRetries).isEqualTo(new EffectiveSetting("5", SettingSource.PLAN));
    }

    @Test
    void resetRemovesTheOverride() {
        // given
        SpaceSettings settings = settings(Map.of("small", SMALL), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        set(settings, spaceId, "check.retries", "2");

        // when
        EffectiveSetting before = settings.effective(spaceId, "check.retries");
        reset(settings, spaceId, "check.retries");
        EffectiveSetting after = settings.effective(spaceId, "check.retries");

        // then
        assertThat(before).isEqualTo(new EffectiveSetting("2", SettingSource.SPACE));
        assertThat(after).isEqualTo(new EffectiveSetting("1", SettingSource.INSTANCE));
    }

    @Test
    void resettingAllSettingsIsOneTransaction() {
        // given two overrides and a store that fails on the second delete
        SettingStore realStore = bean(SettingStore.class);
        SettingStore failingStore = spy(realStore);
        AtomicInteger deletes = new AtomicInteger();
        doAnswer(invocation -> {
                    if (deletes.incrementAndGet() == 2) {
                        throw new IllegalStateException("delete failed");
                    }
                    return invocation.callRealMethod();
                })
                .when(failingStore)
                .delete(any(UUID.class), anyString());
        SpaceSettings settings = settingsWith(failingStore, Map.of(), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        set(settings, spaceId, "timezone", "Europe/Warsaw");
        set(settings, spaceId, "check.interval", "45");

        // when the reset fails after the first delete
        Throwable thrown = catchThrowable(() -> inTransaction(() -> settings.resetAll(spaceId, ACTOR)));

        // then the whole transaction rolled back and every override is still stored
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(realStore.findAll(spaceId)).containsOnlyKeys("timezone", "check.interval");
    }

    @Test
    void resettingAllSettingsWithoutOverridesChangesNothing() {
        // given a space without overrides
        SpaceSettings settings = settings(Map.of(), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        SettingStore store = bean(SettingStore.class);
        AuditLog auditLog = bean(AuditLog.class);

        // when every override is reset
        inTransaction(() -> settings.resetAll(spaceId, ACTOR));

        // then nothing is stored and no setting event was written
        assertThat(store.findAll(spaceId)).isEmpty();
        for (var definition : bean(SettingCatalog.class).all()) {
            String key = definition.key().key();
            List<AuditRecord> history = auditLog.history(spaceId, "space_setting", key, new AuditPage(null, 10));
            assertThat(history).isEmpty();
        }
    }

    @Test
    void resettingAllSettingsRemovesEveryOverride() {
        // given two overrides
        SpaceSettings settings = settings(Map.of("small", SMALL), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        SettingStore store = bean(SettingStore.class);
        set(settings, spaceId, "timezone", "Europe/Warsaw");
        set(settings, spaceId, "check.interval", "45");

        // when every override is reset
        inTransaction(() -> settings.resetAll(spaceId, ACTOR));

        // then the effective values fall back and no override is stored
        assertThat(settings.effective(spaceId, "timezone"))
                .isEqualTo(new EffectiveSetting("UTC", SettingSource.INSTANCE));
        assertThat(settings.effective(spaceId, "check.interval"))
                .isEqualTo(new EffectiveSetting("60", SettingSource.INSTANCE));
        assertThat(store.findAll(spaceId)).isEmpty();
    }

    @Test
    void savingTheSameSettingTwiceKeepsOneRow() {
        // given
        SpaceSettings settings = settings(Map.of(), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        set(settings, spaceId, "check.interval", "45");
        set(settings, spaceId, "check.interval", "50");
        long rows = countSettings(spaceId, "check.interval");
        String value = storedValue(spaceId, "check.interval");

        // then
        assertThat(rows).isEqualTo(1L);
        assertThat(value).isEqualTo("50");
    }

    @Test
    void onlyOneDefaultPlanIsAllowed() {
        // given
        ApplicationContextRunner runner =
                new ApplicationContextRunner().withUserConfiguration(PlansConfiguration.class);

        // when
        runner.withPropertyValues("wakey.plans.small.default=true", "wakey.plans.small.min-interval=30")
                .run(context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    PlanCatalog catalog = context.getBean(PlanCatalog.class);
                    PlanProperties small = catalog.plan("small");
                    assertThat(small.defaultPlan()).isTrue();
                    assertThat(small.minInterval()).isEqualTo(30);
                });

        // when
        runner.withPropertyValues("wakey.plans.small.default=true", "wakey.plans.large.default=true")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).hasMessageContaining("Exactly one plan");
                });
    }

    @Test
    void unknownKeyIsRejected() {
        // given
        SpaceSettings settings = settings(Map.of(), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        Throwable effective = catchThrowable(() -> settings.effective(spaceId, "unknown.key"));
        Throwable stored = catchThrowable(() -> set(settings, spaceId, "unknown.key", "value"));
        Throwable removed = catchThrowable(() -> reset(settings, spaceId, "unknown.key"));
        Throwable badFormat = catchThrowable(() -> set(settings, spaceId, "check.interval", "ten"));
        Throwable badLanguage = catchThrowable(() -> set(settings, spaceId, "language", "de"));

        // then
        assertInvalidField(effective, "/key", SettingError.INVALID_KEY);
        assertInvalidField(stored, "/key", SettingError.INVALID_KEY);
        assertInvalidField(removed, "/key", SettingError.INVALID_KEY);
        assertInvalidField(badFormat, "/value", SettingError.INVALID_VALUE);
        assertInvalidField(badLanguage, "/value", SettingError.INVALID_VALUE);
    }

    @Test
    void raisingThePlanAcceptsTheEarlierRejectedValue() {
        // given
        Map<String, PlanProperties> plans = Map.of("small", SMALL, "large", LARGE);
        SpaceSettings settings = settings(plans, DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        Throwable rejected = catchThrowable(() -> set(settings, spaceId, "check.interval", "10"));
        assignPlan(spaceId, "large");
        set(settings, spaceId, "check.interval", "10");
        EffectiveSetting accepted = settings.effective(spaceId, "check.interval");

        // then
        assertThat(rejected).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) rejected;
        assertThat(exception.getErrorCode()).isEqualTo(SettingError.OUT_OF_PLAN);
        assertThat(exception.getParams()).containsEntry("bound", 30);
        assertThat(accepted).isEqualTo(new EffectiveSetting("10", SettingSource.SPACE));
    }

    @Test
    void everySettingWriteIsRecordedInTheAuditLog() {
        // given
        SpaceSettings settings = settings(Map.of(), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        AuditLog auditLog = bean(AuditLog.class);

        // when
        set(settings, spaceId, "check.interval", "45");
        set(settings, spaceId, "check.interval", "50");
        reset(settings, spaceId, "check.interval");
        List<AuditRecord> history =
                auditLog.history(spaceId, "space_setting", "check.interval", new AuditPage(null, 10));

        // then
        assertThat(history)
                .extracting(AuditRecord::action)
                .containsExactly("setting.reset", "setting.set", "setting.set");
        AuditRecord reset = history.get(0);
        AuditRecord secondSet = history.get(1);
        AuditRecord firstSet = history.get(2);
        assertThat(reset.details()).isEqualTo("{\"old\":\"50\",\"new\":null}");
        assertThat(secondSet.details()).isEqualTo("{\"old\":\"45\",\"new\":\"50\"}");
        assertThat(firstSet.details()).isEqualTo("{\"old\":null,\"new\":\"45\"}");
    }

    @Test
    void aRolledBackWriteLeavesNoAuditEvent() {
        // given
        SpaceSettings settings = settings(Map.of(), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        AuditLog auditLog = bean(AuditLog.class);
        TransactionTemplate template = transactionTemplate();

        // when
        template.executeWithoutResult(status -> {
            settings.set(spaceId, new SettingValue("check.interval", "45"), ACTOR);
            status.setRollbackOnly();
        });
        List<AuditRecord> history =
                auditLog.history(spaceId, "space_setting", "check.interval", new AuditPage(null, 10));
        EffectiveSetting interval = settings.effective(spaceId, "check.interval");

        // then
        assertThat(history).isEmpty();
        assertThat(interval).isEqualTo(new EffectiveSetting("60", SettingSource.INSTANCE));
    }

    @Test
    void instanceDefaultsComeFromConfiguration() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(PlansConfiguration.class, CheckSettingsConfiguration.class);

        // when the configuration sets every field
        runner.withPropertyValues(
                        "wakey.defaults.timezone=Europe/Warsaw",
                        "wakey.defaults.language=en",
                        "wakey.defaults.check.interval=30",
                        "wakey.defaults.check.timeout=15",
                        "wakey.defaults.check.retries=2")
                .run(context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    InstanceDefaultsProperties configured = context.getBean(InstanceDefaultsProperties.class);
                    CheckDefaultsProperties check = context.getBean(CheckDefaultsProperties.class);
                    assertThat(configured.timezone()).isEqualTo("Europe/Warsaw");
                    assertThat(configured.language()).isEqualTo(Language.EN);
                    assertThat(check.interval()).isEqualTo(30);
                    assertThat(check.timeout()).isEqualTo(15);
                    assertThat(check.retries()).isEqualTo(2);
                });

        // when no property is set
        runner.run(context -> {
            // then the annotation defaults apply
            assertThat(context).hasNotFailed();
            InstanceDefaultsProperties fallback = context.getBean(InstanceDefaultsProperties.class);
            CheckDefaultsProperties check = context.getBean(CheckDefaultsProperties.class);
            assertThat(fallback.timezone()).isEqualTo("UTC");
            assertThat(fallback.language()).isEqualTo(Language.PL);
            assertThat(check.interval()).isEqualTo(60);
            assertThat(check.timeout()).isEqualTo(10);
            assertThat(check.retries()).isEqualTo(1);
        });
    }

    @Test
    void twoConcurrentWritesCannotLeaveTheTimeoutAboveTheInterval() throws Exception {
        // given
        SpaceSettings settings = settings(Map.of("large", LARGE_DEFAULT), DEFAULTS, CHECK_DEFAULTS);
        UUID spaceId = createSpace();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // when two writes race on the same space, round after round
            for (int round = 0; round < 20; round++) {
                set(settings, spaceId, "check.interval", "60");
                set(settings, spaceId, "check.timeout", "10");
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<@Nullable Throwable> raisedTimeout =
                        executor.submit(() -> attempt(settings, spaceId, "check.timeout", "50", barrier));
                Future<@Nullable Throwable> loweredInterval =
                        executor.submit(() -> attempt(settings, spaceId, "check.interval", "30", barrier));
                @Nullable Throwable timeoutFailure = raisedTimeout.get(120, TimeUnit.SECONDS);
                @Nullable Throwable intervalFailure = loweredInterval.get(120, TimeUnit.SECONDS);
                EffectiveSetting timeout = settings.effective(spaceId, "check.timeout");
                EffectiveSetting interval = settings.effective(spaceId, "check.interval");

                // then one write is rejected and the stored pair stays valid
                long timeoutSeconds = Long.parseLong(timeout.value());
                long intervalSeconds = Long.parseLong(interval.value());
                assertThat(timeoutFailure != null || intervalFailure != null).isTrue();
                assertThat(timeoutSeconds).isLessThanOrEqualTo(intervalSeconds);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static InstanceDefaultsProperties defaults(@Nullable String timezone, @Nullable Language language) {
        return new InstanceDefaultsProperties(
                timezone == null ? "UTC" : timezone, language == null ? Language.PL : language);
    }

    private static CheckDefaultsProperties checkDefaults(int interval, int timeout, int retries) {
        return new CheckDefaultsProperties(interval, timeout, retries);
    }

    private static void assertInvalidField(Throwable throwable, String pointer, SettingError code) {
        assertThat(throwable).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) throwable;
        assertThat(exception.getErrorCode()).isEqualTo(CommonError.VALIDATION_FAILED);
        assertThat(exception.getFieldErrors()).hasSize(1);
        ApiFieldError problem = exception.getFieldErrors().getFirst();
        assertThat(problem.pointer()).isEqualTo(pointer);
        assertThat(problem.code()).isEqualTo(code.getCode());
    }

    private long countSettings(UUID spaceId, String key) {
        return jdbc().sql("SELECT COUNT(*) FROM space_setting WHERE space_id = :spaceId AND key = :key")
                .param("spaceId", spaceId)
                .param("key", key)
                .query(Long.class)
                .single();
    }

    private String storedValue(UUID spaceId, String key) {
        return jdbc().sql("SELECT value FROM space_setting WHERE space_id = :spaceId AND key = :key")
                .param("spaceId", spaceId)
                .param("key", key)
                .query(String.class)
                .single();
    }

    private SpaceSettings settings(
            Map<String, PlanProperties> plans,
            InstanceDefaultsProperties defaults,
            CheckDefaultsProperties checkDefaults) {
        return settingsWith(bean(SettingStore.class), plans, defaults, checkDefaults);
    }

    private SpaceSettings settingsWith(
            SettingStore store,
            Map<String, PlanProperties> plans,
            InstanceDefaultsProperties defaults,
            CheckDefaultsProperties checkDefaults) {
        PlanCatalog plansCatalog = PlanCatalog.from(plans);
        SettingCatalog catalog = new SettingCatalog(
                List.of(new GeneralSettingDefinitions(defaults), new CheckSettingDefinitions(checkDefaults)));
        return new SpaceSettings(store, bean(SpaceService.class), catalog, plansCatalog, bean(SettingAudit.class));
    }

    private void set(SpaceSettings settings, UUID spaceId, String key, String value) {
        SettingValue settingValue = new SettingValue(key, value);
        inTransaction(() -> settings.set(spaceId, settingValue, ACTOR));
    }

    private void reset(SpaceSettings settings, UUID spaceId, String key) {
        inTransaction(() -> settings.reset(spaceId, key, ACTOR));
    }

    private void assignPlan(UUID spaceId, String planName) {
        SpaceService spaces = bean(SpaceService.class);
        String slug = spaces.slugOf(spaceId).orElseThrow();
        spaces.assignPlan(ADMIN, slug, planName);
    }

    private void inTransaction(Runnable action) {
        transactionTemplate().executeWithoutResult(status -> action.run());
    }

    private TransactionTemplate transactionTemplate() {
        PlatformTransactionManager manager = bean(PlatformTransactionManager.class);
        return new TransactionTemplate(manager);
    }

    private @Nullable Throwable attempt(
            SpaceSettings settings, UUID spaceId, String key, String value, CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
            set(settings, spaceId, key, value);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private UUID createSpace() {
        UUID id = UUID.randomUUID();
        String suffix = id.toString().substring(0, 8);
        jdbc().sql("""
                INSERT INTO space (id, slug, name, version, created_at, updated_at)
                VALUES (:id, :slug, :name, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """)
                .param("id", id)
                .param("slug", "settings-" + suffix)
                .param("name", "Settings test space")
                .update();
        return id;
    }
}
