package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import dev.wakey.spaces.SpaceError;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.web.CommonError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Review rules from the spaces pull request: closed sets use enums and no own server identifier
 * mentions the RFC 9457 wire word instead of the domain word "exception".
 */
class ExceptionNamingTest {

    private static final String ROOT_PACKAGE = "dev.wakey";

    private static final String WIRE_WORD = "problem";

    private static final String TEST_SOURCES = "server/src/test/java";

    private static final List<String> WIRE_FORMAT_TESTS = List.of("WireFormatTest.java", "ContractTest.java");

    @Test
    void noServerIdentifierMentionsProblem() {
        // given
        JavaClasses classes = mainClasses();
        List<String> offenders = new ArrayList<>();

        // when
        for (JavaClass javaClass : classes) {
            collectIdentifier(offenders, javaClass.getName());
            String owner = javaClass.getName();
            for (JavaField field : javaClass.getFields()) {
                collectIdentifier(offenders, owner + "#" + field.getName());
            }
            for (JavaMethod method : javaClass.getMethods()) {
                collectIdentifier(offenders, owner + "#" + method.getName());
            }
        }

        // then
        assertThat(offenders).isEmpty();
    }

    @Test
    void testsUseTheErrorEnumsInsteadOfLiterals() throws IOException {
        // given
        Path root = repositoryRoot();
        List<String> literals = forbiddenLiterals();
        List<String> offenders = new ArrayList<>();

        // when
        collectLiteralOffenders(root, literals, offenders);

        // then
        assertThat(offenders).isEmpty();
    }

    private static JavaClasses mainClasses() {
        return new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(ROOT_PACKAGE);
    }

    private static void collectIdentifier(List<String> offenders, String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        if (normalized.contains(WIRE_WORD)) {
            offenders.add(identifier);
        }
    }

    private static List<String> forbiddenLiterals() {
        List<String> literals = new ArrayList<>();
        for (CommonError error : CommonError.values()) {
            literals.add(error.getCode());
        }
        for (SpaceError error : SpaceError.values()) {
            literals.add(error.getCode());
        }
        for (SpaceRole role : SpaceRole.values()) {
            literals.add(role.name());
        }
        return literals;
    }

    private static void collectLiteralOffenders(Path root, List<String> literals, List<String> offenders)
            throws IOException {
        Path sources = root.resolve(TEST_SOURCES);
        try (Stream<Path> paths = Files.walk(sources)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(ExceptionNamingTest::isScannedTest)
                    .toList();
            for (Path file : files) {
                collectFileOffenders(root, file, literals, offenders);
            }
        }
    }

    private static void collectFileOffenders(Path root, Path file, List<String> literals, List<String> offenders)
            throws IOException {
        String content = Files.readString(file);
        for (String literal : literals) {
            String quoted = "\"" + literal + "\"";
            if (content.contains(quoted)) {
                String relative = root.relativize(file).toString();
                offenders.add(relative + " contains the literal " + literal);
            }
        }
    }

    private static boolean isScannedTest(Path file) {
        String name = file.getFileName().toString();
        boolean javaSource = name.endsWith(".java");
        boolean wireFormatTest = WIRE_FORMAT_TESTS.stream().anyMatch(name::endsWith);
        return javaSource && !wireFormatTest;
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = current.resolve(TEST_SOURCES);
        if (Files.exists(marker)) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.exists(parent.resolve(TEST_SOURCES))) {
            return parent;
        }
        throw new IllegalStateException("Cannot find " + TEST_SOURCES + " from " + current);
    }
}
