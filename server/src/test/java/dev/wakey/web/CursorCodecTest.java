package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    @Test
    void cursorRoundTrips() {
        // given
        String sortValue = "2026-11-24T10:15:30Z";

        // when
        String cursor = CursorCodec.encode(sortValue);
        String decoded = CursorCodec.decode(cursor);

        // then
        assertThat(decoded).isEqualTo(sortValue);
    }

    @Test
    void tamperedCursorIsRejectedAsProblem() {
        // when
        Throwable thrown = catchThrowable(() -> CursorCodec.decode("not-base64!"));

        // then
        assertThat(thrown).isInstanceOf(ApiException.class);
        ApiException problem = (ApiException) Objects.requireNonNull(thrown);
        String code = problem.getErrorCode().getCode();
        assertThat(problem.getErrorCode()).isEqualTo(CommonError.INVALID_CURSOR);
        assertThat(code).isEqualTo(CommonError.INVALID_CURSOR.getCode());
    }

    @Test
    void aCursorLongerThanTheLimitIsRejected() {
        // given
        String cursor = "a".repeat(CursorCodec.MAX_CURSOR_LENGTH + 1);

        // when
        Throwable thrown = catchThrowable(() -> CursorCodec.decode(cursor));

        // then
        assertThat(thrown).isInstanceOf(ApiException.class);
        ApiException problem = (ApiException) Objects.requireNonNull(thrown);
        assertThat(problem.getErrorCode()).isEqualTo(CommonError.INVALID_CURSOR);
    }

    @Test
    void theCursorCarriesOnlyTheSortValue() {
        // given
        String sortValue = "acme-space";

        // when
        String cursor = CursorCodec.encode(sortValue);
        Base64.Decoder decoder = Base64.getUrlDecoder();
        byte[] bytes = decoder.decode(cursor);
        String payload = new String(bytes, StandardCharsets.UTF_8);
        String decoded = CursorCodec.decode(cursor);

        // then
        assertThat(payload).isEqualTo("v1\u001f" + sortValue);
        assertThat(decoded).isEqualTo(sortValue);
    }
}
