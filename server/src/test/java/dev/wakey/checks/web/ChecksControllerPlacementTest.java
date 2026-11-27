package dev.wakey.checks.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ChecksControllerPlacementTest {

    @Test
    void checksHaveOneController() {
        // given
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.wakey.checks.web");
        List<String> controllers = new ArrayList<>();
        for (JavaClass javaClass : classes) {
            if (javaClass.isAnnotatedWith(RestController.class)) {
                controllers.add(javaClass.getName());
            }
        }
        Set<String> operations = operationNames(ChecksController.class);

        // when
        // then
        assertThat(controllers).containsExactly(ChecksController.class.getName());
        assertThat(operations)
                .containsExactlyInAnyOrder(
                        "listChecks",
                        "countChecks",
                        "getCheck",
                        "createCheck",
                        "updateCheck",
                        "deleteCheck",
                        "pauseCheck",
                        "resumeCheck",
                        "bulkChecks");
    }

    @Test
    void checksDomainDoesNotDependOnTheSpacesWebAdapter() {
        // given
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("dev.wakey.checks");

        // when
        // then
        noClasses()
                .that()
                .resideInAPackage("dev.wakey.checks..")
                .and()
                .resideOutsideOfPackage("dev.wakey.checks.web..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("dev.wakey.spaces.web..")
                .check(classes);
    }

    private static Set<String> operationNames(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(RequestMapping.class))
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}
