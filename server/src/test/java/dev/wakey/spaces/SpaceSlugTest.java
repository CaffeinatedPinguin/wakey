package dev.wakey.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.wakey.testsupport.ContractFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SpaceSlugTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void validSlugsAreAccepted() {
        // given
        String fortyCharacters = "a" + "b".repeat(38) + "c";
        List<String> valid = List.of("abc", "acme", "acme-space", "a1b", fortyCharacters);

        // when
        List<String> accepted = valid.stream().map(SpaceSlug::requireValid).toList();

        // then
        assertThat(accepted).containsExactlyElementsOf(valid);
    }

    @Test
    void invalidSlugsAreRejected() {
        // given
        String fortyOneCharacters = "a" + "b".repeat(39) + "c";
        List<String> invalid = List.of("ab", "1abc", "-abc", "Abc", "abc-", "abc d", "", fortyOneCharacters);

        // when
        List<Class<?>> rejectionTypes = new ArrayList<>();
        for (String candidate : invalid) {
            Throwable rejection = catchThrowable(() -> SpaceSlug.requireValid(candidate));
            rejectionTypes.add(rejection.getClass());
        }

        // then
        assertThat(rejectionTypes).hasSize(invalid.size()).containsOnly(InvalidSpaceException.class);
    }

    @Test
    void slugPatternInTheContractMatchesTheRule() throws Exception {
        // given
        String contract = Files.readString(ContractFile.path());
        JsonNode document = MAPPER.readTree(contract);

        // when
        String pattern = document.path("components")
                .path("schemas")
                .path("SpaceResponse")
                .path("properties")
                .path("slug")
                .path("pattern")
                .asString();

        // then
        assertThat(pattern).isEqualTo(SpaceSlug.PATTERN);
    }
}
