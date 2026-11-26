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

/** Maps the add-member operation and documents its 201 answer together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(
        path = SpacesHttp.MEMBERS_PATH,
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Operation(summary = "Add a member to a space")
@ApiResponse(
        responseCode = "201",
        description = "The membership exists; an existing membership keeps its role.",
        useReturnTypeSchema = true)
@interface AddSpaceMemberOperation {}
