package dev.wakey.spaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Maps the upsert operation and documents its 200 and 201 answers together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(path = "/{slug}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
@Operation(summary = "Create or rename a space idempotently")
@ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true)
@CreatedWithLocation
@interface PutSpaceOperation {}
