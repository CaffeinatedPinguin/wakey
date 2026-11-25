// This file is generated, do not edit.
// Source: api/openapi/public.json

export const ErrorCode = {
  BAD_REQUEST: 'bad_request',
  FORBIDDEN: 'forbidden',
  INTERNAL_ERROR: 'internal_error',
  INVALID_BODY: 'invalid_body',
  INVALID_CURSOR: 'invalid_cursor',
  INVALID_NAME: 'invalid_name',
  INVALID_SLUG: 'invalid_slug',
  INVALID_SUBJECT: 'invalid_subject',
  METHOD_NOT_ALLOWED: 'method_not_allowed',
  NOT_ACCEPTABLE: 'not_acceptable',
  NOT_FOUND: 'not_found',
  SLUG_TAKEN: 'slug_taken',
  UNAUTHENTICATED: 'unauthenticated',
  UNSUPPORTED_MEDIA_TYPE: 'unsupported_media_type',
  VALIDATION_FAILED: 'validation_failed',
  VERSION_CONFLICT: 'version_conflict',
} as const

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode]

export const SpaceRole = {
  EDITOR: 'EDITOR',
  VIEWER: 'VIEWER',
} as const

export type SpaceRole = (typeof SpaceRole)[keyof typeof SpaceRole]
