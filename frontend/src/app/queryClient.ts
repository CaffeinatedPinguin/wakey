import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { meQueryKey } from '../features/auth'
import { isUnauthorizedError, shouldRetryRequest } from '../shared/api/client'

export type ExpiredSessionHandler = () => void

// The `me` query is the single source of session state. A 401 from any request
// drops it from the cache and sends the user back to the login screen through
// the router; no component has its own expired-session effect.
export function createQueryClient(onExpired: ExpiredSessionHandler = () => undefined): QueryClient {
  // One expiry at a time: removing the `me` query makes its observer refetch it,
  // so an unguarded handler would keep removing and refetching a 401 response.
  let sessionExpired = false
  const handleError = (error: unknown): void => {
    if (!isUnauthorizedError(error) || sessionExpired) {
      return
    }
    sessionExpired = true
    queryClient.removeQueries({ queryKey: meQueryKey })
    onExpired()
  }

  const queryClient = new QueryClient({
    queryCache: new QueryCache({
      onError: handleError,
      onSuccess: (_data, query) => {
        if (query.queryKey[0] === meQueryKey[0] && query.queryKey[1] === meQueryKey[1]) {
          sessionExpired = false
        }
      },
    }),
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
