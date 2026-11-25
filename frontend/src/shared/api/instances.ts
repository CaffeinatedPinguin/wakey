import { createApiClient } from './client'

// API calls use paths from the contract; routes outside it (sign-out) use the
// origin. One axios instance keeps the interceptors in a single place.
export const serverClient = createApiClient({ baseURL: '' })

// Kept for the cross-origin CSRF test, which imports this name; it is the same
// client, so there is no second base URL to drift from the contract paths.
export { serverClient as apiClient }
