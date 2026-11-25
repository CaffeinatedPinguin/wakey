import { Alert } from '@mantine/core'
import { useNavigate, useRouterState, useSearch } from '@tanstack/react-router'
import type { Dispatch, ReactElement, SetStateAction } from 'react'
import { useEffect, useState } from 'react'
import { spaceOf, validateChecksSearch } from './api/checks.search'
import type { ChecksSearch } from './api/checks.search'

import { BulkDeleteDialog } from './BulkDeleteDialog'
import { ChecksListView } from './list/ChecksListView'
import { MissingSpaceState } from './list/ListStates'
import type { ChecksListViewProps } from './list/ChecksListView'
import type { SelectionState } from './list/types'
import { useChecksBulkActions } from './useChecksBulkActions'
import type { ChecksBulkActions } from './useChecksBulkActions'
import { useChecksData } from './useChecksData'
import type { ChecksPageData } from './useChecksData'
import { useChecksNavigation } from './useChecksNavigation'
import type { ChecksNavigation } from './useChecksNavigation'

const NO_SELECTION: SelectionState = { mode: 'none', ids: [] }

// The list knows no sort, so an old or hand-edited URL is rewritten to the
// search the route validates. The address and the API query then say the same.
function useCanonicalSearch(space: string | undefined): void {
  const navigate = useNavigate()
  const raw = useRouterState({ select: (state) => state.location.search })
  const clean = validateChecksSearch(raw)
  const unknownKey = Object.keys(raw).some((key) => key !== 'space' && !(key in clean))

  useEffect(() => {
    if (!unknownKey) {
      return
    }
    const target = space === undefined ? clean : { ...clean, space }
    void navigate({ to: '/checks', search: target, replace: true })
  }, [navigate, clean, space, unknownKey])
}

interface ChecksPageContentProps {
  space: string
  search: ChecksSearch
}

interface ViewPropsArgs {
  data: ChecksPageData
  bulk: ChecksBulkActions
  navigation: ChecksNavigation
  selection: SelectionState
  setSelection: Dispatch<SetStateAction<SelectionState>>
}

function buildViewProps(args: ViewPropsArgs): ChecksListViewProps {
  const { data, bulk, navigation, selection, setSelection } = args
  return {
    rows: data.rows,
    facets: data.facets,
    filters: data.filters,
    selection,
    totalCount: data.totalCount,
    status: data.status,
    hasNextPage: data.hasNextPage,
    bulkRunning: bulk.running,
    bulkProgress: bulk.progress,
    onFiltersChange: (filters) => {
      setSelection(NO_SELECTION)
      navigation.onFiltersChange(filters)
    },
    onSelectionChange: setSelection,
    onBulkAction: (action) => {
      bulk.onBulkAction(action, selection)
    },
    onRowAction: bulk.onRowAction,
    onCancelBulk: bulk.onCancel,
    onLoadMore: data.loadMore,
    onRetry: data.retry,
    onClearFilters: () => {
      setSelection(NO_SELECTION)
      navigation.onClearFilters()
    },
  }
}

function ChecksPageContent({ space, search }: ChecksPageContentProps): ReactElement {
  const data = useChecksData(space, search)
  const navigation = useChecksNavigation(space, search)
  const [selection, setSelection] = useState<SelectionState>(NO_SELECTION)
  const bulk = useChecksBulkActions(space, data.apiFilters, () => {
    setSelection(NO_SELECTION)
  })
  const viewProps = buildViewProps({ data, bulk, navigation, selection, setSelection })

  return (
    <>
      {bulk.message === null ? null : (
        <Alert color="yellow" mb="sm">
          {bulk.message}
        </Alert>
      )}
      <ChecksListView {...viewProps} />
      <BulkDeleteDialog
        opened={bulk.deleteOpen}
        count={bulk.deleteCount}
        onCancel={bulk.cancelDelete}
        onConfirm={bulk.confirmDelete}
      />
    </>
  )
}

export function ChecksPage(): ReactElement {
  // The /checks route validates the search with validateChecksSearch.
  const rawSearch = useSearch({ strict: false })
  const search = validateChecksSearch(rawSearch)
  const space = spaceOf(rawSearch)
  useCanonicalSearch(space)
  if (space === undefined) {
    return <MissingSpaceState />
  }
  return <ChecksPageContent space={space} search={search} />
}
