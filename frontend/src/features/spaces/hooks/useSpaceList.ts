import { useQueryClient } from '@tanstack/react-query'
import { spacesQueryKeys, useSpaces } from '../api/spaces.queries'
import type { SpaceSummary, SpacesStatus } from '../spaceTypes'

type SpacesQuery = ReturnType<typeof useSpaces>

export interface SpaceList {
  spaces: readonly SpaceSummary[]
  status: SpacesStatus
  hasMore: boolean
  loadMore: () => void
  retry: () => void
}

function flattenSpaces(query: SpacesQuery): SpaceSummary[] {
  return query.data?.pages.flatMap((page) => page.items) ?? []
}

function spacesStatus(query: SpacesQuery): SpacesStatus {
  if (query.isPending) {
    return 'loading'
  }
  if (query.isError) {
    return 'error'
  }
  return flattenSpaces(query).length === 0 ? 'empty' : 'ready'
}

export function useSpaceList(): SpaceList {
  const query = useSpaces()
  const queryClient = useQueryClient()

  return {
    spaces: flattenSpaces(query),
    status: spacesStatus(query),
    hasMore: query.hasNextPage,
    loadMore: () => {
      void query.fetchNextPage()
    },
    retry: () => {
      void queryClient.refetchQueries({ queryKey: spacesQueryKeys.list })
    },
  }
}
