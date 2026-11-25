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
            /** @enum {string} */
            code?: "bad_request" | "validation_failed" | "invalid_body" | "invalid_cursor" | "unauthenticated" | "forbidden" | "not_found" | "method_not_allowed" | "not_acceptable" | "unsupported_media_type" | "internal_error" | "slug_taken" | "version_conflict" | "invalid_name" | "invalid_slug" | "invalid_subject";
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
            /**
             * Format: int64
             * @description The version the caller read; omit to rename without a condition
             * @example 1
             */
            version?: number;
        };
        /** @description A space as seen by the caller */
        SpaceResponse: {
            /**
             * @description The display name
             * @example Acme
             */
            name: string;
            /** @description The caller's membership role, or null for an Admin who is not a member */
            role: components["schemas"]["SpaceRole"];
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
        /** @enum {string|null} */
        SpaceRole: "EDITOR" | "VIEWER" | null;
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
                    "*/*": components["schemas"]["CursorPageSpaceResponse"];
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
}
