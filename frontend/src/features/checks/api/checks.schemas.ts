import { z } from 'zod'
import {
  CheckState,
  CheckType,
  ErrorCategory,
  PausedReason,
} from '../../../shared/api/generated/enums'
import type {
  BulkChecksResponse,
  CountChecksResponse,
  GetCheckResponse,
  ListChecksResponse,
} from '../../../shared/api/generated/types'
import type { ApiShape } from '../../../shared/api/apiShape'

export const checkStateSchema = z.enum(Object.values(CheckState))

export const checkTypeSchema = z.enum(Object.values(CheckType))

export const pausedReasonSchema = z.enum(Object.values(PausedReason))

export const errorCategorySchema = z.enum(Object.values(ErrorCategory))

export const checkResultSchema = z.object({
  checkedAt: z.string(),
  durationMs: z.number().int().optional(),
  errorCategory: errorCategorySchema.optional(),
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

export const bulkChecksResponseSchema = z.object({
  affected: z.number().int(),
}) satisfies z.ZodType<ApiShape<BulkChecksResponse>>

export type CheckRecord = z.infer<typeof checkSchema>
export type CheckPage = z.infer<typeof checksPageSchema>
export type StateCounts = z.infer<typeof stateCountsSchema>
export type BulkChecksResult = z.infer<typeof bulkChecksResponseSchema>
