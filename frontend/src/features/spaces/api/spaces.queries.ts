import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type {
  InfiniteData,
  UseInfiniteQueryResult,
  UseMutationResult,
  UseQueryResult,
} from '@tanstack/react-query'
import type { SpaceSummary } from '../spaceTypes'
import { createSpace, fetchSpace, fetchSpaces } from './spaces.api'
import type { CreateSpaceInput, SpacesPage } from './spaces.api'

export const spacesQueryKeys = {
  list: ['spaces', 'list'] as const,
  detail: (slug: string) => ['spaces', 'detail', slug] as const,
}

export function useSpaces(): UseInfiniteQueryResult<InfiniteData<SpacesPage>> {
  return useInfiniteQuery({
    queryKey: spacesQueryKeys.list,
    queryFn: ({ pageParam, signal }) => fetchSpaces(pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
  })
}

export function useSpace(slug: string | undefined): UseQueryResult<SpaceSummary> {
  return useQuery({
    queryKey: spacesQueryKeys.detail(slug ?? ''),
    queryFn: ({ signal }) => {
      if (slug === undefined) {
        throw new Error('A slug is required to read a space')
      }
      return fetchSpace(slug, signal)
    },
    enabled: slug !== undefined,
  })
}

export function useCreateSpace(): UseMutationResult<SpaceSummary, Error, CreateSpaceInput> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createSpace,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: spacesQueryKeys.list })
    },
  })
}
