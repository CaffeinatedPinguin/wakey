import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query'
import type { UseMutationResult } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { fetchMe, logout } from './auth.api'

export const meQueryKey = ['auth', 'me'] as const

export const meQueryOptions = queryOptions({
  queryKey: meQueryKey,
  queryFn: fetchMe,
})

export function useLogout(): UseMutationResult<void, Error, void> {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: meQueryKey })
      void navigate({ to: '/logged-out' })
    },
  })
}
