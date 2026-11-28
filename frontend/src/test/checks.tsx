import type { RenderResult } from '@testing-library/react'
import { vi } from 'vitest'
import { ChecksListView } from '../features/checks/list/ChecksListView'
import type {
  ChecksListActions,
  ChecksListData,
  ChecksListViewProps,
} from '../features/checks/list/ChecksListView'
import { FacetKey } from '../features/checks/list/types'
import type { CheckRowData, FilterFacet, ListFilters } from '../features/checks/list/types'
import { CheckState } from '../shared/api/generated/enums'
import { renderWithProviders } from './providers'

export const testRows: CheckRowData[] = [
  {
    id: 'c1',
    name: 'API płatności',
    target: 'https://api.example.com/health',
    type: 'HTTP',
    state: 'down',
    lastResultAt: '2026-11-25T10:00:00Z',
    intervalSeconds: 60,
    group: 'produkcja',
    tags: ['produkcja'],
  },
  {
    id: 'c2',
    name: 'Panel klienta',
    target: 'https://panel.example.com',
    type: 'HTTP',
    state: 'up',
    lastResultAt: '2026-11-25T10:01:00Z',
    intervalSeconds: 120,
    tags: ['publiczne'],
  },
]

export const testFacets: FilterFacet[] = [
  {
    key: FacetKey.STATE,
    options: [
      { value: CheckState.DOWN, label: 'Nie działa', count: 7 },
      { value: CheckState.UP, label: 'Działa', count: 219 },
    ],
  },
]

export const testFilters: ListFilters = {
  q: '',
  state: [],
  type: [],
  tag: [],
  group: [],
}

type ChecksListTestProps = ChecksListData & ChecksListActions

export interface RenderedChecksList extends RenderResult {
  viewProps: ChecksListTestProps
}

function baseProps(): ChecksListTestProps {
  return {
    rows: testRows,
    facets: testFacets,
    filters: testFilters,
    selection: { mode: 'none', ids: [] },
    totalCount: 3,
    status: 'success',
    hasActiveFilters: false,
    hasNextPage: false,
    bulkRunning: false,
    onFiltersChange: vi.fn(),
    onSelectionChange: vi.fn(),
    onBulkAction: vi.fn(),
    onRowAction: vi.fn(),
    onLoadMore: vi.fn(),
    onRetry: vi.fn(),
    onClearFilters: vi.fn(),
    onCreateCheck: vi.fn(),
  }
}

function viewProps(flat: ChecksListTestProps): ChecksListViewProps {
  const {
    rows,
    facets,
    filters,
    selection,
    totalCount,
    status,
    hasActiveFilters,
    hasNextPage,
    bulkRunning,
    ...actions
  } = flat
  return {
    data: {
      rows,
      facets,
      filters,
      selection,
      totalCount,
      status,
      hasActiveFilters,
      hasNextPage,
      bulkRunning,
    },
    actions,
  }
}

export function renderChecksList(overrides: Partial<ChecksListTestProps> = {}): RenderedChecksList {
  const flat: ChecksListTestProps = { ...baseProps(), ...overrides }
  const result = renderWithProviders(<ChecksListView {...viewProps(flat)} />)
  return { ...result, viewProps: flat }
}
