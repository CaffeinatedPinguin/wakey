package dev.wakey.api.spaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The desired state of a space; the name is trimmed and must stay within the domain limit. */
@Schema(description = "The desired state of a space")
public record PutSpaceRequest(
        @Schema(description = "The display name, 1-100 characters after trimming", example = "Acme")
        @NotBlank
        @Size(max = 100)
        String name) {}
