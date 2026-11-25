package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.InvalidCheckException;
import dev.wakey.checks.PlanLimits;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class CheckValidatorTest {

    private static final PlanLimits UNLIMITED = new PlanLimits(null, null, null, null, "default");

    @Test
    void httpTargetMustBeAnHttpOrHttpsUrl() {
        // given
        CheckDraft fileTarget = draft(CheckType.HTTP, "file:///etc/passwd", null);
        CheckDraft gopherTarget = draft(CheckType.HTTP, "gopher://example.test", null);
        CheckDraft httpTarget = draft(CheckType.HTTP, "https://example.test/health", null);

        // when
        Throwable fileRejected = catchThrowable(() -> CheckValidator.validate(fileTarget, UNLIMITED));
        Throwable gopherRejected = catchThrowable(() -> CheckValidator.validate(gopherTarget, UNLIMITED));
        CheckDraft accepted = CheckValidator.validate(httpTarget, UNLIMITED);

        // then
        assertThat(fileRejected).isInstanceOf(InvalidCheckException.class);
        assertThat(gopherRejected).isInstanceOf(InvalidCheckException.class);
        assertThat(((InvalidCheckException) fileRejected).field()).isEqualTo("target");
        assertThat(accepted.target()).isEqualTo("https://example.test/health");
    }

    @Test
    void tcpTargetMustBeHostAndPort() {
        // given
        CheckDraft url = draft(CheckType.TCP, "https://db.example.test", null);
        CheckDraft noPort = draft(CheckType.TCP, "db.example.test", null);
        CheckDraft zeroPort = draft(CheckType.TCP, "db.example.test:0", null);
        CheckDraft highPort = draft(CheckType.TCP, "db.example.test:65536", null);
        CheckDraft valid = draft(CheckType.TCP, "db.example.test:5432", null);

        // when
        Throwable urlRejected = catchThrowable(() -> CheckValidator.validate(url, UNLIMITED));
        Throwable noPortRejected = catchThrowable(() -> CheckValidator.validate(noPort, UNLIMITED));
        Throwable zeroPortRejected = catchThrowable(() -> CheckValidator.validate(zeroPort, UNLIMITED));
        Throwable highPortRejected = catchThrowable(() -> CheckValidator.validate(highPort, UNLIMITED));
        CheckDraft accepted = CheckValidator.validate(valid, UNLIMITED);

        // then
        assertThat(urlRejected).isInstanceOf(InvalidCheckException.class);
        assertThat(noPortRejected).isInstanceOf(InvalidCheckException.class);
        assertThat(zeroPortRejected).isInstanceOf(InvalidCheckException.class);
        assertThat(highPortRejected).isInstanceOf(InvalidCheckException.class);
        assertThat(accepted.target()).isEqualTo("db.example.test:5432");
    }

    @Test
    void runbookUrlMustBeHttpOrHttps() {
        // given
        CheckDraft scriptRunbook = draft(CheckType.HTTP, "https://example.test", "javascript:alert(1)");
        CheckDraft validRunbook = draft(CheckType.HTTP, "https://example.test", "https://runbook.example.test/x");

        // when
        Throwable rejected = catchThrowable(() -> CheckValidator.validate(scriptRunbook, UNLIMITED));
        CheckDraft accepted = CheckValidator.validate(validRunbook, UNLIMITED);

        // then
        assertThat(rejected).isInstanceOf(InvalidCheckException.class);
        assertThat(((InvalidCheckException) rejected).field()).isEqualTo("runbookUrl");
        assertThat(accepted.runbookUrl()).isEqualTo("https://runbook.example.test/x");
    }

    private static CheckDraft draft(CheckType type, String target, @Nullable String runbookUrl) {
        return new CheckDraft("check", type, target, 60, 10, 1, null, null, runbookUrl, List.of());
    }
}
