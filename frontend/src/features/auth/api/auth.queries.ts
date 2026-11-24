import { queryOptions, useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import type { QueryClient, UseMutationResult, UseSuspenseQueryResult } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { useCallback, useMemo } from 'react'
import { fetchMe, logout } from './auth.api'
import type { Me } from './auth.schemas'

export const meQueryKey = ['auth', 'me'] as const

export const meQueryOptions = queryOptions({
  queryKey: meQueryKey,
  queryFn: fetchMe,
})

export function useMe(): UseSuspenseQueryResult<Me> {
  return useSuspenseQuery(meQueryOptions)
}

interface LogoutLatch {
  inFlight: boolean
}

// One latch per QueryClient: every useLogout caller in the running app shares it,
// so a second click from another place cannot send a second sign-out request.
const logoutLatches = new WeakMap<QueryClient, LogoutLatch>()

function latchFor(queryClient: QueryClient): LogoutLatch {
  const existing = logoutLatches.get(queryClient)
  if (existing !== undefined) {
    return existing
  }
  const created: LogoutLatch = { inFlight: false }
  logoutLatches.set(queryClient, created)
  return created
}

export function useLogout(): UseMutationResult<void, Error, void> {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const mutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: meQueryKey })
      void navigate({ to: '/logged-out' })
    },
    onError: () => {
      latchFor(queryClient).inFlight = false
    },
  })
  const mutate: typeof mutation.mutate = useCallback(
    (variables, options) => {
      const latch = latchFor(queryClient)
      if (latch.inFlight) {
        return
      }
      latch.inFlight = true
      mutation.mutate(variables, options)
    },
    [mutation, queryClient],
  )
  return useMemo(() => ({ ...mutation, mutate }), [mutation, mutate])
}
