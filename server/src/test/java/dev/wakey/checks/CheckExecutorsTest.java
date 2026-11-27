package dev.wakey.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckExecutorsTest {

    @Test
    void everyCheckTypeHasOneExecutor() {
        // given
        List<CheckExecutor> executors = new ArrayList<>();
        for (CheckType type : CheckType.values()) {
            executors.add(new FakeExecutor(type));
        }
        CheckExecutors registry = new CheckExecutors(executors);

        // when
        List<CheckType> resolved = new ArrayList<>();
        for (CheckType type : CheckType.values()) {
            CheckExecutor executor = registry.executorFor(type);
            resolved.add(executor.type());
        }

        // then
        assertThat(resolved).containsExactly(CheckType.values());
    }

    @Test
    void aSecondExecutorOfTheSameTypeIsRejected() {
        // given
        CheckExecutor first = new FakeExecutor(CheckType.HTTP);
        CheckExecutor second = new FakeExecutor(CheckType.HTTP);

        // when
        // then
        assertThatThrownBy(() -> new CheckExecutors(List.of(first, second))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aMissingExecutorIsReportedWithItsType() {
        // given
        CheckExecutors registry = new CheckExecutors(List.of(new FakeExecutor(CheckType.HTTP)));

        // when
        // then
        assertThatThrownBy(() -> registry.executorFor(CheckType.TCP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TCP");
    }

    private record FakeExecutor(CheckType type) implements CheckExecutor {

        @Override
        public CheckResult execute(Check check) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CheckFormDescriptor formDescriptor() {
            return new CheckFormDescriptor(type, List.of());
        }
    }
}
