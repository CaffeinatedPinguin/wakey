import axios from 'axios'
import type { AxiosResponse } from 'axios'
import { z } from 'zod'
import { ErrorCode } from './generated/enums'
import type { ProblemDetail } from './generated/types'
import { HttpStatus } from './httpStatus'

// Codes the client itself produces; the server never sends them.
export const ClientErrorCode = {
  UNKNOWN: 'unknown',
  NETWORK: 'network',
} as const
export type ClientErrorCode = (typeof ClientErrorCode)[keyof typeof ClientErrorCode]

export type ApiExceptionCode = ErrorCode | ClientErrorCode

const KNOWN_ERROR_CODES: readonly string[] = Object.values(ErrorCode)

function isErrorCode(value: string): value is ErrorCode {
  return KNOWN_ERROR_CODES.includes(value)
}

function errorCodeOf(value: string): ApiExceptionCode {
  return isErrorCode(value) ? value : ClientErrorCode.UNKNOWN
}

// RFC 9457 error body as the server sends it (ADR 0006): the standard fields
// plus the `code` and `errors[]` extensions. The schema is checked against the
// generated contract type, so a field drift breaks compilation.
export const apiFieldErrorSchema = z.object({
  pointer: z.string(),
  code: z.string(),
  params: z.record(z.string(), z.unknown()).default({}),
})

// Defaults keep parsing tolerant of a partial body while giving Zod an output
// without `| undefined`, which `satisfies` needs under exactOptionalPropertyTypes.
// `code` stays a plain string here: the generated type lists the codes this
// client knows, but a newer server may send one it does not.
export const apiExceptionSchema = z.object({
  title: z.string().default(''),
  status: z.number().int().default(0),
  detail: z.string().default(''),
  code: z.string().default(ClientErrorCode.UNKNOWN),
  params: z.record(z.string(), z.unknown()).default({}),
  errors: z.array(apiFieldErrorSchema).default([]),
}) satisfies z.ZodType<Omit<ProblemDetail, 'code'> & { code: string }>

export type ApiFieldError = z.infer<typeof apiFieldErrorSchema>

export interface ApiExceptionInit {
  status?: number | undefined
  code: ApiExceptionCode
  params?: Readonly<Record<string, unknown>> | undefined
  errors?: ApiFieldError[] | undefined
  detail?: string | undefined
}

// The one exception application code sees for a failed API call. `code` is the
// stable machine code the screens translate; `params` carries the extension
// values (for example a plan bound); `errors` describes invalid fields.
export class ApiException extends Error {
  readonly status: number | undefined
  readonly code: ApiExceptionCode
  readonly params: Readonly<Record<string, unknown>>
  readonly errors: ApiFieldError[]
  readonly detail: string | undefined

  constructor({ status, code, params = {}, errors = [], detail }: ApiExceptionInit) {
    super(`API request failed with code ${code}`)
    this.name = 'ApiException'
    this.status = status
    this.code = code
    this.params = params
    this.errors = errors
    this.detail = detail
  }
}

function statusOf(error: unknown): number | undefined {
  if (error instanceof ApiException) {
    return error.status
  }
  if (axios.isAxiosError<unknown>(error)) {
    return error.response?.status
  }
  return undefined
}

export function isUnauthorizedException(error: unknown): boolean {
  return statusOf(error) === HttpStatus.UNAUTHORIZED
}

export function isNotFoundException(error: unknown): boolean {
  return statusOf(error) === HttpStatus.NOT_FOUND
}

function parseApiException(body: unknown): z.infer<typeof apiExceptionSchema> | undefined {
  const result = apiExceptionSchema.safeParse(body)
  return result.success ? result.data : undefined
}

function unknownException(): ApiException {
  return new ApiException({ code: ClientErrorCode.UNKNOWN })
}

function networkException(): ApiException {
  return new ApiException({ code: ClientErrorCode.NETWORK })
}

function responseException(response: AxiosResponse<unknown>): ApiException {
  const errorBody = parseApiException(response.data)
  return new ApiException({
    status: response.status,
    code: errorCodeOf(errorBody?.code ?? ClientErrorCode.UNKNOWN),
    params: errorBody?.params ?? {},
    errors: errorBody?.errors ?? [],
    detail: errorBody?.detail,
  })
}

// Normalises any failure into one ApiException. A response without a valid RFC
// 9457 error body keeps the HTTP status but reports the `unknown` code;
// transport failures report `network`.
export function toApiException(error: unknown): ApiException {
  if (error instanceof ApiException) {
    return error
  }
  if (!axios.isAxiosError<unknown>(error)) {
    return unknownException()
  }
  const response = error.response
  if (response === undefined) {
    return networkException()
  }
  return responseException(response)
}
