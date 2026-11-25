import axios from 'axios'
import { z } from 'zod'
import type { ProblemDetail } from './generated/types'

// RFC 9457 Problem Details as the server sends them (ADR 0006): the standard
// fields plus the `code`, `params` and `errors[]` extensions. The schema is
// checked against the generated contract type, so a field drift breaks
// compilation.
export const problemFieldSchema = z.object({
  pointer: z.string(),
  code: z.string(),
  params: z.record(z.string(), z.unknown()).default({}),
})

// Defaults keep parsing tolerant of a partial body while giving Zod an output
// without `| undefined`, which `satisfies` needs under exactOptionalPropertyTypes.
export const problemSchema = z.object({
  title: z.string().default(''),
  status: z.number().int().default(0),
  detail: z.string().default(''),
  code: z.string().default('unknown'),
  params: z.record(z.string(), z.unknown()).default({}),
  errors: z.array(problemFieldSchema).default([]),
}) satisfies z.ZodType<ProblemDetail>

export type ProblemField = z.infer<typeof problemFieldSchema>

export interface ApiErrorInit {
  status?: number | undefined
  code: string
  params?: Readonly<Record<string, unknown>> | undefined
  errors?: ProblemField[] | undefined
  detail?: string | undefined
}

// The one error type application code sees for a failed API call. `code` is the
// stable machine code the screens translate; `params` carries the extension
// values (for example a plan bound); `errors` describes invalid fields.
export class ApiError extends Error {
  readonly status: number | undefined
  readonly code: string
  readonly params: Readonly<Record<string, unknown>>
  readonly errors: ProblemField[]
  readonly detail: string | undefined

  constructor({ status, code, params = {}, errors = [], detail }: ApiErrorInit) {
    super(`API request failed with code ${code}`)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.params = params
    this.errors = errors
    this.detail = detail
  }
}

function statusOf(error: unknown): number | undefined {
  if (error instanceof ApiError) {
    return error.status
  }
  if (axios.isAxiosError<unknown>(error)) {
    return error.response?.status
  }
  return undefined
}

export function isUnauthorizedError(error: unknown): boolean {
  return statusOf(error) === 401
}

export function isNotFoundError(error: unknown): boolean {
  return statusOf(error) === 404
}

function parseProblem(body: unknown): z.infer<typeof problemSchema> | undefined {
  const result = problemSchema.safeParse(body)
  return result.success ? result.data : undefined
}

function errorFromResponse(status: number, data: unknown): ApiError {
  const problem = parseProblem(data)
  return new ApiError({
    status,
    code: problem?.code ?? 'unknown',
    params: problem?.params ?? {},
    errors: problem?.errors ?? [],
    detail: problem?.detail,
  })
}

// Normalises any failure into one ApiError. A response without a valid Problem
// Details body keeps the HTTP status but reports the `unknown` code; transport
// failures report `network`.
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (!axios.isAxiosError<unknown>(error)) {
    return new ApiError({ code: 'unknown' })
  }
  if (error.response === undefined) {
    return new ApiError({ code: 'network' })
  }
  return errorFromResponse(error.response.status, error.response.data)
}
