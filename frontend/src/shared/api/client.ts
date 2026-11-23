import axios from 'axios'

const API_PREFIX = '/api/v1/'
const SESSION_EXPIRED_EVENT = 'wakey:session-expired'
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'

// axios 1.21 reads the XSRF cookie and sets the header itself. withXSRFToken
// stays undefined on purpose: axios then only attaches the token to
// same-origin requests, never to an absolute URL of another origin.
export const apiClient = axios.create({
  withCredentials: true,
  xsrfCookieName: CSRF_COOKIE_NAME,
  xsrfHeaderName: CSRF_HEADER_NAME,
})

let sessionExpiredScheduled = false

function notifySessionExpired(): void {
  if (sessionExpiredScheduled) {
    return
  }
  sessionExpiredScheduled = true
  queueMicrotask(() => {
    sessionExpiredScheduled = false
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  })
}

export function onSessionExpired(handler: () => void): () => void {
  const listener = (): void => {
    handler()
  }
  window.addEventListener(SESSION_EXPIRED_EVENT, listener)
  return () => {
    window.removeEventListener(SESSION_EXPIRED_EVENT, listener)
  }
}

export function isUnauthorizedError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 401
}

function isApiRequest(url: string | undefined): boolean {
  return url === '/api/v1' || url?.startsWith(API_PREFIX) === true
}

function isUnauthorizedApiError(error: unknown): boolean {
  return (
    axios.isAxiosError(error) && error.response?.status === 401 && isApiRequest(error.config?.url)
  )
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (isUnauthorizedApiError(error)) {
      notifySessionExpired()
    }
    return Promise.reject(error instanceof Error ? error : new Error('Request failed'))
  },
)
