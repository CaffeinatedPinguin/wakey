import { createApiClient } from './client'

// API calls use paths from the contract; routes outside it (sign-out) use the
// origin. One axios instance keeps the interceptors in a single place.
export const serverClient = createApiClient({ baseURL: '' })
