package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.CheckError;
import dev.wakey.checks.CheckSettingError;
import dev.wakey.settings.SettingError;
import dev.wakey.spaces.SpaceError;
import dev.wakey.testsupport.ContractFile;
import dev.wakey.tokens.TokenError;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ErrorCodeWireFormatTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void errorCodesKeepTheirWireValues() {
        // when
        // then
        assertCode(CommonError.BAD_REQUEST, "bad_request", HttpStatus.BAD_REQUEST);
        assertCode(CommonError.VALIDATION_FAILED, "validation_failed", HttpStatus.BAD_REQUEST);
        assertCode(CommonError.INVALID_BODY, "invalid_body", HttpStatus.BAD_REQUEST);
        assertCode(CommonError.INVALID_CURSOR, "invalid_cursor", HttpStatus.BAD_REQUEST);
        assertCode(CommonError.UNAUTHENTICATED, "unauthenticated", HttpStatus.UNAUTHORIZED);
        assertCode(CommonError.FORBIDDEN, "forbidden", HttpStatus.FORBIDDEN);
        assertCode(CommonError.NOT_FOUND, "not_found", HttpStatus.NOT_FOUND);
        assertCode(CommonError.METHOD_NOT_ALLOWED, "method_not_allowed", HttpStatus.METHOD_NOT_ALLOWED);
        assertCode(CommonError.NOT_ACCEPTABLE, "not_acceptable", HttpStatus.NOT_ACCEPTABLE);
        assertCode(CommonError.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertCode(CommonError.VERSION_CONFLICT, "version_conflict", HttpStatus.CONFLICT);
        assertCode(CommonError.INTERNAL_ERROR, "internal_error", HttpStatus.INTERNAL_SERVER_ERROR);
        assertCode(SpaceError.SLUG_TAKEN, "slug_taken", HttpStatus.CONFLICT);
        assertCode(SpaceError.INVALID_NAME, "invalid_name", HttpStatus.BAD_REQUEST);
        assertCode(SpaceError.INVALID_SLUG, "invalid_slug", HttpStatus.BAD_REQUEST);
        assertCode(SpaceError.INVALID_SUBJECT, "invalid_subject", HttpStatus.BAD_REQUEST);
        assertCode(SpaceError.INVALID_ISSUER, "invalid_issuer", HttpStatus.BAD_REQUEST);
    }

    @Test
    void everyWireCodeIsDefinedOnce() {
        // given the public error-code enums; a package-private module alias is not part of the contract
        List<ErrorCode> codes = new ArrayList<>();
        Collections.addAll(codes, CommonError.values());
        Collections.addAll(codes, SpaceError.values());
        Collections.addAll(codes, CheckError.values());
        Collections.addAll(codes, CheckSettingError.values());
        Collections.addAll(codes, SettingError.values());
        Collections.addAll(codes, TokenError.values());

        // when
        Map<String, Long> counts = new HashMap<>();
        for (ErrorCode code : codes) {
            counts.merge(code.getCode(), 1L, Long::sum);
        }

        // then
        assertThat(counts)
                .allSatisfy((code, count) -> assertThat(count).as(code).isEqualTo(1L));
    }

    @Test
    void theContractListsEveryErrorCode() throws Exception {
        // given
        String contract = Files.readString(ContractFile.path());
        JsonNode document = MAPPER.readTree(contract);
        JsonNode codeSchema = document.path("components")
                .path("schemas")
                .path("ProblemDetail")
                .path("properties")
                .path("code");
        List<String> listed = new ArrayList<>();
        for (JsonNode value : codeSchema.path("enum")) {
            listed.add(value.asString());
        }

        // when
        Set<String> expected = new LinkedHashSet<>();
        for (CommonError error : CommonError.values()) {
            expected.add(error.getCode());
        }
        for (SpaceError error : SpaceError.values()) {
            expected.add(error.getCode());
        }
        for (CheckError error : CheckError.values()) {
            expected.add(error.getCode());
        }
        for (SettingError error : SettingError.values()) {
            expected.add(error.getCode());
        }
        for (CheckSettingError error : CheckSettingError.values()) {
            expected.add(error.getCode());
        }
        for (TokenError error : TokenError.values()) {
            expected.add(error.getCode());
        }

        // then
        assertThat(listed).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static void assertCode(ErrorCode error, String wireValue, HttpStatus status) {
        assertThat(error.getCode()).isEqualTo(wireValue);
        assertThat(error.getStatus()).isEqualTo(status);
        assertThat(error.getDetail()).isNotBlank();
        assertThat(error.getDetail()).endsWith(".");
    }
}
