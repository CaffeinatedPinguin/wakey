package dev.wakey.settings.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import dev.wakey.settings.SpaceSettings;
import dev.wakey.spaces.SpaceView;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SettingsAdaptersTest {

    private static final Set<String> ADAPTER_METHODS = Set.of("effective", "effectiveAll", "set", "reset");

    @Test
    void settingsAdaptersNeverPassAPlanName() {
        // given
        JavaClasses adapterClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.wakey.settings.web");

        // when
        List<Method> settingsMethods = List.of(SpaceSettings.class.getMethods());
        List<Method> adapterMethods = settingsMethods.stream()
                .filter(method -> ADAPTER_METHODS.contains(method.getName()))
                .toList();

        // then the settings use cases resolve the plan themselves, so no adapter overload takes one
        noClasses()
                .that()
                .resideInAPackage("dev.wakey.settings.web..")
                .should()
                .callMethod(SpaceView.class, "planName")
                .check(adapterClasses);
        assertThat(adapterMethods).hasSize(ADAPTER_METHODS.size());
        assertThat(parameterCounts(adapterMethods)).containsExactlyInAnyOrder(2, 1, 3, 3);
    }

    private static List<Integer> parameterCounts(List<Method> methods) {
        return methods.stream().map(Method::getParameterCount).toList();
    }
}
