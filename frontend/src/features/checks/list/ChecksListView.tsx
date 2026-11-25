import { Stack } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../../shared/ui'
import { BulkActionBar } from './BulkActionBar'
import { CheckCard } from './CheckCard'
import { ChecksFilterBar } from './ChecksFilterBar'
import { ChecksTable } from './ChecksTable'
import type { ChecksTableProps } from './ChecksTable'
import { EmptyState, ErrorState, LoadingState, NoResultsState, ResultsFooter } from './ListStates'
import type {
  BulkAction,
  CheckRowData,
  FilterFacet,
  ListFilters,
  ListStatus,
  RowAction,
  SelectionState,
} from './types'
import { useMobileList } from './useMobileList'

export interface ChecksListViewProps {
  rows: CheckRowData[]
  facets: FilterFacet[]
  filters: ListFilters
  selection: SelectionState
  totalCount: number
  status: ListStatus
  hasNextPage: boolean
  onFiltersChange: (filters: ListFilters) => void
  onSelectionChange: (selection: SelectionState) => void
  onBulkAction: (action: BulkAction) => void
  onRowAction: (id: string, action: RowAction) => void
  onLoadMore: () => void
  onRetry: () => void
  onClearFilters: () => void
  onCreateCheck?: () => void
}

function CardList(props: ChecksTableProps): ReactElement {
  return (
    <Stack gap="sm">
      {props.rows.map((row) => (
        <CheckCard
          key={row.id}
          row={row}
          rows={props.rows}
          selection={props.selection}
          onSelectionChange={props.onSelectionChange}
          onRowAction={props.onRowAction}
        />
      ))}
    </Stack>
  )
}

function ListBody(props: ChecksListViewProps & { isMobile: boolean }): ReactElement {
  if (props.status === 'loading') {
    return <LoadingState />
  }
  if (props.status === 'error') {
    return <ErrorState onRetry={props.onRetry} />
  }
  if (props.status === 'empty') {
    return <EmptyState onCreate={props.onCreateCheck} />
  }
  if (props.status === 'noResults') {
    return <NoResultsState onClearFilters={props.onClearFilters} />
  }

  const tableProps: ChecksTableProps = {
    rows: props.rows,
    selection: props.selection,
    onSelectionChange: props.onSelectionChange,
    onRowAction: props.onRowAction,
  }
  return (
    <>
      {props.isMobile ? <CardList {...tableProps} /> : <ChecksTable {...tableProps} />}
      <ResultsFooter
        totalCount={props.totalCount}
        hasNextPage={props.hasNextPage}
        onLoadMore={props.onLoadMore}
      />
    </>
  )
}

export function ChecksListView(props: ChecksListViewProps): ReactElement {
  const { t } = useTranslation()
  const isMobile = useMobileList()
  const hasSelection = props.selection.mode !== 'none'

  return (
    <PageFrame title={t('checks.list.title')}>
      <Stack gap="md">
        {hasSelection && !isMobile ? (
          <BulkActionBar
            selection={props.selection}
            totalCount={props.totalCount}
            onBulkAction={props.onBulkAction}
            onSelectionChange={props.onSelectionChange}
          />
        ) : null}
        {hasSelection ? null : (
          <ChecksFilterBar
            filters={props.filters}
            facets={props.facets}
            totalCount={props.totalCount}
            onFiltersChange={props.onFiltersChange}
          />
        )}
        <ListBody {...props} isMobile={isMobile} />
        {hasSelection && isMobile ? (
          <BulkActionBar
            selection={props.selection}
            totalCount={props.totalCount}
            onBulkAction={props.onBulkAction}
            onSelectionChange={props.onSelectionChange}
          />
        ) : null}
      </Stack>
    </PageFrame>
  )
}
