package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CursorPageTest {

    @Test
    void cursorPageCarriesTheCursorOnlyWhenThereIsAnotherPage() {
        // given
        List<String> exactPage = List.of("a", "b", "c");
        List<String> pageWithAnExtraRow = List.of("a", "b", "c", "d");

        // when
        CursorPage<String> lastPage = CursorPage.of(exactPage, 3, value -> value, value -> value);
        CursorPage<String> pageWithMore = CursorPage.of(pageWithAnExtraRow, 3, value -> value, value -> value);

        // then
        assertThat(lastPage.items()).containsExactly("a", "b", "c");
        assertThat(lastPage.nextCursor()).isNull();
        assertThat(pageWithMore.items()).containsExactly("a", "b", "c");
        assertThat(pageWithMore.nextCursor()).isEqualTo(CursorCodec.encode("c"));
    }
}
