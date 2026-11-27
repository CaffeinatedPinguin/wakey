package dev.wakey.settings;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import dev.wakey.checks.CheckDefaultsProperties;
import dev.wakey.checks.CheckSettingDefinitions;
import dev.wakey.checks.CheckSettingKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingCatalogTest {

    private static final InstanceDefaultsProperties DEFAULTS = new InstanceDefaultsProperties("UTC", Language.PL);

    private static final CheckDefaultsProperties CHECK_DEFAULTS = new CheckDefaultsProperties(60, 10, 1);

    @Test
    void checkSettingsAreDefinedByTheChecksModule() {
        // given
        SettingCatalog catalog = new SettingCatalog(
                List.of(new GeneralSettingDefinitions(DEFAULTS), new CheckSettingDefinitions(CHECK_DEFAULTS)));

        // when
        List<SettingDefinition> definitions = catalog.all();
        JavaClasses settingsClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.wakey.settings");

        // then
        assertThat(definitions)
                .extracting(definition -> definition.key())
                .contains(CheckSettingKeys.INTERVAL, CheckSettingKeys.TIMEOUT, CheckSettingKeys.RETRIES);
        noClasses()
                .that()
                .resideInAPackage("dev.wakey.settings..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("dev.wakey.checks..")
                .check(settingsClasses);
    }

    @Test
    void aNewModuleAddsASettingWithoutEditingSettings() {
        // given a module provider with a key the settings module never declares
        SettingKey key = new SettingKey("module.setting");
        SettingDefinitionProvider provider = () -> List.of(new SettingDefinition(
                key,
                SettingGroup.GENERAL,
                SettingType.STRING,
                null,
                null,
                null,
                null,
                value -> true,
                BoundDirection.MAX,
                plan -> null,
                () -> "value",
                null));
        SettingCatalog catalog = new SettingCatalog(List.of(new GeneralSettingDefinitions(DEFAULTS), provider));

        // when
        SettingDefinition definition = catalog.require("module.setting");

        // then
        assertThat(definition.key()).isEqualTo(key);
    }
}
