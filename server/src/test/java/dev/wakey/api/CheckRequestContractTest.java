package dev.wakey.api;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.CheckFieldLimits;
import dev.wakey.checks.CheckLimits;
import dev.wakey.testsupport.ContractFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Pins the bounds of the check request that the contract promises to its clients. */
class CheckRequestContractTest {

    @Test
    void theContractCarriesTheBoundsOfACheckRequest() throws Exception {
        // given
        Path contract = ContractFile.path();
        String content = Files.readString(contract);
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode document = mapper.readTree(content);
        JsonNode properties =
                document.path("components").path("schemas").path("CheckRequest").path("properties");
        JsonNode name = properties.path("name");
        JsonNode target = properties.path("target");
        JsonNode interval = properties.path("intervalSeconds");
        JsonNode timeout = properties.path("timeoutSeconds");
        JsonNode retries = properties.path("retries");
        JsonNode tags = properties.path("tags");

        // when
        int nameMinLength = name.path("minLength").asInt();
        int nameMaxLength = name.path("maxLength").asInt();
        int targetMinLength = target.path("minLength").asInt();
        int targetMaxLength = target.path("maxLength").asInt();
        int intervalMinimum = interval.path("minimum").asInt();
        int timeoutMinimum = timeout.path("minimum").asInt();
        int retriesMinimum = retries.path("minimum").asInt();
        int retriesMaximum = retries.path("maximum").asInt();
        int tagMaximum = tags.path("maxItems").asInt();

        // then
        assertThat(nameMinLength).isEqualTo(CheckFieldLimits.MIN_TEXT_LENGTH);
        assertThat(nameMaxLength).isEqualTo(CheckFieldLimits.MAX_NAME_LENGTH);
        assertThat(targetMinLength).isEqualTo(CheckFieldLimits.MIN_TEXT_LENGTH);
        assertThat(targetMaxLength).isEqualTo(CheckFieldLimits.MAX_TARGET_LENGTH);
        assertThat(intervalMinimum).isEqualTo(CheckLimits.MIN_INTERVAL_SECONDS);
        assertThat(timeoutMinimum).isEqualTo(CheckLimits.MIN_TIMEOUT_SECONDS);
        assertThat(retriesMinimum).isEqualTo(CheckLimits.MIN_RETRIES);
        assertThat(retriesMaximum).isEqualTo(CheckLimits.MAX_RETRIES);
        assertThat(tagMaximum).isEqualTo(CheckFieldLimits.MAX_TAGS);
    }
}
