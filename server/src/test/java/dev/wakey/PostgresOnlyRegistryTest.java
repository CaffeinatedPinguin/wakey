package dev.wakey;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every PostgreSQL-only place in the server sources must be listed in ADR 0008. */
class PostgresOnlyRegistryTest {

    private static final String MARKER = "postgres-only";

    private static final String REGISTRY = "docs/decisions/0008-postgresql-only.md";

    private static final String MAIN_SOURCES = "server/src/main";

    @Test
    void everyPostgresOnlyMarkerHasARowInTheRegistry() throws IOException {
        // given
        Path repositoryRoot = repositoryRoot();
        String registry = Files.readString(repositoryRoot.resolve(REGISTRY));
        List<String> markedFiles = markedFiles(repositoryRoot);

        // when
        List<String> missingRows =
                markedFiles.stream().filter(file -> !registry.contains(file)).toList();

        // then
        assertThat(missingRows)
                .as("files with a postgres-only marker must have a row in ADR 0008")
                .isEmpty();
    }

    private static List<String> markedFiles(Path repositoryRoot) throws IOException {
        Path sources = repositoryRoot.resolve(MAIN_SOURCES);
        try (Stream<Path> paths = Files.walk(sources)) {
            return paths.filter(Files::isRegularFile)
                    .filter(PostgresOnlyRegistryTest::containsMarker)
                    .map(path -> toRepositoryPath(repositoryRoot, path))
                    .sorted()
                    .toList();
        }
    }

    private static boolean containsMarker(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.contains(MARKER)) {
            return true;
        }
        try {
            String content = Files.readString(path);
            return content.contains(MARKER);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }

    private static String toRepositoryPath(Path repositoryRoot, Path path) {
        String relative = repositoryRoot.relativize(path).toString();
        return relative.replace('\\', '/');
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        Path marker = current.resolve(REGISTRY);
        if (Files.exists(marker)) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.exists(parent.resolve(REGISTRY))) {
            return parent;
        }
        throw new IllegalStateException("Cannot find " + REGISTRY + " from " + current);
    }
}
