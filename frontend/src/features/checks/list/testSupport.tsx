import type { RenderResult } from '@testing-library/react'
import { vi } from 'vitest'
import { renderWithProviders } from '../../../test/providers'
import { ChecksListView } from './ChecksListView'
import type { ChecksListViewProps } from './ChecksListView'
import type { CheckRowData, FilterFacet, ListFilters } from './types'

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
    key: 'state',
    options: [
      { value: 'down', label: 'Nie działa', count: 7 },
      { value: 'up', label: 'Działa', count: 219 },
    ],
  },
  {
    key: 'type',
    options: [{ value: 'http', label: 'HTTP', count: 180 }],
  },
]

export const testFilters: ListFilters = {
  q: '',
  state: [],
  type: [],
  tag: [],
  group: [],
}

export interface RenderedChecksList extends RenderResult {
  viewProps: ChecksListViewProps
}

function baseProps(): ChecksListViewProps {
  return {
    rows: testRows,
    facets: testFacets,
    filters: testFilters,
    selection: { mode: 'none', ids: [] },
    totalCount: 3,
    status: 'ready',
    hasNextPage: false,
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

export function renderChecksList(overrides: Partial<ChecksListViewProps> = {}): RenderedChecksList {
  const viewProps: ChecksListViewProps = { ...baseProps(), ...overrides }
  const result = renderWithProviders(<ChecksListView {...viewProps} />)
  return { ...result, viewProps }
}
