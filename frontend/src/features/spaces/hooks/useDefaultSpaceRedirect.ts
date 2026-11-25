import { getRouteApi, useNavigate, useRouterState } from '@tanstack/react-router'
import { useEffect } from 'react'
import { useSpaceList } from './useSpaceList'

const panelRoute = getRouteApi('/panel')

// When the URL has no space yet, the first space from the list becomes the
// current one so the panel always shows a concrete space. Call this once per
// layout: a second caller would start the same navigation twice.
export function useDefaultSpaceRedirect(): void {
  const search = panelRoute.useSearch()
  const requestedSlug: string | undefined = search.space
  const firstSlug = useSpaceList().spaces[0]?.slug
  const navigate = useNavigate()
  const pathname = useRouterState({ select: (state) => state.location.pathname })

  useEffect(() => {
    if (requestedSlug !== undefined || firstSlug === undefined) {
      return
    }
    void navigate({
      to: pathname,
      search: (previous) => ({ ...previous, space: firstSlug }),
      replace: true,
    })
  }, [navigate, pathname, requestedSlug, firstSlug])
}
