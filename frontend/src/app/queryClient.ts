import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { meQueryKey } from '../features/auth'
import { isUnauthorizedError, shouldRetryRequest } from '../shared/api/client'

export type ExpiredSessionHandler = () => void

// The `me` query is the single source of session state. A 401 from any request
// drops it from the cache and sends the user back to the login screen through
// the router; no component has its own expired-session effect.
export function createQueryClient(onExpired: ExpiredSessionHandler = () => undefined): QueryClient {
  const handleError = (error: unknown): void => {
    if (!isUnauthorizedError(error)) {
      return
    }
    queryClient.removeQueries({ queryKey: meQueryKey })
    onExpired()
  }

  const queryClient = new QueryClient({
    queryCache: new QueryCache({ onError: handleError }),
    mutationCache: new MutationCache({ onError: handleError }),
    defaultOptions: {
      queries: {
        retry: shouldRetryRequest,
        refetchOnWindowFocus: false,
      },
    },
  })

  return queryClient
}
