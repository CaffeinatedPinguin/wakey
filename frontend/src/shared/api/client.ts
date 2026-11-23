import axios from 'axios'
import type { AxiosInstance } from 'axios'

const NETWORK_RETRY_LIMIT = 2

export interface ApiClientOptions {
  baseURL: string
}

// Axios 1.21 already defaults xsrfCookieName to 'XSRF-TOKEN' and
// xsrfHeaderName to 'X-XSRF-TOKEN' — the names Spring Security's csrf.spa()
// writes and expects (ADR 0006). Axios only adds the header to same-origin
// requests, so no withXSRFToken option is needed either.
export function createApiClient({ baseURL }: ApiClientOptions): AxiosInstance {
  return axios.create({
    baseURL,
    withCredentials: true,
  })
}

export function isUnauthorizedError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 401
}

export function shouldRetryRequest(failureCount: number, error: unknown): boolean {
  return isNetworkError(error) && failureCount < NETWORK_RETRY_LIMIT
}

function isNetworkError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response === undefined
}
