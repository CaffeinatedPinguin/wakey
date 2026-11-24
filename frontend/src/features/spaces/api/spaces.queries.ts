import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query'
import type { SpaceSummary } from '../spaceTypes'
import { createSpace, fetchSpaces } from './spaces.api'
import type { CreateSpaceInput } from './spaces.api'

export const spacesQueryKeys = {
  list: ['spaces', 'list'] as const,
}

export const spacesQueryOptions = queryOptions({
  queryKey: spacesQueryKeys.list,
  queryFn: ({ signal }) => fetchSpaces(signal),
})

export function useSpaces(): UseQueryResult<SpaceSummary[]> {
  return useQuery(spacesQueryOptions)
}

function appendSpace(
  current: SpaceSummary[] | undefined,
  space: SpaceSummary,
): SpaceSummary[] | undefined {
  if (current === undefined || current.some((item) => item.slug === space.slug)) {
    return current
  }
  return [...current, space]
}

export function useCreateSpace(): UseMutationResult<SpaceSummary, Error, CreateSpaceInput> {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createSpace,
    onSuccess: (space) => {
      queryClient.setQueryData<SpaceSummary[]>(spacesQueryKeys.list, (current) =>
        appendSpace(current, space),
      )
      void queryClient.invalidateQueries({ queryKey: spacesQueryKeys.list })
    },
  })
}
