import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { ApiError, ClientErrorCode, toApiError } from './apiError'

export { isNotFoundError, isUnauthorizedError } from './apiError'

const NETWORK_RETRY_LIMIT = 2

export interface ApiClientOptions {
  baseURL: string
}

// Axios 1.21 already defaults xsrfCookieName to 'XSRF-TOKEN' and
// xsrfHeaderName to 'X-XSRF-TOKEN' — the names Spring Security's csrf.spa()
// writes and expects (ADR 0006). Axios only adds the header to same-origin
// requests, so no withXSRFToken option is needed either.
export function createApiClient({ baseURL }: ApiClientOptions): AxiosInstance {
  const client = axios.create({
    baseURL,
    withCredentials: true,
  })
  client.interceptors.response.use(
    (response) => response,
    (error: unknown) => Promise.reject(toResponseError(error)),
  )
  return client
}

// A cancelled request is not a failure the UI should report, so it keeps its
// original cancellation error; everything else becomes one ApiError.
function toResponseError(error: unknown): Error {
  if (axios.isCancel(error) && error instanceof Error) {
    return error
  }
  return toApiError(error)
}

export function shouldRetryRequest(failureCount: number, error: unknown): boolean {
  return isNetworkError(error) && failureCount < NETWORK_RETRY_LIMIT
}

function isNetworkError(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.code === ClientErrorCode.NETWORK
  }
  return axios.isAxiosError(error) && error.response === undefined
}
