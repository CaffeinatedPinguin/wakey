package dev.wakey;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String ROOT_PACKAGE = "dev.wakey";

    private static final String SHARED_ROLE = "shared";

    /** Every module lists the modules it may depend on; a new module or edge is a deliberate change. */
    private static final Map<String, Set<String>> MODULE_DEPENDENCIES = moduleDependencies();

    /** Every module belongs to a role; modules of one role may depend only on that role and shared modules. */
    private static final Map<String, String> MODULE_ROLES = moduleRoles();

    private static Map<String, Set<String>> moduleDependencies() {
        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("api", Set.of("checks", "spaces", "web", "security"));
        dependencies.put("audit", Set.of());
        dependencies.put("checks", Set.of());
        dependencies.put("config", Set.of());
        dependencies.put("leadership", Set.of());
        dependencies.put("messaging", Set.of());
        dependencies.put("ratelimit", Set.of());
        dependencies.put("retry", Set.of());
        dependencies.put("settings", Set.of());
        dependencies.put("spaces", Set.of());
        dependencies.put("tokens", Set.of("spaces"));
        dependencies.put("web", Set.of());
        dependencies.put("security", Set.of("web"));
        dependencies.put("executor", Set.of("leadership", "messaging"));
        dependencies.put("alerts", Set.of("leadership", "messaging"));
        return dependencies;
    }

    private static Map<String, String> moduleRoles() {
        Map<String, String> roles = new HashMap<>();
        for (String module : MODULE_DEPENDENCIES.keySet()) {
            roles.put(module, SHARED_ROLE);
        }
        roles.put("executor", "executor");
        roles.put("alerts", "alerts");
        return roles;
    }

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
                        if (isSubpackageOf(target, targetModule)) {
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
}
