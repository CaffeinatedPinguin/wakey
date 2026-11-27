package dev.wakey.security.web;

import dev.wakey.security.UserSessions;
import dev.wakey.spaces.web.SessionUser;
import dev.wakey.web.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP adapter for the browser sessions of the caller. Session-only by design, so an API token
 * is rejected by the shared session actor before any rule runs.
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "sessionAuth")
final class MySessionsController {

    private static final String SESSIONS_PATH = ApiPaths.API_V1 + "/me/sessions";

    private static final String SESSION_PATH = SESSIONS_PATH + "/{id}";

    private static final String ERROR_MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final UserSessions sessions;

    @Operation(operationId = "listMySessions", summary = "List the active sessions of the caller")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", useReturnTypeSchema = true),
        @ApiResponse(
                responseCode = "403",
                description = "The caller uses an API token.",
                content =
                        @Content(mediaType = ERROR_MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = SESSIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    List<SessionResponse> listMySessions(SessionUser session, HttpServletRequest request) {
        String currentId = currentSessionId(request);
        List<UserSessions.SessionView> views = sessions.list(session.user(), currentId);
        return views.stream().map(SessionResponse::from).toList();
    }

    @Operation(operationId = "revokeMySession", summary = "End one of the caller's sessions")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The session was ended."),
        @ApiResponse(
                responseCode = "403",
                description = "The caller uses an API token.",
                content =
                        @Content(mediaType = ERROR_MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "The session was not found.",
                content =
                        @Content(mediaType = ERROR_MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = SESSION_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeMySession(
            @Parameter(
                            description = "The opaque session identifier",
                            required = true,
                            schema = @Schema(type = "string"))
                    @PathVariable
                    String id,
            SessionUser session) {
        sessions.revoke(session.user(), id);
    }

    @Operation(operationId = "revokeMyOtherSessions", summary = "End every other session of the caller")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Every other session was ended."),
        @ApiResponse(
                responseCode = "403",
                description = "The caller uses an API token.",
                content =
                        @Content(mediaType = ERROR_MEDIA_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = SESSIONS_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeMyOtherSessions(SessionUser session, HttpServletRequest request) {
        sessions.revokeOthers(session.user(), currentSessionId(request));
    }

    private static @Nullable String currentSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return session.getId();
    }

    record SessionResponse(String id, Instant createdAt, Instant lastUsedAt, Instant expiresAt, boolean current) {

        static SessionResponse from(UserSessions.SessionView view) {
            return new SessionResponse(
                    view.id(), view.createdAt(), view.lastUsedAt(), view.expiresAt(), view.current());
        }
    }
}
