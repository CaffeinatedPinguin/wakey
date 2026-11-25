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
         * @description A rename is last-write-wins unless an If-Match precondition is sent. Send If-None-Match: * to create the space only when the slug is free, and If-Match: "<version>" to rename only the version the caller read.
         */
        put: operations["putSpace"];
        post?: never;
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
        CurrentUser: {
            /** @description Whether the user is an instance administrator */
            admin: boolean;
            /** @description The OIDC subject of the authenticated user */
            subject: string;
        };
        CursorPageSpaceResponse: {
            /** @description The items on this page */
            items: components["schemas"]["SpaceResponse"][];
            /** @description The cursor of the next page, or null on the last page */
            nextCursor: string | null;
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
                    "application/json": components["schemas"]["CurrentUser"];
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
            /** @description The cursor or the limit is invalid. */
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
                /** @description Send * to create the space only when the slug is free. */
                "If-None-Match"?: string;
                /** @description The quoted space version to rename, for example "1". */
                "If-Match"?: string;
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
            /** @description The request is invalid or a precondition header is malformed. */
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
            /** @description The slug is taken or the If-Match version is stale. */
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
}
