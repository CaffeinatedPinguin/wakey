package dev.wakey;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import dev.wakey.web.CurrentUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ArchitectureTest {

    private static final String ROOT_PACKAGE = "dev.wakey";

    private static final String SHARED_ROLE = "shared";

    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";

    private static final String AUTHENTICATION = "org.springframework.security.core.Authentication";

    private static final String ME_CONTROLLER = "dev.wakey.security.web.MeController";

    /** The HTTP argument types of the spaces module are its public web contract. */
    private static final Set<String> PUBLIC_SUBPACKAGES = Set.of("dev.wakey.spaces.web");

    /** Every module lists the modules it may depend on; a new module or edge is a deliberate change. */
    private static final Map<String, Set<String>> MODULE_DEPENDENCIES = Map.ofEntries(
            dependencyEntry("audit"),
            dependencyEntry("checks", "audit", "settings", "spaces", "web"),
            dependencyEntry("config"),
            dependencyEntry("leadership"),
            dependencyEntry("messaging"),
            dependencyEntry("settings", "audit", "spaces", "web"),
            dependencyEntry("spaces", "audit", "web"),
            dependencyEntry("tokens", "audit", "spaces", "web"),
            dependencyEntry("web"),
            dependencyEntry("security", "web", "tokens", "spaces"),
            dependencyEntry("executor", "leadership", "messaging"),
            dependencyEntry("alerts", "leadership", "messaging"));

    /** Every module belongs to a role; modules of one role may depend only on that role and shared modules. */
    private static final Map<String, String> MODULE_ROLES = Map.ofEntries(
            Map.entry("audit", SHARED_ROLE),
            Map.entry("checks", SHARED_ROLE),
            Map.entry("config", SHARED_ROLE),
            Map.entry("leadership", SHARED_ROLE),
            Map.entry("messaging", SHARED_ROLE),
            Map.entry("settings", SHARED_ROLE),
            Map.entry("spaces", SHARED_ROLE),
            Map.entry("tokens", SHARED_ROLE),
            Map.entry("web", SHARED_ROLE),
            Map.entry("security", SHARED_ROLE),
            Map.entry("executor", "executor"),
            Map.entry("alerts", "alerts"));

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

    private record ModuleEdge(String module, JavaClass target) {}

    private static final ArchCondition<JavaClass> DEPEND_ONLY_ON_PUBLIC_MODULE_PACKAGES =
            new ArchCondition<>("depend on another module only through its main package") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (ModuleEdge edge : dependenciesOnOtherModules(javaClass)) {
                        JavaClass target = edge.target();
                        String targetModule = edge.module();
                        boolean publicSubpackage = PUBLIC_SUBPACKAGES.contains(target.getPackageName());
                        if (isSubpackageOf(target, targetModule) && !publicSubpackage) {
                            String message = javaClass.getName() + " depends on " + target.getName()
                                    + " in a private subpackage of module '" + targetModule
                                    + "'; depend only on its main package 'dev.wakey." + targetModule + "'.";
                            events.add(SimpleConditionEvent.violated(javaClass, message));
                        }
                    }
                }
            };

    private static final ArchCondition<JavaClass> DEPEND_ONLY_ON_DECLARED_MODULES =
            new ArchCondition<>("depend only on modules declared in MODULE_DEPENDENCIES") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    Optional<String> originModule = moduleOf(javaClass);
                    if (originModule.isEmpty()) {
                        return;
                    }
                    String module = originModule.get();
                    if (!MODULE_DEPENDENCIES.containsKey(module)) {
                        String message = "Module '" + module
                                + "' is not declared in MODULE_DEPENDENCIES; add the module to the dependency map.";
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                        return;
                    }
                    Set<String> allowedTargets = MODULE_DEPENDENCIES.get(module);
                    for (ModuleEdge edge : dependenciesOnOtherModules(javaClass)) {
                        if (!allowedTargets.contains(edge.module())) {
                            String message = javaClass.getName() + " in module '" + module + "' depends on module '"
                                    + edge.module()
                                    + "'; add the edge to MODULE_DEPENDENCIES only if it is intentional.";
                            events.add(SimpleConditionEvent.violated(javaClass, message));
                        }
                    }
                }
            };

    private static final ArchCondition<JavaClass> DEPEND_ONLY_ON_SAME_ROLE_OR_SHARED_MODULES =
            new ArchCondition<>("depend only on modules of the same role or shared modules") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    Optional<String> originModule = moduleOf(javaClass);
                    if (originModule.isEmpty()) {
                        return;
                    }
                    String module = originModule.get();
                    String role = MODULE_ROLES.get(module);
                    if (role == null) {
                        String message = "Module '" + module
                                + "' is not assigned in MODULE_ROLES; add it to a role or to the shared modules.";
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                        return;
                    }
                    for (ModuleEdge edge : dependenciesOnOtherModules(javaClass)) {
                        String targetRole = MODULE_ROLES.get(edge.module());
                        if (targetRole == null) {
                            String message = "Module '" + edge.module()
                                    + "' is not assigned in MODULE_ROLES; add it to a role or to the shared modules.";
                            events.add(SimpleConditionEvent.violated(javaClass, message));
                        } else if (!targetRole.equals(role) && !SHARED_ROLE.equals(targetRole)) {
                            String message = javaClass.getName() + " in role '" + role
                                    + "' must not depend on module '" + edge.module() + "' in role '" + targetRole
                                    + "'; communicate between roles through the messaging queue.";
                            events.add(SimpleConditionEvent.violated(javaClass, message));
                        }
                    }
                }
            };

    private static final ArchCondition<JavaMethod> DO_NOT_TAKE_AN_AUTHENTICATION =
            new ArchCondition<>("not take an Authentication parameter") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    for (JavaParameter parameter : method.getParameters()) {
                        String parameterType = parameter.getRawType().getName();
                        if (AUTHENTICATION.equals(parameterType)) {
                            String message = method.getFullName()
                                    + " takes an Authentication parameter; take the resolved actor instead.";
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                }
            };

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ROOT_PACKAGE);

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

    private static final ArchRule DEPEND_ON_OTHER_MODULES_ONLY_THROUGH_THEIR_MAIN_PACKAGES =
            classes().that().resideInAPackage("dev.wakey..").should(DEPEND_ONLY_ON_PUBLIC_MODULE_PACKAGES);

    private static final ArchRule DEPEND_ONLY_ON_MODULES_DECLARED_IN_THE_DEPENDENCY_MAP =
            classes().that().resideInAPackage("dev.wakey..").should(DEPEND_ONLY_ON_DECLARED_MODULES);

    private static final ArchRule DEPEND_ONLY_ON_MODULES_OF_THE_SAME_ROLE_OR_SHARED_MODULES =
            classes().that().resideInAPackage("dev.wakey..").should(DEPEND_ONLY_ON_SAME_ROLE_OR_SHARED_MODULES);

    private static final ArchRule NO_CONTROLLER_METHOD_TAKES_AN_AUTHENTICATION = methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(REST_CONTROLLER)
            .should(DO_NOT_TAKE_AN_AUTHENTICATION)
            .allowEmptyShould(true);

    private static final ArchRule NO_CONTROLLER_CALLS_THE_CURRENT_USER_FACTORY = noClasses()
            .that()
            .areAnnotatedWith(REST_CONTROLLER)
            .should()
            .callMethod(CurrentUser.class, "from", Authentication.class);

    private static final ArchRule NO_STANDARD_STREAMS = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    private static final ArchRule NO_GENERIC_EXCEPTIONS = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    private static final ArchRule NO_JAVA_UTIL_LOGGING = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    private static final ArchRule NO_FIELD_INJECTION = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    private static final ArchRule NO_DEPRECATED_API = GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;

    private static List<ModuleEdge> dependenciesOnOtherModules(JavaClass javaClass) {
        Optional<String> originModule = moduleOf(javaClass);
        List<ModuleEdge> edges = new ArrayList<>();
        for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
            JavaClass target = dependency.getTargetClass();
            Optional<String> targetModule = moduleOf(target);
            if (targetModule.isPresent() && !targetModule.equals(originModule)) {
                ModuleEdge edge = new ModuleEdge(targetModule.get(), target);
                edges.add(edge);
            }
        }
        return edges;
    }

    private static Map.Entry<String, Set<String>> dependencyEntry(String module, String... dependencies) {
        return Map.entry(module, Set.of(dependencies));
    }

    private static Set<String> productionModules() {
        Set<String> modules = new HashSet<>();
        for (JavaClass javaClass : CLASSES) {
            moduleOf(javaClass).ifPresent(modules::add);
        }
        return modules;
    }

    private static Optional<String> moduleOf(JavaClass javaClass) {
        String prefix = ROOT_PACKAGE + ".";
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return Optional.empty();
        }
        String belowRoot = packageName.substring(prefix.length());
        int endOfModule = belowRoot.indexOf('.');
        String module = endOfModule < 0 ? belowRoot : belowRoot.substring(0, endOfModule);
        return Optional.of(module);
    }

    private static boolean isSubpackageOf(JavaClass javaClass, String module) {
        return javaClass.getPackageName().startsWith(ROOT_PACKAGE + "." + module + ".");
    }

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

    @Test
    void dependsOnOtherModulesOnlyThroughTheirMainPackages() {
        DEPEND_ON_OTHER_MODULES_ONLY_THROUGH_THEIR_MAIN_PACKAGES.check(CLASSES);
    }

    @Test
    void dependsOnlyOnModulesDeclaredInTheDependencyMap() {
        DEPEND_ONLY_ON_MODULES_DECLARED_IN_THE_DEPENDENCY_MAP.check(CLASSES);
    }

    @Test
    void dependsOnlyOnModulesOfTheSameRoleOrSharedModules() {
        DEPEND_ONLY_ON_MODULES_OF_THE_SAME_ROLE_OR_SHARED_MODULES.check(CLASSES);
    }

    @Test
    void noControllerMethodTakesAnAuthenticationParameter() {
        // when
        NO_CONTROLLER_METHOD_TAKES_AN_AUTHENTICATION.check(CLASSES);
        NO_CONTROLLER_CALLS_THE_CURRENT_USER_FACTORY.check(CLASSES);

        // then
        JavaClass meController = CLASSES.get(ME_CONTROLLER);
        boolean takesCurrentUser = meController.getMethods().stream().anyMatch(ArchitectureTest::takesCurrentUser);
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

    @Test
    void everyModuleIsListedExplicitlyInBothMaps() {
        // when
        Set<String> modules = productionModules();

        // then
        assertThat(MODULE_DEPENDENCIES.keySet()).isEqualTo(MODULE_ROLES.keySet());
        assertThat(modules).isSubsetOf(MODULE_DEPENDENCIES.keySet());
    }
}
