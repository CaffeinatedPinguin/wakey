import { Stack } from '@mantine/core'
import type { QueryStatus } from '@tanstack/react-query'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { BulkCheckAction } from '../../../shared/api/generated/enums'
import { PageFrame, useIsMobile } from '../../../shared/ui'
import { BulkActionBar } from './BulkActionBar'
import { CheckCard } from './CheckCard'
import { ChecksFilterBar } from './ChecksFilterBar'
import { ChecksTable } from './ChecksTable'
import type { ChecksTableProps } from './ChecksTable'
import { EmptyState, ErrorState, LoadingState, NoResultsState, ResultsFooter } from './ListStates'
import { SelectionMode } from './types'
import type { CheckRowData, FilterFacet, ListFilters, SelectionState } from './types'

export interface ChecksListData {
  rows: CheckRowData[]
  facets: FilterFacet[]
  filters: ListFilters
  selection: SelectionState
  totalCount: number
  status: QueryStatus
  hasActiveFilters: boolean
  hasNextPage: boolean
  bulkRunning: boolean
}

export interface ChecksListActions {
  onFiltersChange: (filters: ListFilters) => void
  onSelectionChange: (selection: SelectionState) => void
  onBulkAction: (action: BulkCheckAction) => void
  onRowAction: (id: string, action: BulkCheckAction) => void
  onLoadMore: () => void
  onRetry: () => void
  onClearFilters: () => void
  onCreateCheck?: () => void
}

export interface ChecksListViewProps {
  data: ChecksListData
  actions: ChecksListActions
}

interface ListBodyProps {
  data: ChecksListData
  actions: ChecksListActions
  isMobile: boolean
}

interface SelectionActionsProps {
  data: ChecksListData
  actions: ChecksListActions
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

function ListBody(props: ListBodyProps): ReactElement {
  const { data, actions } = props
  if (data.status === 'pending') {
    return <LoadingState />
  }
  if (data.status === 'error') {
    return <ErrorState onRetry={actions.onRetry} />
  }
  if (data.rows.length === 0) {
    return data.hasActiveFilters ? (
      <NoResultsState onClearFilters={actions.onClearFilters} />
    ) : (
      <EmptyState onCreate={actions.onCreateCheck} />
    )
  }

  const tableProps: ChecksTableProps = {
    rows: data.rows,
    selection: data.selection,
    onSelectionChange: actions.onSelectionChange,
    onRowAction: actions.onRowAction,
  }
  return (
    <>
      {props.isMobile ? <CardList {...tableProps} /> : <ChecksTable {...tableProps} />}
      <ResultsFooter
        totalCount={data.totalCount}
        hasNextPage={data.hasNextPage}
        onLoadMore={actions.onLoadMore}
      />
    </>
  )
}

function SelectionActions({ data, actions }: SelectionActionsProps): ReactElement {
  return (
    <BulkActionBar
      selection={data.selection}
      totalCount={data.totalCount}
      running={data.bulkRunning}
      onBulkAction={actions.onBulkAction}
      onSelectionChange={actions.onSelectionChange}
    />
  )
}

export function ChecksListView(props: ChecksListViewProps): ReactElement {
  const { t } = useTranslation()
  const isMobile = useIsMobile()
  const hasSelection = props.data.selection.mode !== SelectionMode.NONE

  return (
    <PageFrame title={t('checks.list.title')}>
      <Stack gap="md">
        {hasSelection && !isMobile ? <SelectionActions {...props} /> : null}
        {hasSelection ? null : (
          <ChecksFilterBar
            filters={props.data.filters}
            facets={props.data.facets}
            totalCount={props.data.totalCount}
            onFiltersChange={props.actions.onFiltersChange}
          />
        )}
        <ListBody {...props} isMobile={isMobile} />
        {hasSelection && isMobile ? <SelectionActions {...props} /> : null}
      </Stack>
    </PageFrame>
  )
}
