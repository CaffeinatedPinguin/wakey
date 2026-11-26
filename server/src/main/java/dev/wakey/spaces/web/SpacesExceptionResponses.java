package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceError;
import dev.wakey.web.CommonError;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

/** Documents the error answers that each spaces operation really returns. */
final class SpacesExceptionResponses implements GlobalOperationCustomizer {

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private static final String SPACE_MEMBER_RESPONSE_REF = "#/components/schemas/SpaceMemberResponse";

    private static final String ADD_MEMBER_DESCRIPTION =
            "The membership exists; an existing membership keeps its role.";

    private static final String PUT_DESCRIPTION =
            "A missing version renames the space without a version condition, but a concurrent change "
                    + "of the same space can still answer 409 ";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        String operationId = operation.getOperationId();
        if (operationId == null) {
            operationId = handlerMethod.getMethod().getName();
        }
        switch (operationId) {
            case "listSpaces" -> documentList(operation);
            case "getSpace" -> documentGet(operation);
            case "createSpace" -> documentCreate(operation);
            case "putSpace" -> documentPut(operation);
            case "listSpaceMembers" -> documentMemberList(operation);
            case "addSpaceMember" -> documentMemberAdd(operation);
            case "changeSpaceMemberRole" -> documentMemberChange(operation);
            case "removeSpaceMember" -> documentMemberRemove(operation);
            default -> {
                // Every other operation documents its errors elsewhere.
            }
        }
        return operation;
    }

    private static void documentList(Operation operation) {
        add(operation, HttpStatus.BAD_REQUEST, "The limit or the cursor is invalid.");
    }

    private static void documentGet(Operation operation) {
        add(operation, HttpStatus.BAD_REQUEST, "The slug is invalid.");
        add(operation, HttpStatus.NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentCreate(Operation operation) {
        add(operation, HttpStatus.BAD_REQUEST, CommonError.BAD_REQUEST.getDetail());
        add(operation, HttpStatus.FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, HttpStatus.CONFLICT, SpaceError.SLUG_TAKEN.getDetail());
    }

    private static void documentPut(Operation operation) {
        String conflictCode = CommonError.VERSION_CONFLICT.getCode();
        operation.setDescription(PUT_DESCRIPTION + conflictCode + ".");
        add(operation, HttpStatus.BAD_REQUEST, CommonError.BAD_REQUEST.getDetail());
        add(operation, HttpStatus.FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, HttpStatus.CONFLICT, CommonError.VERSION_CONFLICT.getDetail());
    }

    private static void documentMemberList(Operation operation) {
        add(operation, HttpStatus.BAD_REQUEST, "The limit or the cursor is invalid.");
        add(operation, HttpStatus.NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentMemberAdd(Operation operation) {
        operation.setSummary("Add a member to a space");
        replaceWithJson(operation, HttpStatus.CREATED, ADD_MEMBER_DESCRIPTION, SPACE_MEMBER_RESPONSE_REF);
        add(operation, HttpStatus.BAD_REQUEST, CommonError.VALIDATION_FAILED.getDetail());
        add(operation, HttpStatus.FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, HttpStatus.NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentMemberChange(Operation operation) {
        operation.setSummary("Change a member's role");
        replaceWithNoContent(operation, HttpStatus.NO_CONTENT, "The role was changed.");
        add(operation, HttpStatus.BAD_REQUEST, CommonError.VALIDATION_FAILED.getDetail());
        add(operation, HttpStatus.FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, HttpStatus.NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentMemberRemove(Operation operation) {
        operation.setSummary("Remove a member from a space");
        replaceWithNoContent(operation, HttpStatus.NO_CONTENT, "The member was removed.");
        add(operation, HttpStatus.BAD_REQUEST, CommonError.VALIDATION_FAILED.getDetail());
        add(operation, HttpStatus.FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, HttpStatus.NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void replaceWithJson(Operation operation, HttpStatus status, String description, String schemaRef) {
        Schema<?> schema = new Schema<>().$ref(schemaRef);
        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content();
        content.addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType);
        ApiResponse response = new ApiResponse().description(description).content(content);
        replace(operation, status, response);
    }

    private static void replaceWithNoContent(Operation operation, HttpStatus status, String description) {
        replace(operation, status, new ApiResponse().description(description));
    }

    private static void replace(Operation operation, HttpStatus status, ApiResponse response) {
        ApiResponses responses = responsesOf(operation);
        responses.remove(codeOf(HttpStatus.OK));
        responses.addApiResponse(codeOf(status), response);
    }

    private static void add(Operation operation, HttpStatus status, String description) {
        ApiResponses responses = responsesOf(operation);
        responses.addApiResponse(codeOf(status), errorResponse(description));
    }

    private static String codeOf(HttpStatus status) {
        return Integer.toString(status.value());
    }

    private static ApiResponses responsesOf(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        return responses;
    }

    private static ApiResponse errorResponse(String description) {
        Schema<?> schema = new Schema<>().$ref(ERROR_SCHEMA_REF);
        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content();
        content.addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE, mediaType);
        return new ApiResponse().description(description).content(content);
    }
}
