package dev.wakey.checks;

import java.util.List;

/** Test-only access to the package-private registry constructor. */
public final class CheckExecutorsFixture {

    private CheckExecutorsFixture() {}

    public static CheckExecutors of(List<CheckExecutor> executors) {
        return new CheckExecutors(executors);
    }
}
