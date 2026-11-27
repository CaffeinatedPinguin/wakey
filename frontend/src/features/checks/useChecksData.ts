import type { QueryStatus } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { stateFacets, toCheckRow, totalFromCounts } from './api/checks.adapters'
import type { CheckApiFilters } from './api/checks.api'
import { useCheckCounts, useChecksList } from './api/checks.queries'
import { hasActiveFilters, toApiFilters, toListFilters } from './api/checks.search'
import type { ChecksSearch } from './api/checks.search'
import type { CheckRowData, FilterFacet, ListFilters } from './list/types'

export interface ChecksPageData {
  rows: CheckRowData[]
  facets: FilterFacet[]
  filters: ListFilters
  status: QueryStatus
  hasActiveFilters: boolean
  totalCount: number
  hasNextPage: boolean
  apiFilters: CheckApiFilters
  loadMore: () => void
  retry: () => void
}

function flattenRows(query: ReturnType<typeof useChecksList>): CheckRowData[] {
  const pages = query.data?.pages ?? []
  return pages.flatMap((page) => page.items.map(toCheckRow))
}

export function useChecksData(space: string, search: ChecksSearch): ChecksPageData {
  const { t } = useTranslation()
  const filters = toListFilters(search)
  const apiFilters = toApiFilters(search)
  const list = useChecksList(space, apiFilters)
  const counts = useCheckCounts(space, apiFilters)
  const rows = flattenRows(list)
  const totalCount = totalFromCounts(counts.data) ?? rows.length
  const facets = stateFacets(counts.data, (state) => t(`common.state.${state}`))
  const reload = list.refetch
  return {
    rows,
    facets,
    filters,
    status: list.status,
    hasActiveFilters: hasActiveFilters(search),
    totalCount,
    hasNextPage: list.hasNextPage,
    apiFilters,
    loadMore: () => {
      void list.fetchNextPage()
    },
    retry: () => {
      void reload()
    },
  }
}
