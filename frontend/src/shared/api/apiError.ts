import axios from 'axios'
import { z } from 'zod'
import { ErrorCode } from './generated/enums'
import type { ProblemDetail } from './generated/types'

// Codes the client itself produces; the server never sends them.
export const ClientErrorCode = {
  UNKNOWN: 'unknown',
  NETWORK: 'network',
} as const
export type ClientErrorCode = (typeof ClientErrorCode)[keyof typeof ClientErrorCode]

export type ApiErrorCode = ErrorCode | ClientErrorCode

const KNOWN_ERROR_CODES: readonly string[] = Object.values(ErrorCode)

function isErrorCode(value: string): value is ErrorCode {
  return KNOWN_ERROR_CODES.includes(value)
}

function errorCodeOf(value: string): ApiErrorCode {
  return isErrorCode(value) ? value : ClientErrorCode.UNKNOWN
}

// RFC 9457 Problem Details as the server sends them (ADR 0006): the standard
// fields plus the `code` and `errors[]` extensions. The schema is checked
// against the generated contract type, so a field drift breaks compilation.
export const problemFieldSchema = z.object({
  pointer: z.string(),
  code: z.string(),
  params: z.record(z.string(), z.unknown()).default({}),
})

// Defaults keep parsing tolerant of a partial body while giving Zod an output
// without `| undefined`, which `satisfies` needs under exactOptionalPropertyTypes.
// `code` stays a plain string here: the generated type lists the codes this
// client knows, but a newer server may send one it does not.
export const problemDetailSchema = z.object({
  title: z.string().default(''),
  status: z.number().int().default(0),
  detail: z.string().default(''),
  code: z.string().default(ClientErrorCode.UNKNOWN),
  errors: z.array(problemFieldSchema).default([]),
}) satisfies z.ZodType<Omit<ProblemDetail, 'code'> & { code: string }>

export type ProblemField = z.infer<typeof problemFieldSchema>

export interface ApiErrorInit {
  status?: number | undefined
  code: ApiErrorCode
  errors?: ProblemField[] | undefined
  detail?: string | undefined
}

// The one error type application code sees for a failed API call. `code` is the
// stable machine code the screens translate; `errors` describes invalid fields.
export class ApiError extends Error {
  readonly status: number | undefined
  readonly code: ApiErrorCode
  readonly errors: ProblemField[]
  readonly detail: string | undefined

  constructor({ status, code, errors = [], detail }: ApiErrorInit) {
    super(`API request failed with code ${code}`)
    this.name = 'ApiError'
    this.status = status
    this.code = code
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

function parseProblem(body: unknown): z.infer<typeof problemDetailSchema> | undefined {
  const result = problemDetailSchema.safeParse(body)
  return result.success ? result.data : undefined
}

// Normalises any failure into one ApiError. A response without a valid Problem
// Details body keeps the HTTP status but reports the `unknown` code; transport
// failures report `network`.
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (!axios.isAxiosError<unknown>(error)) {
    return new ApiError({ code: ClientErrorCode.UNKNOWN })
  }
  if (error.response === undefined) {
    return new ApiError({ code: ClientErrorCode.NETWORK })
  }
  const problem = parseProblem(error.response.data)
  return new ApiError({
    status: error.response.status,
    code: errorCodeOf(problem?.code ?? ClientErrorCode.UNKNOWN),
    errors: problem?.errors ?? [],
    detail: problem?.detail,
  })
}
