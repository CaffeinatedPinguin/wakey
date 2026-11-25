package dev.wakey.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainExceptionHandlerTest {

    private static final String ADVICE_ANNOTATION = "@RestControllerAdvice";

    private static final String ERRORS_SUFFIX = "Errors.java";

    private static final String DOMAIN_CATCH = "catch (Space";

    @Test
    void domainExceptionsAreMappedByOneAdvice() {
        // given
        List<Path> sources = apiSources();

        // when
        List<Path> advices = sources.stream()
                .filter(file -> contains(file, ADVICE_ANNOTATION))
                .toList();
        List<Path> errorMappers = sources.stream()
                .filter(file -> file.getFileName().toString().endsWith(ERRORS_SUFFIX))
                .filter(file -> contains(file, DOMAIN_CATCH))
                .toList();

        // then
        assertThat(advices).hasSize(1);
        String adviceFile = advices.getFirst().getFileName().toString();
        assertThat(adviceFile).isEqualTo("DomainExceptionHandler.java");
        assertThat(errorMappers).isEmpty();
    }

    private static List<Path> apiSources() {
        Path directory = apiSourceDirectory();
        try (var files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path apiSourceDirectory() {
        Path moduleRelative = Path.of("src", "main", "java", "dev", "wakey", "api");
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return Path.of("server", "src", "main", "java", "dev", "wakey", "api");
    }

    private static boolean contains(Path file, String text) {
        try {
            return Files.readString(file).contains(text);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
