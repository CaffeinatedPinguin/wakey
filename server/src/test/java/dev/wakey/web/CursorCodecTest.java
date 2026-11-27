package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    private static final int MAX_CURSOR_LENGTH = CursorCodec.maxCursorLength(100);

    @Test
    void cursorRoundTrips() {
        // given
        String sortValue = "2026-11-24T10:15:30Z";

        // when
        String cursor = CursorCodec.encode(sortValue);
        String decoded = CursorCodec.decode(cursor, MAX_CURSOR_LENGTH);

        // then
        assertThat(decoded).isEqualTo(sortValue);
    }

    @Test
    void tamperedCursorIsRejected() {
        // when
        Throwable thrown = catchThrowable(() -> CursorCodec.decode("not-base64!", MAX_CURSOR_LENGTH));

        // then
        assertThat(thrown).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) Objects.requireNonNull(thrown);
        String code = exception.getErrorCode().getCode();
        assertThat(exception.getErrorCode()).isEqualTo(CommonError.INVALID_CURSOR);
        assertThat(code).isEqualTo(CommonError.INVALID_CURSOR.getCode());
    }

    @Test
    void aCursorLongerThanTheLimitIsRejected() {
        // given
        int maxCursorLength = CursorCodec.maxCursorLength(100);
        String cursor = "a".repeat(maxCursorLength + 1);

        // when
        Throwable thrown = catchThrowable(() -> CursorCodec.decode(cursor, maxCursorLength));

        // then
        assertThat(thrown).isInstanceOf(ApiException.class);
        ApiException exception = (ApiException) Objects.requireNonNull(thrown);
        assertThat(exception.getErrorCode()).isEqualTo(CommonError.INVALID_CURSOR);
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
        String decoded = CursorCodec.decode(cursor, MAX_CURSOR_LENGTH);

        // then
        assertThat(payload).isEqualTo("v1\u001f" + sortValue);
        assertThat(decoded).isEqualTo(sortValue);
    }
}
