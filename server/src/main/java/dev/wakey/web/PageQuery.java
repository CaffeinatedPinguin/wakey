package dev.wakey.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;

/** The keyset paging shared by every list endpoint. */
@ParameterObject
public record PageQuery(
        @Schema(description = "The page size, at least 1; values above the instance maximum are capped", example = "20")
        @Positive
        @Nullable
        Integer limit,

        @Schema(description = "The opaque cursor of the next page") @Nullable
        String cursor) {

    public int pageSize(ApiProperties apiProperties) {
        return apiProperties.pageSize(limit);
    }
}
