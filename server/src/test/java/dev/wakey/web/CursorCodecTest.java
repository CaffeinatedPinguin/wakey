package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Objects;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    @Test
    void cursorRoundTrips() {
        // given
        String sortValue = "2026-11-24T10:15:30Z";
        String id = "9f8e7d6c-1a2b-3c4d-5e6f-7a8b9c0d1e2f";

        // when
        String cursor = CursorCodec.encode(sortValue, id);
        CursorCodec.CursorPosition position = CursorCodec.decode(cursor);

        // then
        assertThat(position.sortValue()).isEqualTo(sortValue);
        assertThat(position.id()).isEqualTo(id);
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
}
