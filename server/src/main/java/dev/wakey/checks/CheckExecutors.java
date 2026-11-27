package dev.wakey.checks;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The executors of all check types, one for each type. The map is built once from the beans, so a
 * duplicate or a missing executor for a requested type is reported with the type in the message.
 */
public final class CheckExecutors {

    private final Map<CheckType, CheckExecutor> executors;

    CheckExecutors(List<CheckExecutor> executors) {
        Map<CheckType, CheckExecutor> byType = new EnumMap<>(CheckType.class);
        for (CheckExecutor executor : executors) {
            CheckExecutor previous = byType.put(executor.type(), executor);
            if (previous != null) {
                throw new IllegalStateException("Two CheckExecutors registered for " + executor.type());
            }
        }
        this.executors = Map.copyOf(byType);
    }

    /** Returns the executor of the type or fails loudly when no bean handles it. */
    public CheckExecutor executorFor(CheckType type) {
        CheckExecutor executor = executors.get(type);
        if (executor == null) {
            throw new IllegalStateException("No CheckExecutor registered for " + type);
        }
        return executor;
    }
}

@Configuration(proxyBeanMethods = false)
class CheckExecutorsConfiguration {

    @Bean
    CheckExecutors checkExecutors(List<CheckExecutor> executors) {
        return new CheckExecutors(executors);
    }
}
