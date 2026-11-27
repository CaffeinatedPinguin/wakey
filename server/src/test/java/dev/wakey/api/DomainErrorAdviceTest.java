package dev.wakey.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainErrorAdviceTest {

    private static final String ADVICE_ANNOTATION = "@RestControllerAdvice";

    private static final String EXCEPTION_SUFFIX = "Exception.java";

    private static final Path WEB_PACKAGE = Path.of("web");

    @Test
    void moduleErrorsAreRenderedByTheSingleWebAdvice() {
        // given
        List<Path> sources = mainSources();

        // when
        List<Path> advices = sources.stream()
                .filter(file -> contains(file, ADVICE_ANNOTATION))
                .toList();
        Path root = mainSourceDirectory();
        List<String> moduleExceptions = sources.stream()
                .map(root::relativize)
                .filter(path -> path.getFileName().toString().endsWith(EXCEPTION_SUFFIX))
                .filter(path -> !path.startsWith(WEB_PACKAGE))
                .map(Path::toString)
                .toList();

        // then
        assertThat(advices).hasSize(1);
        String adviceFile = advices.getFirst().getFileName().toString();
        assertThat(adviceFile).isEqualTo("ApiExceptionHandler.java");
        assertThat(moduleExceptions).isEmpty();
    }

    private static List<Path> mainSources() {
        Path directory = mainSourceDirectory();
        try (var files = Files.walk(directory)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path mainSourceDirectory() {
        Path moduleRelative = Path.of("src", "main", "java", "dev", "wakey");
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return Path.of("server", "src", "main", "java", "dev", "wakey");
    }

    private static boolean contains(Path file, String text) {
        try {
            return Files.readString(file).contains(text);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
