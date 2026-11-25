import { useNavigate } from '@tanstack/react-router'
import type { ChecksSearch } from './api/checks.search'
import { clearListFilters, withListFilters } from './api/checks.search'
import type { ListFilters } from './list/types'

export interface ChecksNavigation {
  onFiltersChange: (filters: ListFilters) => void
  onClearFilters: () => void
}

export function useChecksNavigation(space: string, search: ChecksSearch): ChecksNavigation {
  const navigate = useNavigate()

  function go(next: ChecksSearch): void {
    // Replace the list filters instead of merging, so removing a filter (or
    // clearing them all) also removes it from the URL and the API query.
    const target = { ...next, space }
    void navigate({ to: '/checks', search: target })
  }

  return {
    onFiltersChange: (filters) => {
      go(withListFilters(search, filters))
    },
    onClearFilters: () => {
      go(clearListFilters())
    },
  }
}
