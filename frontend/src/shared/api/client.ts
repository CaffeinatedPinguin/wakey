import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { ApiException, ClientErrorCode, toApiException } from './apiException'

export { isNotFoundException, isUnauthorizedException } from './apiException'

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
// original cancellation error; everything else becomes one ApiException.
function toResponseError(error: unknown): Error {
  if (axios.isCancel(error) && error instanceof Error) {
    return error
  }
  return toApiException(error)
}

export function shouldRetryRequest(failureCount: number, error: unknown): boolean {
  return isNetworkException(error) && failureCount < NETWORK_RETRY_LIMIT
}

function isNetworkException(error: unknown): boolean {
  if (error instanceof ApiException) {
    return error.code === ClientErrorCode.NETWORK
  }
  return axios.isAxiosError(error) && error.response === undefined
}
