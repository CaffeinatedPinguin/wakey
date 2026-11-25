package dev.wakey.testsupport;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates the committed public OpenAPI contract from the module or the repository root. */
public final class ContractFile {

    private static final String CONTRACT = "public.json";

    private ContractFile() {}

    public static Path path() {
        Path moduleRelative = Path.of("..", "api", "openapi", CONTRACT);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Path.of("api", "openapi", CONTRACT);
    }
}
