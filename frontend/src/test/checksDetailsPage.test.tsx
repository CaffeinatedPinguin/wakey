import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import i18n from '../app/i18n'
import type { ChecksApiMock } from './checksApiMock'
import { countsFor, createChecksApiMock, makeCheck } from './checksApiMock'
import { installMockApi } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

const DESKTOP_WIDTH = 1024

let api: ChecksApiMock

function detailCheck() {
  return makeCheck({
    id: 'c1',
    lastResult: { checkedAt: '2026-11-25T11:59:48Z', state: 'DOWN', durationMs: 240 },
  })
}

beforeEach(() => {
  api = createChecksApiMock()
  installMockApi(api.handler)
  installViewport(DESKTOP_WIDTH)
})

afterEach(async () => {
  cleanup()
  installViewport(DESKTOP_WIDTH)
  await i18n.changeLanguage('pl')
})

describe('the check details page', () => {
  it('the check details page shows the check from the API', async () => {
    api.state.checks = [detailCheck()]
    api.state.counts = countsFor(api.state.checks)
    renderApp('/checks/c1?space=alpha')

    expect(await screen.findByRole('heading', { level: 1, name: 'API płatności' })).toBeDefined()
    expect(screen.getByText('https://payments.example/health')).toBeDefined()
    expect(screen.getAllByText('Nie działa').length).toBeGreaterThan(0)
    expect(screen.getByText('60 s')).toBeDefined()
  })

  it('a missing check shows the not found view', async () => {
    renderApp('/checks/missing?space=alpha')

    expect(await screen.findByRole('heading', { name: 'Nie znaleziono checka' })).toBeDefined()
  })

  it('the check details page without a space shows the missing space state', async () => {
    api.state.spaces = []
    renderApp('/checks/c1')

    expect(await screen.findByRole('heading', { name: 'Brak przestrzeni' })).toBeDefined()
    expect(screen.queryByRole('status', { name: 'Ładowanie checka…' })).toBeNull()
  })
})

describe('the check details page actions', () => {
  it('deleting a check from the details page returns to the list', async () => {
    api.state.checks = [detailCheck()]
    api.state.counts = countsFor(api.state.checks)
    const view = renderApp('/checks/c1?space=alpha')
    await screen.findByRole('heading', { level: 1, name: 'API płatności' })

    fireEvent.click(screen.getByRole('button', { name: 'Usuń' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Usuń' }))

    await waitFor(() => {
      expect(api.state.deleteCalls).toEqual(['c1'])
    })
    await waitFor(() => {
      expect(view.router.state.location.pathname).toBe('/checks')
    })
    expect(await screen.findByRole('heading', { level: 1, name: 'Checki' })).toBeDefined()
    expect(view.router.state.location.search).toMatchObject({ space: 'alpha' })
  })
})
