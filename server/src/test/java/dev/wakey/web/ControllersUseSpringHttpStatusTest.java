package dev.wakey.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP status codes come from Spring constants, never from numeric literals or a repeated constant
 * (review #4697195992).
 */
class ControllersUseSpringHttpStatusTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("dev.wakey");

    private static final DescribedPredicate<JavaMethodCall> CALLS_STATUS_WITH_AN_INT =
            new DescribedPredicate<>("call ResponseEntity.status(int)") {
                @Override
                public boolean test(JavaMethodCall call) {
                    AccessTarget.MethodCallTarget target = call.getTarget();
                    boolean responseEntity = target.getOwner().getName().equals(ResponseEntity.class.getName());
                    return responseEntity
                            && target.getName().equals("status")
                            && target.getRawParameterTypes().size() == 1
                            && target.getRawParameterTypes().get(0).getName().equals("int");
                }
            };

    private static final ArchCondition<JavaClass> NOT_DECLARE_STATIC_INT_CONSTANTS =
            new ArchCondition<>("not declare a static int constant") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (JavaField field : javaClass.getFields()) {
                        boolean isStatic = field.getModifiers().contains(JavaModifier.STATIC);
                        boolean isInt = field.getRawType().isPrimitive()
                                && field.getRawType().getName().equals("int");
                        if (isStatic && isInt) {
                            String message = field.getFullName()
                                    + " is a static int constant; use a Spring HttpStatus constant instead.";
                            events.add(SimpleConditionEvent.violated(field, message));
                        }
                    }
                }
            };

    @Test
    void controllersDoNotHardCodeHttpStatusCodes() {
        // when
        noClasses()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .callMethodWhere(CALLS_STATUS_WITH_AN_INT)
                .check(CLASSES);
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should(NOT_DECLARE_STATIC_INT_CONSTANTS)
                .check(CLASSES);

        // then
        // the rules above reject a numeric status code and a repeated Spring status constant.
    }
}
