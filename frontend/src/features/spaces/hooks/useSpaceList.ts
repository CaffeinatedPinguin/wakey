import { useQueryClient } from '@tanstack/react-query'
import { spacesQueryKeys, useSpaces } from '../api/spaces.queries'
import { SpacesStatus } from '../spaceTypes'
import type { SpaceSummary } from '../spaceTypes'

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
    return SpacesStatus.LOADING
  }
  if (query.isError) {
    return SpacesStatus.ERROR
  }
  return flattenSpaces(query).length === 0 ? SpacesStatus.EMPTY : SpacesStatus.READY
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
