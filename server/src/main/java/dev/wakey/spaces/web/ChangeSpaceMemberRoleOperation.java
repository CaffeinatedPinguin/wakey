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

/** Maps the change-role operation and documents its 204 answer together. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(
        path = SpacesHttp.MEMBER_ROLE_PATH,
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Operation(summary = "Change a member's role")
@ApiResponse(responseCode = "204", description = "The role was changed.")
@interface ChangeSpaceMemberRoleOperation {}
