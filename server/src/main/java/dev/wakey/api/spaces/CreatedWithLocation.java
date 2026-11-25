package dev.wakey.api.spaces;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.HttpHeaders;

/** Documents a 201 answer that points at the created (or already existing) space. */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "201",
        description = "The space was created.",
        headers =
                @Header(
                        name = HttpHeaders.LOCATION,
                        description = "The URL of the created space.",
                        schema = @Schema(type = "string")),
        useReturnTypeSchema = true)
@interface CreatedWithLocation {}
