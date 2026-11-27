// This file is generated, do not edit.
// Source: api/openapi/public.json

export interface paths {
    "/api/v1/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Return the authenticated user */
        get: operations["getMe"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/me/sessions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List the active sessions of the caller */
        get: operations["listMySessions"];
        put?: never;
        post?: never;
        /** End every other session of the caller */
        delete: operations["revokeMyOtherSessions"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/me/sessions/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /** End one of the caller's sessions */
        delete: operations["revokeMySession"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listSpaces"];
        put?: never;
        post: operations["createSpace"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["getSpace"];
        /**
         * Create or rename a space idempotently
         * @description A missing version renames the space without a version condition, but a concurrent change of the same space can still answer 409 version_conflict.
         */
        put: operations["putSpace"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/checks": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List the checks of a space */
        get: operations["listChecks"];
        put?: never;
        /** Create a check in a space */
        post: operations["createCheck"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/checks/bulk": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Pause, resume or delete many checks at once */
        post: operations["bulkChecks"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/checks/counts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Count the checks of a space by state */
        get: operations["countChecks"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/checks/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Read one check of a space with its newest result */
        get: operations["getCheck"];
        /** Replace a check, rejecting a stale version */
        put: operations["updateCheck"];
        post?: never;
        /** Delete a check together with its results and tags */
        delete: operations["deleteCheck"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/checks/{id}/pause": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Pause monitoring of a check */
        post: operations["pauseCheck"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/checks/{id}/resume": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /** Resume monitoring of a check */
        post: operations["resumeCheck"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/members": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["listSpaceMembers"];
        put?: never;
        /** Add a member to a space */
        post: operations["addSpaceMember"];
        /** Remove a member from a space */
        delete: operations["removeSpaceMember"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/members/role": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** Change a member's role */
        put: operations["changeSpaceMemberRole"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/plan": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** Assign a plan to a space idempotently */
        put: operations["putSpacePlan"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/settings": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** Read the effective settings of a space */
        get: operations["getSpaceSettings"];
        put?: never;
        post?: never;
        /** Reset every space setting to the plan values */
        delete: operations["resetSpaceSettings"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/settings/{key}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /** Set a space setting within the plan */
        put: operations["putSpaceSetting"];
        post?: never;
        /** Reset a space setting to the plan value */
        delete: operations["deleteSpaceSetting"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/tokens": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List the caller's API tokens in a space */
        get: operations["listTokens"];
        put?: never;
        /** Create an API token for a space */
        post: operations["createToken"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/spaces/{slug}/tokens/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /** Revoke one of the caller's API tokens */
        delete: operations["revokeToken"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description A membership to add */
        AddSpaceMemberRequest: {
            /**
             * @description The OIDC issuer of the member
             * @example https://issuer.example.com
             */
            issuer: string;
            /** @description The role in the space */
            role: components["schemas"]["SpaceRole"];
            /**
             * @description The OIDC subject of the member
             * @example user-42
             */
            subject: string;
        };
        /** @enum {string} */
        BulkCheckAction: "PAUSE" | "RESUME" | "DELETE";
        /** @description The bulk action and either the check ids or the filter it applies to */
        BulkChecksRequest: {
            /**
             * @description The action to apply
             * @example PAUSE
             */
            action: components["schemas"]["BulkCheckAction"];
            /**
             * Format: int32
             * @description The number of checks the caller confirmed in the dialog; required with a filter and must be omitted with ids
             * @example 2
             */
            expectedCount?: number;
            /** @description The filter that selects every matching check */
            filter?: components["schemas"]["CheckFilterQuery"];
            /** @description The check ids, from 1 to 100 entries; duplicate ids are applied once */
            ids?: string[];
        };
        /** @description The result of a bulk action */
        BulkChecksResponse: {
            /**
             * Format: int32
             * @description How many checks the action changed
             * @example 2
             */
            affected: number;
        };
        /** @description The new role of a member */
        ChangeSpaceMemberRoleRequest: {
            /** @description The role in the space */
            role: components["schemas"]["SpaceRole"];
        };
        /** @description The filters shared by the check list and its counters */
        CheckFilterQuery: {
            /** @description The group name */
            group?: string;
            /** @description Why monitoring is paused */
            pausedReason?: components["schemas"]["PausedReason"];
            /** @description Text matched against the name and the target */
            q?: string;
            /** @description The current state, derived from the newest result */
            state?: components["schemas"]["CheckState"];
            /** @description A tag the check must carry */
            tag?: string;
            /** @description The protocol to measure */
            type?: components["schemas"]["CheckType"];
        };
        /** @description The editable fields of a check */
        CheckRequest: {
            /** @description The optional description */
            description?: string;
            /** @description The optional group name */
            groupName?: string;
            /**
             * Format: int32
             * @description Seconds between measurements
             * @example 60
             */
            intervalSeconds?: number;
            /**
             * @description The name, 1-100 characters after trimming
             * @example API health
             */
            name: string;
            /**
             * Format: int32
             * @description Retries after a failed measurement
             * @example 1
             */
            retries?: number;
            /** @description The optional runbook address */
            runbookUrl?: string;
            /** @description The optional tags */
            tags: string[];
            /**
             * @description The target address
             * @example https://example.test/health
             */
            target: string;
            /**
             * Format: int32
             * @description Seconds before a measurement times out
             * @example 10
             */
            timeoutSeconds?: number;
            /**
             * @description The protocol to measure
             * @example HTTP
             */
            type: components["schemas"]["CheckType"];
        };
        /** @description A check as seen by the caller */
        CheckResponse: {
            /** Format: date-time */
            createdAt: string;
            /** @description The optional description */
            description?: string;
            /** @description The optional group name */
            groupName?: string;
            /** Format: uuid */
            id: string;
            /** Format: int32 */
            intervalSeconds: number;
            /** @description The newest result, present only when the caller reads one check */
            lastResult?: components["schemas"]["CheckResultResponse"];
            name: string;
            paused: boolean;
            /** @description Why monitoring is paused, if it is */
            pausedReason?: components["schemas"]["PausedReason"];
            /** Format: int32 */
            retries: number;
            /** @description The optional runbook address */
            runbookUrl?: string;
            state: components["schemas"]["CheckState"];
            tags: string[];
            target: string;
            /** Format: int32 */
            timeoutSeconds: number;
            type: components["schemas"]["CheckType"];
            /** Format: date-time */
            updatedAt: string;
            /** Format: int64 */
            version: number;
        };
        /** @description The newest measurement of a check */
        CheckResultResponse: {
            /**
             * Format: date-time
             * @description When the measurement happened
             */
            checkedAt: string;
            /**
             * Format: int32
             * @description How long the measurement took in milliseconds
             */
            durationMs?: number;
            /** @description Why a failed measurement failed */
            errorCategory?: components["schemas"]["ErrorCategory"];
            /**
             * Format: int32
             * @description The HTTP status code of an HTTP check
             */
            httpStatus?: number;
            /**
             * @description The location that measured the check
             * @example server
             */
            locationId?: string;
            /** @description The measured phases as a JSON document */
            phases?: string;
            /** @description The negotiated protocol version */
            protocolVersion?: string;
            /** @description The state of the measurement */
            state: components["schemas"]["CheckState"];
        };
        /** @enum {string} */
        CheckState: "UP" | "DEGRADED" | "DOWN" | "UNKNOWN";
        /** @enum {string} */
        CheckType: "HTTP" | "TCP" | "CERTIFICATE";
        /** @description The desired state of a new space */
        CreateSpaceRequest: {
            /**
             * @description The display name, 1-100 characters after trimming
             * @example Acme
             */
            name: string;
            /**
             * @description The stable space slug
             * @example acme
             */
            slug: string;
        };
        /** @description A new API token */
        CreateTokenRequest: {
            /**
             * Format: date-time
             * @description When the token expires, or null for no expiry
             */
            expiresAt?: string | null;
            /**
             * @description The token name
             * @example ci-deploy
             */
            name: string;
            /** @description The greatest scope the token may use */
            scope: components["schemas"]["TokenScope"];
        };
        /** @description A newly created API token and its secret, shown only once */
        CreateTokenResponse: {
            /** @description The token secret, shown only in this response */
            secret: string;
            /** @description The stored token, without the secret or the hash */
            token: components["schemas"]["TokenSummary"];
        };
        CursorPageCheckResponse: {
            /** @description The items on this page */
            items: components["schemas"]["CheckResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        CursorPageSpaceMemberResponse: {
            /** @description The items on this page */
            items: components["schemas"]["SpaceMemberResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        CursorPageSpaceResponse: {
            /** @description The items on this page */
            items: components["schemas"]["SpaceResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        CursorPageSpaceSettingResponse: {
            /** @description The items on this page */
            items: components["schemas"]["SpaceSettingResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        CursorPageTokenSummary: {
            /** @description The items on this page */
            items: components["schemas"]["TokenSummary"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        /** @enum {string} */
        ErrorCategory: "TARGET_DOWN" | "LOCATION_NETWORK" | "AUTH";
        /**
         * @description A supported language
         * @enum {string}
         */
        Language: "pl" | "en";
        MeResponse: {
            /** @description Whether the user is an instance administrator */
            admin: boolean;
            /** @description The OIDC issuer of the authenticated user */
            issuer: string;
            /** @description The OIDC subject of the authenticated user */
            subject: string;
        };
        /** @description The reason to pause monitoring */
        PauseCheckRequest: {
            /**
             * @description The pause reason
             * @example MANUAL
             */
            reason: components["schemas"]["PausedReason"];
        };
        /** @enum {string} */
        PausedReason: "MANUAL" | "PLAN_LIMIT" | "ABANDONED" | "UNVERIFIED";
        ProblemDetail: {
            /** @enum {string} */
            code?: "bad_request" | "validation_failed" | "invalid_body" | "invalid_cursor" | "unauthenticated" | "forbidden" | "not_found" | "method_not_allowed" | "not_acceptable" | "unsupported_media_type" | "version_conflict" | "too_many_requests" | "internal_error" | "out_of_plan" | "plan_limit" | "invalid_target" | "invalid_timeout" | "invalid_tags" | "invalid_runbook_url" | "invalid_group_name" | "invalid_description" | "location_error_cannot_be_down" | "bulk_count_changed" | "unknown_plan" | "invalid_key" | "invalid_value" | "timeout_exceeds_interval" | "slug_taken" | "invalid_name" | "invalid_slug" | "invalid_subject" | "invalid_issuer" | "token_limit" | "invalid_token" | "insufficient_scope" | "invalid_expiry";
            detail?: string;
            errors?: {
                code?: string;
                params?: {
                    [key: string]: unknown;
                };
                pointer?: string;
            }[];
            /** Format: uri */
            instance?: string;
            params?: {
                [key: string]: unknown;
            };
            /** Format: int32 */
            status?: number;
            title?: string;
            /** Format: uri */
            type?: string;
        };
        /** @description The plan to assign to a space */
        PutSpacePlanRequest: {
            /**
             * @description A configured plan name
             * @example small
             */
            plan: string;
        };
        /** @description The desired state of a space */
        PutSpaceRequest: {
            /**
             * @description The display name, 1-100 characters after trimming
             * @example Acme
             */
            name: string;
            /**
             * Format: int64
             * @description The version the caller read; omit to rename without a version condition. A concurrent change of the same space can still answer 409.
             * @example 1
             */
            version?: number;
        };
        SessionResponse: {
            /** Format: date-time */
            createdAt?: string;
            current?: boolean;
            /** Format: date-time */
            expiresAt?: string;
            id?: string;
            /** Format: date-time */
            lastUsedAt?: string;
        };
        /** @description The desired value of a space setting */
        SetSpaceSettingRequest: {
            /**
             * @description The desired value
             * @example 60
             */
            value: string;
        };
        /** @enum {string} */
        SettingGroup: "GENERAL" | "DEFAULTS";
        /** @enum {string} */
        SettingSource: "SPACE" | "PLAN" | "INSTANCE";
        /** @enum {string} */
        SettingType: "STRING" | "INTEGER" | "ENUM";
        /** @description A membership of a space */
        SpaceMemberResponse: {
            /**
             * @description The OIDC issuer of the member
             * @example https://issuer.example.com
             */
            issuer: string;
            /** @description The role in the space */
            role: components["schemas"]["SpaceRole"];
            /**
             * @description The OIDC subject of the member
             * @example user-42
             */
            subject: string;
        };
        /** @description A space as seen by the caller */
        SpaceResponse: {
            /**
             * @description The display name
             * @example Acme
             */
            name: string;
            /** @description The caller's membership role, or null for an Admin who is not a member */
            role: components["schemas"]["SpaceRole"] | null;
            /**
             * @description The stable space slug
             * @example acme
             */
            slug: string;
            /**
             * Format: int64
             * @description The version growing whenever the name changes
             * @example 1
             */
            version: number;
        };
        /** @enum {string} */
        SpaceRole: "EDITOR" | "VIEWER";
        /** @description One effective space setting */
        SpaceSettingResponse: {
            /** @description The group the setting is rendered in */
            group: components["schemas"]["SettingGroup"];
            /**
             * @description The stable setting key
             * @example check.interval
             */
            key: string;
            /**
             * Format: int64
             * @description The effective upper bound, or null
             */
            max?: number | null;
            /**
             * Format: int64
             * @description The effective lower bound, or null
             */
            min?: number | null;
            /** @description The allowed values of an enum setting, or null */
            options?: components["schemas"]["Language"][] | null;
            /**
             * @description The plan the value was resolved against
             * @example small
             */
            planName: string;
            /** @description Where the effective value comes from */
            source: components["schemas"]["SettingSource"];
            /** @description The value shape */
            type: components["schemas"]["SettingType"];
            /** @description The unit of the value, or null */
            unit?: string | null;
            /**
             * @description The effective value
             * @example 60
             */
            value: string;
        };
        /** @description How many filtered checks are in each state */
        StateCountsResponse: {
            /** Format: int64 */
            degraded: number;
            /** Format: int64 */
            down: number;
            /** Format: int64 */
            unknown: number;
            /** Format: int64 */
            up: number;
        };
        /** @enum {string} */
        TokenScope: "READ" | "WRITE";
        TokenSummary: {
            /** Format: date-time */
            createdAt?: string;
            /** Format: date-time */
            expiresAt?: string;
            /** Format: uuid */
            id?: string;
            /** Format: date-time */
            lastUsedAt?: string;
            name?: string;
            /** Format: date-time */
            revokedAt?: string;
            scope?: components["schemas"]["TokenScope"];
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    getMe: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MeResponse"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    listMySessions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SessionResponse"][];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller uses an API token. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    revokeMyOtherSessions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Every other session was ended. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller uses an API token. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    revokeMySession: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The opaque session identifier */
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The session was ended. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller uses an API token. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The session was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    listSpaces: {
        parameters: {
            query?: {
                /** @description The page size, at least 1; values above the instance maximum are capped. */
                limit?: number;
                cursor?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CursorPageSpaceResponse"];
                };
            };
            /** @description The limit or the cursor is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    createSpace: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateSpaceRequest"];
            };
        };
        responses: {
            /** @description The space was created. */
            201: {
                headers: {
                    /** @description The URL of the created space. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SpaceResponse"];
                };
            };
            /** @description The request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Access is denied. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description A space with that slug already exists. */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    getSpace: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SpaceResponse"];
                };
            };
            /** @description The slug is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    putSpace: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PutSpaceRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SpaceResponse"];
                };
            };
            /** @description The space was created. */
            201: {
                headers: {
                    /** @description The URL of the created space. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SpaceResponse"];
                };
            };
            /** @description The request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Access is denied. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The resource changed since the supplied version. */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    listChecks: {
        parameters: {
            query?: {
                /**
                 * @description The maximum number of items to return
                 * @example 20
                 */
                limit?: number | null;
                /** @description The opaque cursor of the next page */
                cursor?: string | null;
                /** @description Text matched against the name and the target */
                q?: string | null;
                /** @description The current state, derived from the newest result */
                state?: components["schemas"]["CheckState"];
                /** @description The protocol to measure */
                type?: components["schemas"]["CheckType"];
                /** @description A tag the check must carry */
                tag?: string | null;
                /** @description The group name */
                group?: string | null;
                /** @description Why monitoring is paused */
                pausedReason?: components["schemas"]["PausedReason"];
            };
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CursorPageCheckResponse"];
                };
            };
            /** @description The cursor is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    createCheck: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CheckRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CheckResponse"];
                };
            };
            /** @description The request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The role does not allow changing checks in this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    bulkChecks: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["BulkChecksRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["BulkChecksResponse"];
                };
            };
            /** @description The request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The role does not allow changing checks in this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The number of matching checks changed since the dialog was shown. */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    countChecks: {
        parameters: {
            query?: {
                /** @description Text matched against the name and the target */
                q?: string | null;
                /** @description The current state, derived from the newest result */
                state?: components["schemas"]["CheckState"];
                /** @description The protocol to measure */
                type?: components["schemas"]["CheckType"];
                /** @description A tag the check must carry */
                tag?: string | null;
                /** @description The group name */
                group?: string | null;
                /** @description Why monitoring is paused */
                pausedReason?: components["schemas"]["PausedReason"];
            };
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["StateCountsResponse"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    getCheck: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CheckResponse"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    updateCheck: {
        parameters: {
            query: {
                version: number;
            };
            header?: never;
            path: {
                slug: string;
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CheckRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CheckResponse"];
                };
            };
            /** @description The request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The role does not allow changing checks in this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The check was changed by someone else. */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    deleteCheck: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The role does not allow changing checks in this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    pauseCheck: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PauseCheckRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CheckResponse"];
                };
            };
            /** @description The request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The role does not allow changing checks in this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    resumeCheck: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CheckResponse"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The role does not allow changing checks in this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    listSpaceMembers: {
        parameters: {
            query?: {
                /** @description The page size, at least 1; values above the instance maximum are capped. */
                limit?: number;
                cursor?: string;
            };
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CursorPageSpaceMemberResponse"];
                };
            };
            /** @description The limit or the cursor is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    addSpaceMember: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AddSpaceMemberRequest"];
            };
        };
        responses: {
            /** @description The membership already exists; an existing membership keeps its role. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SpaceMemberResponse"];
                };
            };
            /** @description The membership was created. */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SpaceMemberResponse"];
                };
            };
            /** @description The request contains invalid fields. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Access is denied. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    removeSpaceMember: {
        parameters: {
            query: {
                /** @description The OIDC issuer of the member */
                issuer: string;
                /** @description The OIDC subject of the member */
                subject: string;
            };
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The request contains invalid fields. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Access is denied. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    changeSpaceMemberRole: {
        parameters: {
            query: {
                /** @description The OIDC issuer of the member */
                issuer: string;
                /** @description The OIDC subject of the member */
                subject: string;
            };
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ChangeSpaceMemberRoleRequest"];
            };
        };
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The request contains invalid fields. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Access is denied. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    putSpacePlan: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PutSpacePlanRequest"];
            };
        };
        responses: {
            /** @description The plan was assigned. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The plan is not configured on this instance. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Only an administrator can assign a plan. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    getSpaceSettings: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CursorPageSpaceSettingResponse"];
                };
            };
            /** @description The space plan or a setting is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    resetSpaceSettings: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The overrides were removed. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller cannot change the settings of this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    putSpaceSetting: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                key: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["SetSpaceSettingRequest"];
            };
        };
        responses: {
            /** @description The setting was stored. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The value is outside the plan or has an invalid format. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller cannot change the settings of this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    deleteSpaceSetting: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                key: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The override was removed. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The setting key is unknown. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller cannot change the settings of this space. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    listTokens: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CursorPageTokenSummary"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    createToken: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateTokenRequest"];
            };
        };
        responses: {
            /** @description The token was created; the secret is returned only here. */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CreateTokenResponse"];
                };
            };
            /** @description The token request is invalid. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller is not allowed to create this token. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    revokeToken: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                slug: string;
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The token was revoked. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Authentication is required. */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The caller is not allowed to revoke this token. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested resource was not found. */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Too many requests. */
            429: {
                headers: {
                    /** @description Seconds to wait before retrying. */
                    "Retry-After"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
}
