import { z } from 'zod'
import type {
  CountChecksResponse,
  GetCheckResponse,
  ListChecksResponse,
} from '../../../shared/api/generated/types'

// Zod gives every optional field the type `T | undefined`, while the generated
// contract under exactOptionalPropertyTypes keeps `T` on an optional property.
// This mirrors the contract shape so `satisfies z.ZodType<...>` still checks it.
type OptionalKeys<T> = {
  [K in keyof T]-?: object extends Pick<T, K> ? K : never
}[keyof T]

type RequiredKeys<T> = Exclude<keyof T, OptionalKeys<T>>

type ApiShape<T> = T extends readonly (infer U)[]
  ? ApiShape<U>[]
  : T extends object
    ? { [K in RequiredKeys<T>]: ApiShape<T[K]> } & {
        [K in OptionalKeys<T>]?: ApiShape<T[K]> | undefined
      }
    : T

export const checkStateSchema = z.enum(['UP', 'DEGRADED', 'DOWN', 'UNKNOWN'])

export const checkTypeSchema = z.enum(['HTTP', 'TCP', 'CERTIFICATE'])

export const pausedReasonSchema = z.enum(['MANUAL', 'PLAN_LIMIT', 'ABANDONED', 'UNVERIFIED'])

export const checkResultSchema = z.object({
  checkedAt: z.string(),
  durationMs: z.number().int().optional(),
  errorCategory: z.enum(['TARGET_DOWN', 'LOCATION_NETWORK', 'AUTH']).optional(),
  httpStatus: z.number().int().optional(),
  locationId: z.string().optional(),
  phases: z.string().optional(),
  protocolVersion: z.string().optional(),
  state: checkStateSchema,
}) satisfies z.ZodType<ApiShape<NonNullable<GetCheckResponse['lastResult']>>>

export const checkSchema = z.object({
  id: z.string(),
  name: z.string(),
  type: checkTypeSchema,
  target: z.string(),
  intervalSeconds: z.number().int(),
  timeoutSeconds: z.number().int(),
  retries: z.number().int(),
  groupName: z.string().optional(),
  description: z.string().optional(),
  runbookUrl: z.string().optional(),
  paused: z.boolean(),
  pausedReason: pausedReasonSchema.optional(),
  tags: z.array(z.string()),
  state: checkStateSchema,
  version: z.number().int(),
  createdAt: z.string(),
  updatedAt: z.string(),
  lastResult: checkResultSchema.optional(),
}) satisfies z.ZodType<ApiShape<GetCheckResponse>>

// Every field in the contract is required; nextCursor is explicitly nullable.
export const checksPageSchema = z.object({
  items: z.array(checkSchema),
  nextCursor: z.string().nullable(),
}) satisfies z.ZodType<ApiShape<ListChecksResponse>>

export const stateCountsSchema = z.object({
  up: z.number().int(),
  degraded: z.number().int(),
  down: z.number().int(),
  unknown: z.number().int(),
}) satisfies z.ZodType<ApiShape<CountChecksResponse>>

export type CheckRecord = z.infer<typeof checkSchema>
export type CheckPage = z.infer<typeof checksPageSchema>
export type StateCounts = z.infer<typeof stateCountsSchema>
export type CheckState = z.infer<typeof checkStateSchema>
export type CheckType = z.infer<typeof checkTypeSchema>
export type PausedReason = z.infer<typeof pausedReasonSchema>
