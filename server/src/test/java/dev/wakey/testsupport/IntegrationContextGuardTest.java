package dev.wakey.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Keeps the integration suite on the shared Spring context. Only the classes below may customise
 * the context, because they need a value or a bean that the whole run cannot share; any other test
 * with its own property source, import or dynamic property source must move its data to the shared
 * test profile.
 */
class IntegrationContextGuardTest {

    private static final String ROOT_PACKAGE = "dev.wakey";

    private static final Set<String> ALLOWED_CONTEXT_CLASSES = Set.of(
            "dev.wakey.checks.internal.CheckQueryCountTest",
            "dev.wakey.security.SessionMaxAgeIntegrationTest",
            "dev.wakey.security.OidcProviderIntegrationTest",
            "dev.wakey.spaces.SpaceListQueryCountTest",
            "dev.wakey.spaces.SpaceRoleTransactionTest",
            "dev.wakey.tokens.MemberRemovalRollbackIntegrationTest",
            "dev.wakey.tokens.internal.TokenQueryCountTest",
            "dev.wakey.spaces.web.DefaultPageSizeIntegrationTest",
            "dev.wakey.web.OpenApiDisabledTest");

    @Test
    void integrationTestsShareOneSpringContextExceptForTheGlobalOnes() {
        // when
        JavaClasses classes = new ClassFileImporter().importPackages(ROOT_PACKAGE);
        List<String> ownContexts = classes.stream()
                .filter(javaClass -> javaClass.isAssignableTo(PostgresIntegrationTest.class.getName()))
                .filter(javaClass -> !javaClass.getModifiers().contains(JavaModifier.ABSTRACT))
                .filter(IntegrationContextGuardTest::hasOwnContext)
                .map(JavaClass::getName)
                .sorted()
                .toList();

        // then
        assertThat(ownContexts).containsExactlyInAnyOrderElementsOf(ALLOWED_CONTEXT_CLASSES);
    }

    private static boolean hasOwnContext(JavaClass javaClass) {
        return javaClass.isAnnotatedWith(TestPropertySource.class.getName())
                || javaClass.isAnnotatedWith(Import.class.getName())
                || javaClass.isAnnotatedWith(ActiveProfiles.class.getName())
                || javaClass.getMethods().stream()
                        .anyMatch(method -> method.isAnnotatedWith(DynamicPropertySource.class.getName()));
    }
}
