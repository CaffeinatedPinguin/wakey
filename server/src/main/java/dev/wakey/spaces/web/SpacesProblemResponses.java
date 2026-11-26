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
import org.springframework.web.method.HandlerMethod;

/** Documents the error answers that each spaces operation really returns. */
final class SpacesProblemResponses implements GlobalOperationCustomizer {

    private static final String PROBLEM_DETAIL_REF = "#/components/schemas/ProblemDetail";

    private static final String BAD_REQUEST = "400";

    private static final String FORBIDDEN = "403";

    private static final String NOT_FOUND = "404";

    private static final String CONFLICT = "409";

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
        add(operation, BAD_REQUEST, "The limit or the cursor is invalid.");
    }

    private static void documentGet(Operation operation) {
        add(operation, BAD_REQUEST, "The slug is invalid.");
        add(operation, NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentCreate(Operation operation) {
        add(operation, BAD_REQUEST, CommonError.BAD_REQUEST.getDetail());
        add(operation, FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, CONFLICT, SpaceError.SLUG_TAKEN.getDetail());
    }

    private static void documentPut(Operation operation) {
        String conflictCode = SpaceError.VERSION_CONFLICT.getCode();
        operation.setDescription(PUT_DESCRIPTION + conflictCode + ".");
        add(operation, BAD_REQUEST, CommonError.BAD_REQUEST.getDetail());
        add(operation, FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, CONFLICT, SpaceError.VERSION_CONFLICT.getDetail());
    }

    private static void documentMemberList(Operation operation) {
        add(operation, BAD_REQUEST, "The limit or the cursor is invalid.");
        add(operation, NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentMemberAdd(Operation operation) {
        add(operation, BAD_REQUEST, CommonError.VALIDATION_FAILED.getDetail());
        add(operation, FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentMemberChange(Operation operation) {
        add(operation, BAD_REQUEST, CommonError.VALIDATION_FAILED.getDetail());
        add(operation, FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void documentMemberRemove(Operation operation) {
        add(operation, BAD_REQUEST, CommonError.VALIDATION_FAILED.getDetail());
        add(operation, FORBIDDEN, CommonError.FORBIDDEN.getDetail());
        add(operation, NOT_FOUND, CommonError.NOT_FOUND.getDetail());
    }

    private static void add(Operation operation, String status, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        responses.addApiResponse(status, problemResponse(description));
    }

    private static ApiResponse problemResponse(String description) {
        Schema<?> schema = new Schema<>().$ref(PROBLEM_DETAIL_REF);
        MediaType mediaType = new MediaType().schema(schema);
        Content content = new Content();
        content.addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE, mediaType);
        return new ApiResponse().description(description).content(content);
    }
}
