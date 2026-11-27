// This file is generated, do not edit.
// Source: api/openapi/public.json

export const BulkCheckAction = {
  DELETE: 'DELETE',
  PAUSE: 'PAUSE',
  RESUME: 'RESUME',
} as const

export type BulkCheckAction = (typeof BulkCheckAction)[keyof typeof BulkCheckAction]

export const CheckState = {
  DEGRADED: 'DEGRADED',
  DOWN: 'DOWN',
  UNKNOWN: 'UNKNOWN',
  UP: 'UP',
} as const

export type CheckState = (typeof CheckState)[keyof typeof CheckState]

export const CheckType = {
  CERTIFICATE: 'CERTIFICATE',
  HTTP: 'HTTP',
  TCP: 'TCP',
} as const

export type CheckType = (typeof CheckType)[keyof typeof CheckType]

export const ErrorCategory = {
  AUTH: 'AUTH',
  LOCATION_NETWORK: 'LOCATION_NETWORK',
  TARGET_DOWN: 'TARGET_DOWN',
} as const

export type ErrorCategory = (typeof ErrorCategory)[keyof typeof ErrorCategory]

export const ErrorCode = {
  BAD_REQUEST: 'bad_request',
  BULK_COUNT_CHANGED: 'bulk_count_changed',
  FORBIDDEN: 'forbidden',
  INSUFFICIENT_SCOPE: 'insufficient_scope',
  INTERNAL_ERROR: 'internal_error',
  INVALID_BODY: 'invalid_body',
  INVALID_CURSOR: 'invalid_cursor',
  INVALID_DESCRIPTION: 'invalid_description',
  INVALID_EXPIRY: 'invalid_expiry',
  INVALID_GROUP_NAME: 'invalid_group_name',
  INVALID_ISSUER: 'invalid_issuer',
  INVALID_KEY: 'invalid_key',
  INVALID_NAME: 'invalid_name',
  INVALID_RUNBOOK_URL: 'invalid_runbook_url',
  INVALID_SLUG: 'invalid_slug',
  INVALID_SUBJECT: 'invalid_subject',
  INVALID_TAGS: 'invalid_tags',
  INVALID_TARGET: 'invalid_target',
  INVALID_TIMEOUT: 'invalid_timeout',
  INVALID_TOKEN: 'invalid_token',
  INVALID_VALUE: 'invalid_value',
  LOCATION_ERROR_CANNOT_BE_DOWN: 'location_error_cannot_be_down',
  METHOD_NOT_ALLOWED: 'method_not_allowed',
  NOT_ACCEPTABLE: 'not_acceptable',
  NOT_FOUND: 'not_found',
  OUT_OF_PLAN: 'out_of_plan',
  PLAN_LIMIT: 'plan_limit',
  SLUG_TAKEN: 'slug_taken',
  TIMEOUT_EXCEEDS_INTERVAL: 'timeout_exceeds_interval',
  TOKEN_LIMIT: 'token_limit',
  UNAUTHENTICATED: 'unauthenticated',
  UNKNOWN_PLAN: 'unknown_plan',
  UNSUPPORTED_MEDIA_TYPE: 'unsupported_media_type',
  VALIDATION_FAILED: 'validation_failed',
  VERSION_CONFLICT: 'version_conflict',
} as const

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode]

export const Language = {
  EN: 'en',
  PL: 'pl',
} as const

export type Language = (typeof Language)[keyof typeof Language]

export const PausedReason = {
  ABANDONED: 'ABANDONED',
  MANUAL: 'MANUAL',
  PLAN_LIMIT: 'PLAN_LIMIT',
  UNVERIFIED: 'UNVERIFIED',
} as const

export type PausedReason = (typeof PausedReason)[keyof typeof PausedReason]

export const SettingGroup = {
  DEFAULTS: 'DEFAULTS',
  GENERAL: 'GENERAL',
} as const

export type SettingGroup = (typeof SettingGroup)[keyof typeof SettingGroup]

export const SettingSource = {
  INSTANCE: 'INSTANCE',
  PLAN: 'PLAN',
  SPACE: 'SPACE',
} as const

export type SettingSource = (typeof SettingSource)[keyof typeof SettingSource]

export const SettingType = {
  ENUM: 'ENUM',
  INTEGER: 'INTEGER',
  STRING: 'STRING',
} as const

export type SettingType = (typeof SettingType)[keyof typeof SettingType]

export const SpaceRole = {
  EDITOR: 'EDITOR',
  VIEWER: 'VIEWER',
} as const

export type SpaceRole = (typeof SpaceRole)[keyof typeof SpaceRole]

export const TokenScope = {
  READ: 'READ',
  WRITE: 'WRITE',
} as const

export type TokenScope = (typeof TokenScope)[keyof typeof TokenScope]
