package dev.wakey;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Server rules from the review of the spaces pull request (ADR 0005, "Reguły z review warstwy
 * przestrzeni"): placement of controllers, operations without headers, errors as codes, ports, data
 * access, Spring beans, configuration properties and API DTOs.
 */
class ServerConventionsTest {

    private static final String ROOT_PACKAGE = "dev.wakey";

    /** The ports ADR 0005 allows with a single production implementation. */
    private static final Set<String> PORTS = Set.of(
            "dev.wakey.leadership.LeaderLease",
            "dev.wakey.messaging.MessageQueue",
            "CheckExecutor",
            "NotificationChannel",
            "RateLimiter");

    /** The modules that may use JdbcClient: hand-written SQL with locks and measurement results (ADR 0005). */
    private static final String[] JDBC_CLIENT_PACKAGES = {
        "dev.wakey.leadership..", "dev.wakey.messaging..", "dev.wakey.results.."
    };

    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";

    private static final String REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader";

    private static final String HTTP_HEADERS = "org.springframework.http.HttpHeaders";

    private static final String COMPONENT = "org.springframework.stereotype.Component";

    private static final String LOMBOK_GENERATED = "lombok.Generated";

    private static final Set<String> INJECTION_QUALIFIERS = Set.of(
            "org.springframework.beans.factory.annotation.Qualifier",
            "org.springframework.beans.factory.annotation.Value");

    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";

    private static final ArchCondition<JavaMethod> NOT_TAKE_HEADERS =
            new ArchCondition<>("not take request headers as parameters") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    for (JavaParameter parameter : method.getParameters()) {
                        String type = parameter.getRawType().getName();
                        boolean headers = HTTP_HEADERS.equals(type) || parameter.isAnnotatedWith(REQUEST_HEADER);
                        if (headers) {
                            String message = method.getFullName()
                                    + " reads request headers; take the input from the path, query or body.";
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                }
            };

    private static final ArchCondition<JavaClass> BE_A_PORT_OR_HAVE_TWO_IMPLEMENTATIONS =
            new ArchCondition<>("be a port from ADR 0005 or have at least two implementations") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean port = PORTS.contains(javaClass.getName()) || PORTS.contains(javaClass.getSimpleName());
                    boolean repository = javaClass.isAssignableTo(SPRING_DATA_REPOSITORY);
                    int implementations = javaClass.getSubclasses().size();
                    if (!port && !repository && implementations < 2) {
                        String message = javaClass.getName() + " is an interface with " + implementations
                                + " implementation(s); use the class directly or add it to the ports in ADR 0005.";
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    }
                }
            };

    private static final ArchCondition<JavaClass> GET_DEPENDENCIES_FROM_A_LOMBOK_CONSTRUCTOR =
            new ArchCondition<>("get dependencies from a @RequiredArgsConstructor") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (JavaConstructor constructor : javaClass.getConstructors()) {
                        boolean handWritten = !constructor.getParameters().isEmpty()
                                && !constructor.isAnnotatedWith(LOMBOK_GENERATED)
                                && !hasQualifiedParameter(constructor);
                        if (handWritten) {
                            String message = constructor.getFullName()
                                    + " is hand-written; use @RequiredArgsConstructor (or a @Qualifier/@Value parameter).";
                            events.add(SimpleConditionEvent.violated(javaClass, message));
                        }
                    }
                }
            };

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ROOT_PACKAGE);

    private static final ArchRule CONTROLLERS_LIVE_IN_A_WEB_SUBPACKAGE = classes()
            .that()
            .areAnnotatedWith(REST_CONTROLLER)
            .should()
            .resideInAPackage("dev.wakey.*.web")
            .because("a module keeps its HTTP adapter in a private web subpackage (ADR 0005)");

    private static final ArchRule OPERATIONS_DO_NOT_READ_HEADERS = methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(REST_CONTROLLER)
            .should(NOT_TAKE_HEADERS)
            .allowEmptyShould(true);

    private static final ArchRule EXCEPTIONS_LIVE_IN_WEB = classes()
            .that()
            .resideInAPackage("dev.wakey..")
            .and()
            .areAssignableTo(Throwable.class)
            .should()
            .resideInAPackage("dev.wakey.web..")
            .because("a module throws ApiException with its ErrorCode enum instead of its own exception classes")
            .allowEmptyShould(true);

    private static final ArchRule ERROR_CODES_ARE_ENUMS = classes()
            .that()
            .implement("dev.wakey.web.ErrorCode")
            .should()
            .beEnums()
            .allowEmptyShould(true);

    private static final ArchRule INTERFACES_ARE_PORTS = classes()
            .that()
            .resideInAPackage("dev.wakey..")
            .and()
            .areInterfaces()
            .and()
            .areNotAnnotations()
            .and()
            .doNotHaveSimpleName("package-info")
            .should(BE_A_PORT_OR_HAVE_TWO_IMPLEMENTATIONS)
            .allowEmptyShould(true);

    private static final ArchRule JDBC_CLIENT_ONLY_IN_SQL_MODULES = noClasses()
            .that()
            .resideOutsideOfPackages(JDBC_CLIENT_PACKAGES)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.jdbc.core.simple.JdbcClient")
            .because("aggregates are Spring Data JDBC repositories (ADR 0005)");

    private static final ArchRule BEANS_USE_LOMBOK_CONSTRUCTORS = classes()
            .that()
            .resideInAPackage("dev.wakey..")
            .and()
            .areMetaAnnotatedWith(COMPONENT)
            .should(GET_DEPENDENCIES_FROM_A_LOMBOK_CONSTRUCTOR);

    private static final ArchRule CONFIGURATION_PROPERTIES_ARE_NOT_HTTP = noClasses()
            .that()
            .areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.http..", "org.springframework.web..", "dev.wakey.web..")
            .because("configuration holds values; validation errors of a request belong to the web layer");

    private static final ArchRule API_SCHEMAS_ONLY_ON_WEB_DTOS = noClasses()
            .that()
            .resideOutsideOfPackages("dev.wakey.web..", "dev.wakey.*.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("io.swagger.v3.oas.annotations..")
            .because("API DTOs are separate records in the web layer (ADR 0006)");

    private static boolean hasQualifiedParameter(JavaConstructor constructor) {
        for (JavaParameter parameter : constructor.getParameters()) {
            for (String qualifier : INJECTION_QUALIFIERS) {
                if (parameter.isAnnotatedWith(qualifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void controllersLiveInAWebSubpackage() {
        CONTROLLERS_LIVE_IN_A_WEB_SUBPACKAGE.check(CLASSES);
    }

    @Test
    void operationsDoNotReadHeaders() {
        OPERATIONS_DO_NOT_READ_HEADERS.check(CLASSES);
    }

    @Test
    void exceptionsLiveInWeb() {
        EXCEPTIONS_LIVE_IN_WEB.check(CLASSES);
    }

    @Test
    void errorCodesAreEnums() {
        ERROR_CODES_ARE_ENUMS.check(CLASSES);
    }

    @Test
    void interfacesArePorts() {
        INTERFACES_ARE_PORTS.check(CLASSES);
    }

    @Test
    void jdbcClientOnlyInSqlModules() {
        JDBC_CLIENT_ONLY_IN_SQL_MODULES.check(CLASSES);
    }

    @Test
    void beansUseLombokConstructors() {
        BEANS_USE_LOMBOK_CONSTRUCTORS.check(CLASSES);
    }

    @Test
    void configurationPropertiesAreNotHttp() {
        CONFIGURATION_PROPERTIES_ARE_NOT_HTTP.check(CLASSES);
    }

    @Test
    void apiSchemasOnlyOnWebDtos() {
        API_SCHEMAS_ONLY_ON_WEB_DTOS.check(CLASSES);
    }
}
