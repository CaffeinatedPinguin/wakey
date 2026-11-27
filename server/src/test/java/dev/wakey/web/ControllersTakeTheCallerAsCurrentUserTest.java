package dev.wakey.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.wakey.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

/** The caller is resolved once by the argument resolver, never by a controller method itself. */
class ControllersTakeTheCallerAsCurrentUserTest {

    private static final String AUTHENTICATION = Authentication.class.getName();

    private static final String ME_CONTROLLER = "dev.wakey.security.web.MeController";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("dev.wakey");

    private static final ArchCondition<JavaMethod> NOT_TAKE_AUTHENTICATION =
            new ArchCondition<>("not take Authentication as a parameter") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    for (JavaParameter parameter : method.getParameters()) {
                        String parameterType = parameter.getRawType().getName();
                        if (AUTHENTICATION.equals(parameterType)) {
                            String message = method.getFullName() + " takes Authentication; take CurrentUser instead.";
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                }
            };

    @Test
    void controllersTakeTheCallerAsCurrentUser() {
        // when
        methods()
                .that()
                .areDeclaredInClassesThat()
                .areAnnotatedWith(RestController.class)
                .should(NOT_TAKE_AUTHENTICATION)
                .check(CLASSES);
        noClasses()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .callMethod(CurrentUser.class, "from", Authentication.class)
                .check(CLASSES);

        // then
        JavaClass meController = CLASSES.get(ME_CONTROLLER);
        boolean takesCurrentUser = meController.getMethods().stream()
                .anyMatch(ControllersTakeTheCallerAsCurrentUserTest::takesCurrentUser);
        assertThat(takesCurrentUser).isTrue();
    }

    private static boolean takesCurrentUser(JavaMethod method) {
        if (!method.getName().equals("me")) {
            return false;
        }
        for (JavaClass parameter : method.getRawParameterTypes()) {
            if (parameter.isEquivalentTo(CurrentUser.class)) {
                return true;
            }
        }
        return false;
    }
}
