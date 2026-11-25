import { Alert, Button, Stack, Text } from '@mantine/core'
import { Link, useSearch } from '@tanstack/react-router'
import type { Dispatch, ReactElement, SetStateAction } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../shared/ui'
import { validateChecksSearch } from './api/checks.search'
import { spaceOf } from './api/checks.search'
import { BulkDeleteDialog } from './BulkDeleteDialog'
import { ChecksListView } from './list/ChecksListView'
import type { ChecksListViewProps } from './list/ChecksListView'
import type { SelectionState } from './list/types'
import { useChecksBulkActions } from './useChecksBulkActions'
import type { ChecksBulkActions } from './useChecksBulkActions'
import { useChecksData } from './useChecksData'
import type { ChecksPageData } from './useChecksData'
import { useChecksNavigation } from './useChecksNavigation'
import type { ChecksNavigation } from './useChecksNavigation'

const NO_SELECTION: SelectionState = { mode: 'none', ids: [] }

function ChecksEmptySection(): ReactElement {
  const { t } = useTranslation()
  return (
    <PageFrame title={t('checks.list.title')}>
      <Stack align="center" gap="md" py="xl">
        <Text c="dimmed" ta="center">
          {t('common.section.comingSoon')}
        </Text>
        <Button component={Link} to="/" variant="light">
          {t('common.section.backToOverview')}
        </Button>
      </Stack>
    </PageFrame>
  )
}

interface ChecksPageContentProps {
  space: string
  search: ReturnType<typeof validateChecksSearch>
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
    onFiltersChange: (filters) => {
      setSelection(NO_SELECTION)
      navigation.onFiltersChange(filters)
    },
    onSelectionChange: setSelection,
    onBulkAction: (action) => {
      bulk.onBulkAction(action, selection)
    },
    onRowAction: bulk.onRowAction,
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
  const bulk = useChecksBulkActions(space, data.apiFilters)
  const navigation = useChecksNavigation(space, search)
  const [selection, setSelection] = useState<SelectionState>(NO_SELECTION)
  const viewProps = buildViewProps({ data, bulk, navigation, selection, setSelection })
  const deleteCount = selection.mode === 'all' ? data.totalCount : selection.ids.length

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
        count={deleteCount}
        onCancel={bulk.cancelDelete}
        onConfirm={() => {
          bulk.confirmDelete(selection)
        }}
      />
    </>
  )
}

export function ChecksPage(): ReactElement {
  const search = useSearch({ strict: false })
  const space = spaceOf(search)
  if (space === undefined) {
    return <ChecksEmptySection />
  }
  return <ChecksPageContent space={space} search={validateChecksSearch(search)} />
}
