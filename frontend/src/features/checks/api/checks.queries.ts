import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type {
  InfiniteData,
  UseInfiniteQueryResult,
  UseMutationResult,
  UseQueryResult,
} from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import {
  deleteCheck,
  fetchCheck,
  fetchCheckCounts,
  fetchChecks,
  pauseCheck,
  resumeCheck,
} from './checks.api'
import type { CheckApiFilters } from './checks.api'
import type { CheckPage, CheckRecord, StateCounts } from './checks.schemas'

export interface CheckMutationVariables {
  slug: string
  id: string
}

export const checksQueryKeys = {
  all: ['checks'] as const,
  list: (slug: string, filters: CheckApiFilters) => ['checks', 'list', slug, filters] as const,
  counts: (slug: string, filters: CheckApiFilters) => ['checks', 'counts', slug, filters] as const,
  detail: (slug: string, id: string) => ['checks', 'detail', slug, id] as const,
}

function invalidateChecks(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: checksQueryKeys.all })
}

export function useChecksList(
  slug: string,
  filters: CheckApiFilters,
): UseInfiniteQueryResult<InfiniteData<CheckPage>> {
  return useInfiniteQuery({
    queryKey: checksQueryKeys.list(slug, filters),
    queryFn: ({ pageParam, signal }) => fetchChecks(slug, filters, pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
  })
}

export function useCheckCounts(
  slug: string,
  filters: CheckApiFilters,
): UseQueryResult<StateCounts> {
  return useQuery({
    queryKey: checksQueryKeys.counts(slug, filters),
    queryFn: ({ signal }) => fetchCheckCounts(slug, filters, signal),
  })
}

export function useCheck(slug: string, id: string): UseQueryResult<CheckRecord> {
  return useQuery({
    queryKey: checksQueryKeys.detail(slug, id),
    queryFn: ({ signal }) => fetchCheck(slug, id, signal),
  })
}

export function usePauseCheck(): UseMutationResult<CheckRecord, Error, CheckMutationVariables> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: CheckMutationVariables) => pauseCheck(variables.slug, variables.id),
    onSuccess: () => {
      invalidateChecks(queryClient)
    },
  })
}

export function useResumeCheck(): UseMutationResult<CheckRecord, Error, CheckMutationVariables> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: CheckMutationVariables) => resumeCheck(variables.slug, variables.id),
    onSuccess: () => {
      invalidateChecks(queryClient)
    },
  })
}

export function useDeleteCheck(): UseMutationResult<void, Error, CheckMutationVariables> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (variables: CheckMutationVariables) => deleteCheck(variables.slug, variables.id),
    onSuccess: () => {
      invalidateChecks(queryClient)
    },
  })
}
