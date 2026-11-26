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

/** Maps the remove-member operation and documents its 204 answer together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(
        path = SpacesHttp.MEMBERS_PATH,
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Operation(summary = "Remove a member from a space")
@ApiResponse(responseCode = "204", description = "The member was removed.")
@interface RemoveSpaceMemberOperation {}
