import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { afterEach, beforeAll, beforeEach, expect, it } from 'vitest'
import i18n from '../../../app/i18n'
import { renderChecksList } from '../../../test/checks'
import { installChecksTranslations } from '../../../test/checksListI18n'
import { installViewport } from '../../../test/viewport'

const DESKTOP_WIDTH = 1024

beforeAll(async () => {
  await installChecksTranslations()
})

beforeEach(() => {
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

it('the checks list shows the seven columns with a state badge', () => {
  renderChecksList()
  const table = screen.getByRole('table')
  const columns = ['Stan', 'Nazwa', 'Typ', 'Interwał', 'Ostatni wynik', 'Tagi', 'Akcje']

  for (const column of columns) {
    expect(within(table).getByRole('columnheader', { name: column })).toBeDefined()
  }
  expect(within(table).getByText('Nie działa')).toBeDefined()
})

it('the filter bar shows facet counts and reports changes', () => {
  const result = renderChecksList()

  expect(screen.getByText('7')).toBeDefined()
  expect(screen.getByText('219')).toBeDefined()

  fireEvent.click(screen.getByRole('checkbox', { name: 'Nie działa' }))

  expect(result.viewProps.onFiltersChange).toHaveBeenCalledWith({
    q: '',
    state: ['DOWN'],
    type: [],
    tag: [],
    group: [],
  })
})

it('selecting the page shows a banner to select all results', () => {
  const result = renderChecksList()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))

  expect(result.viewProps.onSelectionChange).toHaveBeenCalledWith({
    mode: 'page',
    ids: ['c1', 'c2'],
  })

  cleanup()
  renderChecksList({ selection: { mode: 'page', ids: ['c1', 'c2'] } })

  expect(screen.getByText('Zaznaczono 2 na stronie')).toBeDefined()
  expect(screen.getByRole('button', { name: 'Zaznacz wszystkie 3 wyniki' })).toBeDefined()
})

it('bulk actions replace the filter bar while rows are selected', () => {
  const result = renderChecksList({ selection: { mode: 'page', ids: ['c1'] } })

  expect(screen.queryByLabelText('Szukaj')).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  expect(result.viewProps.onBulkAction).toHaveBeenCalledWith('PAUSE')

  cleanup()
  renderChecksList()

  expect(screen.getByLabelText('Szukaj')).toBeDefined()
})

it('selecting all results is reported through onSelectionChange', () => {
  const result = renderChecksList({ selection: { mode: 'page', ids: ['c1', 'c2'] } })

  fireEvent.click(screen.getByRole('button', { name: 'Zaznacz wszystkie 3 wyniki' }))

  expect(result.viewProps.onSelectionChange).toHaveBeenCalledWith({ mode: 'all', ids: [] })
})

it('the checks list explains the empty state with a next action', () => {
  const result = renderChecksList({ status: 'success', rows: [], totalCount: 0 })

  expect(screen.getByText('Brak checków')).toBeDefined()
  fireEvent.click(screen.getByRole('button', { name: 'Dodaj pierwszy check' }))

  expect(result.viewProps.onCreateCheck).toHaveBeenCalledTimes(1)
  expect(screen.queryByText('Brak checków spełniających kryteria')).toBeNull()
})

it('no results caused by filters offers to clear them', () => {
  const result = renderChecksList({ status: 'success', rows: [], hasActiveFilters: true })

  expect(screen.getByText('Brak checków spełniających kryteria')).toBeDefined()
  fireEvent.click(screen.getByRole('button', { name: 'Wyczyść filtry' }))

  expect(result.viewProps.onClearFilters).toHaveBeenCalledTimes(1)
  expect(screen.queryByText('Brak checków')).toBeNull()
})

it('the checks list shows a skeleton while loading', () => {
  const { container } = renderChecksList({ status: 'pending', rows: [], totalCount: 0 })

  expect(screen.getByRole('status', { name: 'Ładowanie checków…' })).toBeDefined()
  expect(container.querySelectorAll('.mantine-Skeleton-root').length).toBeGreaterThan(0)
})

it('the checks list offers a retry after an error', () => {
  const result = renderChecksList({ status: 'error', rows: [] })

  expect(screen.getByText('Nie udało się wczytać checków')).toBeDefined()
  expect(screen.getByLabelText('Szukaj')).toBeDefined()
  fireEvent.click(screen.getByRole('button', { name: 'Ponów' }))

  expect(result.viewProps.onRetry).toHaveBeenCalledTimes(1)
})

it('load more is offered only when there is a next page', () => {
  const result = renderChecksList({ hasNextPage: true })
  fireEvent.click(screen.getByRole('button', { name: 'Wczytaj więcej' }))

  expect(result.viewProps.onLoadMore).toHaveBeenCalledTimes(1)

  cleanup()
  renderChecksList({ hasNextPage: false })

  expect(screen.queryByRole('button', { name: 'Wczytaj więcej' })).toBeNull()
})
