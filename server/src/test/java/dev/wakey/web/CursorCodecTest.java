package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
        assertThat(thrown).isInstanceOf(ApiProblemException.class);
        ApiProblemException problem = (ApiProblemException) Objects.requireNonNull(thrown);
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(problem.getCode()).isEqualTo("invalid_cursor");
    }
}
