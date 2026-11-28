package dev.wakey.spaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Maps the upsert operation and documents its 200, 201 and error answers together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(
        path = SpacesHttp.PATH + "/{slug}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Operation(
        summary = "Create or rename a space idempotently",
        description = "A missing version renames the space without a version condition, but a concurrent change "
                + "of the same space can still answer 409 version_conflict.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
    @ApiResponse(
            responseCode = "400",
            description = "The request is invalid.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
            responseCode = "403",
            description = "Access is denied.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
            responseCode = "409",
            description = "The resource changed since the supplied version.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
})
@CreatedWithLocation
@interface PutSpaceOperation {}
