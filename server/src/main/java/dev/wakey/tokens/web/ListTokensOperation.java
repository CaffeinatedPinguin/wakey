package dev.wakey.tokens.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Maps the token listing operation and documents all of its answers together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Operation(operationId = "listTokens", summary = "List the caller's API tokens in a space")
@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
@SecurityRequirement(name = TokenOpenApiConfiguration.SESSION_AUTH)
@RequestMapping(path = TokenPaths.COLLECTION, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
@interface ListTokensOperation {}
