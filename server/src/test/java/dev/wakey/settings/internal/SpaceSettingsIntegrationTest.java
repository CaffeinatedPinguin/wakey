package dev.wakey.settings.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.settings.EffectiveSetting;
import dev.wakey.settings.InstanceDefaultsProperties;
import dev.wakey.settings.InvalidSettingException;
import dev.wakey.settings.PlanProperties;
import dev.wakey.settings.SettingOutOfPlanException;
import dev.wakey.settings.SettingSource;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.testsupport.PostgresIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SpaceSettingsIntegrationTest extends PostgresIntegrationTest {

    private static final InstanceDefaultsProperties NO_DEFAULTS = new InstanceDefaultsProperties(null, null, null);

    private static final PlanProperties SMALL = new PlanProperties(true, 30, 20, 3, 10);

    private static final PlanProperties LARGE = new PlanProperties(false, 5, 120, 10, 100);

    @Test
    void defaultsComeFromTheCatalog() {
        // given
        UUID spaceId = createSpace();
        SpaceSettings settings = settings(Map.of(), NO_DEFAULTS);

        // when
        Map<String, EffectiveSetting> effective = settings.effectiveAll(spaceId, "default");

        // then
        assertThat(effective)
                .containsEntry("timezone", new EffectiveSetting("UTC", SettingSource.DEFAULT))
                .containsEntry("language", new EffectiveSetting("pl", SettingSource.DEFAULT))
                .containsEntry("check.interval", new EffectiveSetting("60", SettingSource.DEFAULT))
                .containsEntry("check.timeout", new EffectiveSetting("10", SettingSource.DEFAULT))
                .containsEntry("check.retries", new EffectiveSetting("1", SettingSource.DEFAULT));
    }

    @Test
    void planValueOverridesTheInstanceDefault() {
        // given
        InstanceDefaultsProperties.CheckDefaults check = new InstanceDefaultsProperties.CheckDefaults(45, 25, 5);
        InstanceDefaultsProperties defaults = new InstanceDefaultsProperties(null, null, check);
        PlanProperties plan = new PlanProperties(true, 30, 20, 3, 10);
        SpaceSettings settings = settings(Map.of("default", plan), defaults);
        UUID spaceId = createSpace();

        // when
        EffectiveSetting interval = settings.effective(spaceId, "default", "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "default", "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "default", "check.retries");

        // then
        assertThat(interval).isEqualTo(new EffectiveSetting("45", SettingSource.INSTANCE));
        assertThat(timeout).isEqualTo(new EffectiveSetting("20", SettingSource.PLAN));
        assertThat(retries).isEqualTo(new EffectiveSetting("3", SettingSource.PLAN));
    }

    @Test
    void spaceOverrideWinsWithinThePlan() {
        // given
        SpaceSettings settings = settings(Map.of("small", SMALL), NO_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        settings.set(spaceId, "small", "check.interval", "45");
        settings.set(spaceId, "small", "check.timeout", "15");
        EffectiveSetting interval = settings.effective(spaceId, "small", "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "small", "check.timeout");

        // then
        assertThat(interval).isEqualTo(new EffectiveSetting("45", SettingSource.SPACE));
        assertThat(timeout).isEqualTo(new EffectiveSetting("15", SettingSource.SPACE));
    }

    @Test
    void overrideOutsideThePlanIsRejectedWithItsBound() {
        // given
        SpaceSettings settings = settings(Map.of("small", SMALL), NO_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        Throwable thrown = catchThrowable(() -> settings.set(spaceId, "small", "check.interval", "10"));

        // then
        assertThat(thrown).isInstanceOf(SettingOutOfPlanException.class);
        SettingOutOfPlanException exception = (SettingOutOfPlanException) thrown;
        assertThat(exception.key()).isEqualTo("check.interval");
        assertThat(exception.value()).isEqualTo(10L);
        assertThat(exception.bound()).isEqualTo(30L);
        assertThat(exception.planName()).isEqualTo("small");
    }

    @Test
    void effectiveValueReportsItsSource() {
        // given
        PlanProperties plan = new PlanProperties(true, 30, 20, null, 10);
        InstanceDefaultsProperties defaults = new InstanceDefaultsProperties("Europe/Warsaw", null, null);
        SpaceSettings settings = settings(Map.of("default", plan), defaults);
        UUID spaceId = createSpace();
        settings.set(spaceId, "default", "language", "en");

        // when
        EffectiveSetting timezone = settings.effective(spaceId, "default", "timezone");
        EffectiveSetting language = settings.effective(spaceId, "default", "language");
        EffectiveSetting interval = settings.effective(spaceId, "default", "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "default", "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "default", "check.retries");

        // then
        assertThat(timezone).isEqualTo(new EffectiveSetting("Europe/Warsaw", SettingSource.INSTANCE));
        assertThat(language).isEqualTo(new EffectiveSetting("en", SettingSource.SPACE));
        assertThat(interval).isEqualTo(new EffectiveSetting("60", SettingSource.DEFAULT));
        assertThat(timeout).isEqualTo(new EffectiveSetting("10", SettingSource.DEFAULT));
        assertThat(retries).isEqualTo(new EffectiveSetting("1", SettingSource.DEFAULT));
    }

    @Test
    void aPlanBoundIsNeverTheDefaultValue() {
        // given
        PlanProperties plan = new PlanProperties(true, 10, 30, 5, 10);
        SpaceSettings settings = settings(Map.of("default", plan), NO_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        EffectiveSetting interval = settings.effective(spaceId, "default", "check.interval");
        EffectiveSetting timeout = settings.effective(spaceId, "default", "check.timeout");
        EffectiveSetting retries = settings.effective(spaceId, "default", "check.retries");

        // then
        assertThat(interval).isEqualTo(new EffectiveSetting("60", SettingSource.DEFAULT));
        assertThat(timeout).isEqualTo(new EffectiveSetting("10", SettingSource.DEFAULT));
        assertThat(retries).isEqualTo(new EffectiveSetting("1", SettingSource.DEFAULT));

        // when the instance defaults fall outside the bounds
        InstanceDefaultsProperties.CheckDefaults check = new InstanceDefaultsProperties.CheckDefaults(5, 40, 8);
        InstanceDefaultsProperties defaults = new InstanceDefaultsProperties(null, null, check);
        SpaceSettings clamped = settings(Map.of("default", plan), defaults);
        EffectiveSetting clampedInterval = clamped.effective(spaceId, "default", "check.interval");
        EffectiveSetting clampedTimeout = clamped.effective(spaceId, "default", "check.timeout");
        EffectiveSetting clampedRetries = clamped.effective(spaceId, "default", "check.retries");

        // then the nearest bound wins with the PLAN source
        assertThat(clampedInterval).isEqualTo(new EffectiveSetting("10", SettingSource.PLAN));
        assertThat(clampedTimeout).isEqualTo(new EffectiveSetting("30", SettingSource.PLAN));
        assertThat(clampedRetries).isEqualTo(new EffectiveSetting("5", SettingSource.PLAN));
    }

    @Test
    void resetRemovesTheOverride() {
        // given
        SpaceSettings settings = settings(Map.of("small", SMALL), NO_DEFAULTS);
        UUID spaceId = createSpace();
        settings.set(spaceId, "small", "check.retries", "2");

        // when
        EffectiveSetting before = settings.effective(spaceId, "small", "check.retries");
        settings.reset(spaceId, "check.retries");
        EffectiveSetting after = settings.effective(spaceId, "small", "check.retries");

        // then
        assertThat(before).isEqualTo(new EffectiveSetting("2", SettingSource.SPACE));
        assertThat(after).isEqualTo(new EffectiveSetting("1", SettingSource.DEFAULT));
    }

    @Test
    void savingTheSameSettingTwiceKeepsOneRow() {
        // given
        SpaceSettings settings = settings(Map.of(), NO_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        settings.set(spaceId, "default", "check.interval", "45");
        settings.set(spaceId, "default", "check.interval", "50");
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
        SpaceSettings settings = settings(Map.of(), NO_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        Throwable effective = catchThrowable(() -> settings.effective(spaceId, "default", "unknown.key"));
        Throwable stored = catchThrowable(() -> settings.set(spaceId, "default", "unknown.key", "value"));
        Throwable removed = catchThrowable(() -> settings.reset(spaceId, "unknown.key"));
        Throwable badFormat = catchThrowable(() -> settings.set(spaceId, "default", "check.interval", "ten"));
        Throwable badLanguage = catchThrowable(() -> settings.set(spaceId, "default", "language", "de"));

        // then
        assertThat(effective).isInstanceOf(InvalidSettingException.class);
        assertThat(stored).isInstanceOf(InvalidSettingException.class);
        assertThat(removed).isInstanceOf(InvalidSettingException.class);
        assertThat(badFormat).isInstanceOf(InvalidSettingException.class);
        assertThat(badLanguage).isInstanceOf(InvalidSettingException.class);
    }

    @Test
    void raisingThePlanAcceptsTheEarlierRejectedValue() {
        // given
        Map<String, PlanProperties> plans = Map.of("small", SMALL, "large", LARGE);
        SpaceSettings settings = settings(plans, NO_DEFAULTS);
        UUID spaceId = createSpace();

        // when
        Throwable rejected = catchThrowable(() -> settings.set(spaceId, "small", "check.interval", "10"));
        settings.set(spaceId, "large", "check.interval", "10");
        EffectiveSetting accepted = settings.effective(spaceId, "large", "check.interval");

        // then
        assertThat(rejected).isInstanceOf(SettingOutOfPlanException.class);
        SettingOutOfPlanException exception = (SettingOutOfPlanException) rejected;
        assertThat(exception.bound()).isEqualTo(30L);
        assertThat(accepted).isEqualTo(new EffectiveSetting("10", SettingSource.SPACE));
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

    private SpaceSettings settings(Map<String, PlanProperties> plans, InstanceDefaultsProperties defaults) {
        PlanCatalog catalog = PlanCatalog.from(plans, defaults);
        return new JdbcSpaceSettings(jdbc(), catalog);
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
