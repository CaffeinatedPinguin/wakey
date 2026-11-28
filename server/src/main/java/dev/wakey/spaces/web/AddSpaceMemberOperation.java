package dev.wakey.spaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Maps the member creation and documents its answers together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag(name = "spaces")
@Operation(summary = "Add a member to a space")
@ApiResponses({
    @ApiResponse(
            responseCode = "200",
            description = "The membership already exists; an existing membership keeps its role.",
            useReturnTypeSchema = true),
    @ApiResponse(responseCode = "201", description = "The membership was created.", useReturnTypeSchema = true),
    @ApiResponse(
            responseCode = "400",
            description = "The request contains invalid fields.",
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
                            schema = @Schema(implementation = ProblemDetail.class)))
})
@RequestMapping(method = RequestMethod.POST)
@interface AddSpaceMemberOperation {}
