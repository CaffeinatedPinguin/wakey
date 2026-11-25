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
    "/api/v1/spaces": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** List the spaces visible to the caller */
        get: operations["listSpaces"];
        put?: never;
        post?: never;
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
        /** Read one space visible to the caller */
        get: operations["getSpace"];
        /**
         * Create or rename a space idempotently
         * @description A rename is last-write-wins until the space carries a version or an If-Match precondition. Send If-None-Match: * to create the space only when the slug is free.
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
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
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
            /**
             * @description Why monitoring is paused, if it is
             * @enum {string}
             */
            pausedReason?: "MANUAL" | "PLAN_LIMIT" | "ABANDONED" | "UNVERIFIED";
            /** Format: int32 */
            retries: number;
            /** @description The optional runbook address */
            runbookUrl?: string;
            /** @enum {string} */
            state: "UP" | "DEGRADED" | "DOWN" | "UNKNOWN";
            tags: string[];
            target: string;
            /** Format: int32 */
            timeoutSeconds: number;
            /** @enum {string} */
            type: "HTTP" | "TCP" | "CERTIFICATE";
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
            /**
             * @description Why a failed measurement failed
             * @enum {string}
             */
            errorCategory?: "TARGET_DOWN" | "LOCATION_NETWORK" | "AUTH";
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
            /**
             * @description The state of the measurement
             * @enum {string}
             */
            state: "UP" | "DEGRADED" | "DOWN" | "UNKNOWN";
        };
        /** @description The desired state of a new check */
        CreateCheckRequest: {
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
             * @enum {string}
             */
            type: "HTTP" | "TCP" | "CERTIFICATE";
        };
        CurrentUser: {
            /** @description Whether the user is an instance administrator */
            admin: boolean;
            /** @description The OIDC subject of the authenticated user */
            subject: string;
        };
        CursorPageCheckResponse: {
            /** @description The items on this page */
            items: components["schemas"]["CheckResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        CursorPageSpaceResponse: {
            /** @description The items on this page */
            items: components["schemas"]["SpaceResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
        };
        /** @description The reason to pause monitoring */
        PauseCheckRequest: {
            /**
             * @description The pause reason
             * @example MANUAL
             * @enum {string}
             */
            reason: "MANUAL" | "PLAN_LIMIT" | "ABANDONED" | "UNVERIFIED";
        };
        ProblemDetail: {
            code?: string;
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
            /** Format: int32 */
            status?: number;
            title?: string;
            /** Format: uri */
            type?: string;
        };
        /** @description The desired state of a space */
        PutSpaceRequest: {
            /**
             * @description The display name, 1-100 characters after trimming
             * @example Acme
             */
            name: string;
        };
        /** @description A space as seen by the caller */
        SpaceResponse: {
            /** @description Whether the caller is an application administrator */
            admin: boolean;
            /**
             * @description The display name
             * @example Acme
             */
            name: string;
            /**
             * @description The caller's membership role, or null for an Admin who is not a member
             * @enum {string|null}
             */
            role: "EDITOR" | "VIEWER" | null;
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
        /** @description The replacement values of a check */
        UpdateCheckRequest: {
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
             * @enum {string}
             */
            type: "HTTP" | "TCP" | "CERTIFICATE";
            /**
             * Format: int64
             * @description The version the caller last read
             */
            version?: number;
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
                    "*/*": components["schemas"]["CurrentUser"];
                };
            };
        };
    };
    listSpaces: {
        parameters: {
            query?: {
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
            /** @description The space was not found. */
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
    putSpace: {
        parameters: {
            query?: never;
            header?: {
                "If-None-Match"?: string;
            };
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
            /** @description The caller is not allowed to create or rename spaces. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The space already exists and If-None-Match: * was sent. */
            409: {
                headers: {
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
                state?: "UP" | "DEGRADED" | "DOWN" | "UNKNOWN" | null;
                /** @description The protocol to measure */
                type?: "HTTP" | "TCP" | "CERTIFICATE" | null;
                /** @description A tag the check must carry */
                tag?: string | null;
                /** @description The group name */
                group?: string | null;
                /** @description Why monitoring is paused */
                pausedReason?: "MANUAL" | "PLAN_LIMIT" | "ABANDONED" | "UNVERIFIED" | null;
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
            /** @description The space was not found. */
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
                "application/json": components["schemas"]["CreateCheckRequest"];
            };
        };
        responses: {
            /** @description The check was created. */
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
            /** @description The space was not found. */
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
    countChecks: {
        parameters: {
            query?: {
                /** @description Text matched against the name and the target */
                q?: string | null;
                /** @description The current state, derived from the newest result */
                state?: "UP" | "DEGRADED" | "DOWN" | "UNKNOWN" | null;
                /** @description The protocol to measure */
                type?: "HTTP" | "TCP" | "CERTIFICATE" | null;
                /** @description A tag the check must carry */
                tag?: string | null;
                /** @description The group name */
                group?: string | null;
                /** @description Why monitoring is paused */
                pausedReason?: "MANUAL" | "PLAN_LIMIT" | "ABANDONED" | "UNVERIFIED" | null;
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
            /** @description The space was not found. */
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
            /** @description The space or check was not found. */
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
    updateCheck: {
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
                "application/json": components["schemas"]["UpdateCheckRequest"];
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
            /** @description The space or check was not found. */
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
            /** @description The check was deleted. */
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
            /** @description The space or check was not found. */
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
            /** @description The space or check was not found. */
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
            /** @description The space or check was not found. */
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
}
