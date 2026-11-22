package dev.wakey;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final Set<String> WALL_CLOCK_TYPES = Set.of(
            "java.time.Instant",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.OffsetDateTime",
            "java.time.ZonedDateTime",
            "java.time.Year",
            "java.time.YearMonth");

    private static final DescribedPredicate<JavaMethodCall> CALLS_WALL_CLOCK =
            new DescribedPredicate<>("call a wall-clock method") {
                @Override
                public boolean test(JavaMethodCall call) {
                    AccessTarget.MethodCallTarget target = call.getTarget();
                    String owner = target.getOwner().getName();
                    boolean noArgumentNow = target.getName().equals("now")
                            && target.getRawParameterTypes().isEmpty()
                            && WALL_CLOCK_TYPES.contains(owner);
                    boolean fixedClock = owner.equals("java.time.Clock")
                            && (target.getName().equals("systemUTC")
                                    || target.getName().equals("systemDefaultZone"));
                    return noArgumentNow || fixedClock;
                }
            };

    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("dev.wakey");

    private static final ArchRule NO_JAVA_NET_HTTP = noClasses()
            .that()
            .resideInAPackage("dev.wakey..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("java.net.http..");

    private static final ArchRule NO_WALL_CLOCK_OUTSIDE_CLOCK_CONFIGURATION = noClasses()
            .that()
            .resideInAPackage("dev.wakey..")
            .and()
            .doNotHaveSimpleName("ClockConfiguration")
            .should()
            .callMethodWhere(CALLS_WALL_CLOCK);

    private static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .allowEmptyShould(true);

    private static final ArchRule NO_CYCLES_BETWEEN_FIRST_LEVEL_PACKAGES =
            slices().matching("dev.wakey.(*)..").should().beFreeOfCycles();

    private static final ArchRule NO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    private static final ArchRule NO_GENERIC_EXCEPTIONS = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    private static final ArchRule NO_JAVA_UTIL_LOGGING = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    private static final ArchRule NO_FIELD_INJECTION = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    private static final ArchRule NO_DEPRECATED_API = GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;

    @Test
    void doesNotAccessStandardStreams() {
        NO_STANDARD_STREAMS.check(CLASSES);
    }

    @Test
    void doesNotThrowGenericExceptions() {
        NO_GENERIC_EXCEPTIONS.check(CLASSES);
    }

    @Test
    void doesNotUseJavaUtilLogging() {
        NO_JAVA_UTIL_LOGGING.check(CLASSES);
    }

    @Test
    void doesNotUseFieldInjection() {
        NO_FIELD_INJECTION.check(CLASSES);
    }

    @Test
    void doesNotUseDeprecatedApi() {
        NO_DEPRECATED_API.check(CLASSES);
    }

    @Test
    void doesNotUseJavaNetHttp() {
        NO_JAVA_NET_HTTP.check(CLASSES);
    }

    @Test
    void doesNotReadTheWallClockOutsideTheClockConfiguration() {
        NO_WALL_CLOCK_OUTSIDE_CLOCK_CONFIGURATION.check(CLASSES);
    }

    @Test
    void domainDoesNotDependOnSpring() {
        DOMAIN_IS_FRAMEWORK_FREE.check(CLASSES);
    }

    @Test
    void doesNotHaveCyclesBetweenFirstLevelPackages() {
        NO_CYCLES_BETWEEN_FIRST_LEVEL_PACKAGES.check(CLASSES);
    }
}
