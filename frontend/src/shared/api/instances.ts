import { appConfig } from '../config'
import { createApiClient } from './client'

// The single place that turns configuration into HTTP clients. API calls use
// the configured prefix; server routes outside it (sign-out) use the origin.
export const apiClient = createApiClient({ baseURL: appConfig.apiBaseUrl })
export const serverClient = createApiClient({ baseURL: '' })
