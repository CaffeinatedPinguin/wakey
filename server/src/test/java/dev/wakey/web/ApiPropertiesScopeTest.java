package dev.wakey.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ApiPropertiesScopeTest {

    private static final String ROOT_PACKAGE = "dev.wakey";

    @Test
    void noModuleOutsideWebImportsApiProperties() {
        // given
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT_PACKAGE);

        // when
        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackages("dev.wakey.web", "dev.wakey.web..", "dev.wakey.*.web", "dev.wakey.*.web..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(ApiProperties.class.getName());

        // then
        rule.check(classes);
    }
}
