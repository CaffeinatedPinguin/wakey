package dev.wakey.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;

/** The keyset paging shared by every list endpoint; both parameters are optional query values. */
@ParameterObject
public record PageQuery(
        @Schema(description = "The page size, at least 1; values above the instance maximum are capped", example = "20")
        @Positive
        Integer limit,

        @Schema(description = "The opaque cursor of the next page")
        String cursor) {

    public int pageSize(ApiProperties apiProperties) {
        return apiProperties.pageSize(limit);
    }
}
