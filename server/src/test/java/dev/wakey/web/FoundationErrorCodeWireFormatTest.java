package dev.wakey.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.wakey.checks.CheckError;
import dev.wakey.checks.CheckSettingError;
import dev.wakey.settings.SettingError;
import dev.wakey.spaces.SpaceError;
import dev.wakey.tokens.TokenError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Pins the wire values of the error codes the foundation modules introduced. */
class FoundationErrorCodeWireFormatTest {

    @Test
    void errorCodesOfTheFoundationModulesKeepTheirWireValues() {
        // when
        // then
        assertCode(CommonError.NOT_FOUND, "not_found", HttpStatus.NOT_FOUND);
        assertCode(CheckError.PLAN_LIMIT, "plan_limit", HttpStatus.BAD_REQUEST);
        assertCode(SpaceError.INVALID_NAME, "invalid_name", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.INVALID_TARGET, "invalid_target", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.INVALID_TIMEOUT, "invalid_timeout", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.INVALID_TAGS, "invalid_tags", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.INVALID_RUNBOOK_URL, "invalid_runbook_url", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.INVALID_GROUP_NAME, "invalid_group_name", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.INVALID_DESCRIPTION, "invalid_description", HttpStatus.BAD_REQUEST);
        assertCode(CheckError.LOCATION_ERROR_CANNOT_BE_DOWN, "location_error_cannot_be_down", HttpStatus.BAD_REQUEST);
        assertCode(SettingError.OUT_OF_PLAN, "out_of_plan", HttpStatus.BAD_REQUEST);
        assertCode(SettingError.UNKNOWN_PLAN, "unknown_plan", HttpStatus.BAD_REQUEST);
        assertCode(SettingError.INVALID_KEY, "invalid_key", HttpStatus.BAD_REQUEST);
        assertCode(SettingError.INVALID_VALUE, "invalid_value", HttpStatus.BAD_REQUEST);
        assertCode(CheckSettingError.TIMEOUT_EXCEEDS_INTERVAL, "timeout_exceeds_interval", HttpStatus.BAD_REQUEST);
        assertCode(TokenError.TOKEN_LIMIT, "token_limit", HttpStatus.BAD_REQUEST);
        assertCode(TokenError.INVALID_TOKEN, "invalid_token", HttpStatus.UNAUTHORIZED);
        assertCode(CommonError.INSUFFICIENT_SCOPE, "insufficient_scope", HttpStatus.FORBIDDEN);
        assertCode(SpaceError.INVALID_NAME, "invalid_name", HttpStatus.BAD_REQUEST);
        assertCode(TokenError.INVALID_EXPIRY, "invalid_expiry", HttpStatus.BAD_REQUEST);
    }

    private static void assertCode(ErrorCode error, String wireValue, HttpStatus status) {
        assertThat(error.getCode()).isEqualTo(wireValue);
        assertThat(error.getStatus()).isEqualTo(status);
        assertThat(error.getDetail()).isNotBlank();
        assertThat(error.getDetail()).endsWith(".");
    }
}
