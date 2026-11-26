import { getRouteApi, useNavigate, useRouterState } from '@tanstack/react-router'
import { isNotFoundException } from '../../../shared/api/client'
import { useSpace, useSpaces } from '../api/spaces.queries'
import type { SpaceSummary } from '../spaceTypes'

const panelRoute = getRouteApi('/panel')

export interface CurrentSpace {
  slug: string
  space: SpaceSummary | undefined
  isUnknown: boolean
  select: (slug: string) => void
}

type SpacesQuery = ReturnType<typeof useSpaces>
type SpaceNavigate = ReturnType<typeof useNavigate>

function firstSpaceSlug(query: SpacesQuery): string | undefined {
  return query.data?.pages[0]?.items[0]?.slug
}

function selectSpace(navigate: SpaceNavigate, pathname: string): (slug: string) => void {
  return (slug) => {
    void navigate({ to: pathname, search: (previous) => ({ ...previous, space: slug }) })
  }
}

export function useCurrentSpace(): CurrentSpace {
  const search = panelRoute.useSearch()
  const requestedSlug: string | undefined = search.space
  const list = useSpaces()
  const detail = useSpace(requestedSlug)
  const navigate = useNavigate()
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const firstSlug = firstSpaceSlug(list)

  return {
    slug: requestedSlug ?? firstSlug ?? '',
    space: detail.data,
    isUnknown: detail.isError && isNotFoundException(detail.error),
    select: selectSpace(navigate, pathname),
  }
}
