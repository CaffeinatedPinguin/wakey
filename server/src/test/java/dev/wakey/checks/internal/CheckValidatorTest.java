package dev.wakey.checks.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.checks.CheckDraft;
import dev.wakey.checks.CheckError;
import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckType;
import dev.wakey.spaces.SpaceError;
import dev.wakey.web.ApiException;
import dev.wakey.web.ApiFieldError;
import dev.wakey.web.CommonError;
import dev.wakey.web.ErrorCode;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class CheckValidatorTest {

    @Test
    void httpTargetMustBeAnHttpOrHttpsUrl() {
        // given
        CheckDraft fileTarget = draft(CheckType.HTTP, "file:///etc/passwd", null);
        CheckDraft gopherTarget = draft(CheckType.HTTP, "gopher://example.test", null);
        CheckDraft httpTarget = draft(CheckType.HTTP, "https://example.test/health", null);

        // when
        Throwable fileRejected = catchThrowable(() -> CheckValidator.validate(fileTarget));
        Throwable gopherRejected = catchThrowable(() -> CheckValidator.validate(gopherTarget));
        CheckDraft accepted = CheckValidator.validate(httpTarget);

        // then
        assertInvalid(fileRejected, "/target", CheckError.INVALID_TARGET);
        assertInvalid(gopherRejected, "/target", CheckError.INVALID_TARGET);
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
        Throwable urlRejected = catchThrowable(() -> CheckValidator.validate(url));
        Throwable noPortRejected = catchThrowable(() -> CheckValidator.validate(noPort));
        Throwable zeroPortRejected = catchThrowable(() -> CheckValidator.validate(zeroPort));
        Throwable highPortRejected = catchThrowable(() -> CheckValidator.validate(highPort));
        CheckDraft accepted = CheckValidator.validate(valid);

        // then
        assertInvalid(urlRejected, "/target", CheckError.INVALID_TARGET);
        assertInvalid(noPortRejected, "/target", CheckError.INVALID_TARGET);
        assertInvalid(zeroPortRejected, "/target", CheckError.INVALID_TARGET);
        assertInvalid(highPortRejected, "/target", CheckError.INVALID_TARGET);
        assertThat(accepted.target()).isEqualTo("db.example.test:5432");
    }

    @Test
    void anIpv6TargetWithAPortIsAccepted() {
        // given
        CheckDraft check = draft("ipv6", CheckType.TCP, "[2001:db8::1]:443");

        // when
        CheckDraft accepted = CheckValidator.validate(check);

        // then
        assertThat(accepted.target()).isEqualTo("[2001:db8::1]:443");
    }

    @Test
    void anIpv6LoopbackWithAPortIsAValidTcpTarget() {
        // given
        CheckDraft check = draft("ipv6 loopback", CheckType.TCP, "[::1]:443");

        // when
        CheckDraft accepted = CheckValidator.validate(check);

        // then
        assertThat(accepted.target()).isEqualTo("[::1]:443");
    }

    @Test
    void anIpv6CertificateTargetWithoutAPortIsAccepted() {
        // given
        CheckDraft check = draft("ipv6 certificate", CheckType.CERTIFICATE, "[::1]");

        // when
        CheckDraft accepted = CheckValidator.validate(check);

        // then
        assertThat(accepted.target()).isEqualTo("[::1]");
    }

    @Test
    void aTargetWithCredentialsOrAPathIsRejected() {
        // given
        CheckDraft credentials = draft("credentials", CheckType.TCP, "user@db.example.test:5432");
        CheckDraft path = draft("path", CheckType.TCP, "db.example.test:5432/health");

        // when
        Throwable credentialsRejected = catchThrowable(() -> CheckValidator.validate(credentials));
        Throwable pathRejected = catchThrowable(() -> CheckValidator.validate(path));

        // then
        assertInvalid(credentialsRejected, "/target", CheckError.INVALID_TARGET);
        assertInvalid(pathRejected, "/target", CheckError.INVALID_TARGET);
    }

    @Test
    void aTcpTargetWithUserInfoIsRejected() {
        // given
        CheckDraft check = draft("user info", CheckType.TCP, "user@host:1");

        // when
        Throwable rejected = catchThrowable(() -> CheckValidator.validate(check));

        // then
        assertInvalid(rejected, "/target", CheckError.INVALID_TARGET);
    }

    @Test
    void aTcpTargetWithAPathIsRejected() {
        // given
        CheckDraft check = draft("path", CheckType.TCP, "host:1/path");

        // when
        Throwable rejected = catchThrowable(() -> CheckValidator.validate(check));

        // then
        assertInvalid(rejected, "/target", CheckError.INVALID_TARGET);
    }

    @Test
    void aCheckNameWithAControlOrFormatCharacterIsRejected() {
        // given
        CheckDraft control = draft("bad\u0007name", CheckType.HTTP, "https://control.test");
        CheckDraft format = draft("bad\u200Fname", CheckType.HTTP, "https://format.test");

        // when
        Throwable controlRejected = catchThrowable(() -> CheckValidator.validate(control));
        Throwable formatRejected = catchThrowable(() -> CheckValidator.validate(format));

        // then
        assertInvalid(controlRejected, "/name", SpaceError.INVALID_NAME);
        assertInvalid(formatRejected, "/name", SpaceError.INVALID_NAME);
    }

    @Test
    void aCheckNameCountsCodePoints() {
        // given
        String emoji = "\uD83D\uDE00";
        String atLimit = "a".repeat(CheckFieldLimits.MAX_NAME_LENGTH - 1) + emoji;
        String overLimit = "a".repeat(CheckFieldLimits.MAX_NAME_LENGTH) + emoji;
        CheckDraft acceptedDraft = draft(atLimit, CheckType.HTTP, "https://limit.test");
        CheckDraft rejectedDraft = draft(overLimit, CheckType.HTTP, "https://over-limit.test");

        // when
        CheckDraft accepted = CheckValidator.validate(acceptedDraft);
        Throwable rejected = catchThrowable(() -> CheckValidator.validate(rejectedDraft));

        // then
        assertThat(accepted.name()).isEqualTo(atLimit);
        assertInvalid(rejected, "/name", SpaceError.INVALID_NAME);
    }

    @Test
    void aNameIsNormalizedToNfcAndStripped() {
        // given
        CheckDraft check = draft("  Cafe\u0301  ", CheckType.HTTP, "https://nfc.test");

        // when
        CheckDraft accepted = CheckValidator.validate(check);

        // then
        assertThat(accepted.name()).isEqualTo("Caf\u00E9");
    }

    @Test
    void tagsDifferingOnlyInCaseOrNoBreakSpaceAreOneTag() {
        // given
        List<String> tags = List.of("Hello World", "hello world", "hello\u00A0world");
        CheckDraft draft =
                new CheckDraft("tagged", CheckType.HTTP, "https://tagged.test", 60, 10, 1, null, null, null, tags);

        // when
        CheckDraft accepted = CheckValidator.validate(draft);

        // then
        assertThat(accepted.tags()).containsExactly("Hello World");
    }

    @Test
    void runbookUrlMustBeHttpOrHttps() {
        // given
        CheckDraft scriptRunbook = draft(CheckType.HTTP, "https://example.test", "javascript:alert(1)");
        CheckDraft validRunbook = draft(CheckType.HTTP, "https://example.test", "https://runbook.example.test/x");

        // when
        Throwable rejected = catchThrowable(() -> CheckValidator.validate(scriptRunbook));
        CheckDraft accepted = CheckValidator.validate(validRunbook);

        // then
        assertInvalid(rejected, "/runbookUrl", CheckError.INVALID_RUNBOOK_URL);
        assertThat(accepted.runbookUrl()).isEqualTo("https://runbook.example.test/x");
    }

    private static void assertInvalid(Throwable throwable, String pointer, ErrorCode code) {
        assertThat(throwable).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) throwable;
        assertThat(exception.getErrorCode()).isEqualTo(CommonError.VALIDATION_FAILED);
        assertThat(exception.getFieldErrors()).hasSize(1);
        ApiFieldError problem = exception.getFieldErrors().getFirst();
        assertThat(problem.pointer()).isEqualTo(pointer);
        assertThat(problem.code()).isEqualTo(code.getCode());
    }

    private static CheckDraft draft(CheckType type, String target, @Nullable String runbookUrl) {
        return new CheckDraft("check", type, target, 60, 10, 1, null, null, runbookUrl, List.of());
    }

    private static CheckDraft draft(String name, CheckType type, String target) {
        return new CheckDraft(name, type, target, 60, 10, 1, null, null, null, List.of());
    }
}
