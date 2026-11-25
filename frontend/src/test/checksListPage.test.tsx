import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import i18n from '../app/i18n'
import type { ChecksApiMock } from './checksApiMock'
import { countsFor, createChecksApiMock, makeCheck } from './checksApiMock'
import { installMockApi } from './mockApi'
import { renderApp } from './renderApp'
import { installViewport } from './viewport'

const DESKTOP_WIDTH = 1024

let api: ChecksApiMock

function threeChecks() {
  return [
    makeCheck({ id: 'c1', name: 'API płatności' }),
    makeCheck({ id: 'c2', name: 'Panel klienta', state: 'UP', tags: ['publiczne'] }),
    makeCheck({ id: 'c3', name: 'Serwer SMTP', type: 'TCP', state: 'DEGRADED' }),
  ]
}

function installChecks(checks: ReturnType<typeof threeChecks>): void {
  api.state.checks = checks
  api.state.counts = countsFor(checks)
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

it('the checks page lists the checks of the current space from the API', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  renderApp('/checks?space=alpha')

  expect(await screen.findByText('API płatności')).toBeDefined()
  expect(screen.getByText('Panel klienta')).toBeDefined()
  expect(screen.getAllByText('Nie działa').length).toBeGreaterThan(0)
  expect(api.state.listCalls.some((url) => url.startsWith('/api/v1/spaces/alpha/checks'))).toBe(
    true,
  )
})

it('filters search and sorting live in the URL and reach the API', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&q=pay&state=down&sort=name')
  await screen.findByText('API płatności')

  await waitFor(() => {
    const reached = api.state.listCalls.some(
      (url) => url.includes('q=pay') && url.includes('state=DOWN') && url.includes('sort=name'),
    )
    expect(reached).toBe(true)
  })

  fireEvent.change(screen.getByLabelText('Szukaj'), { target: { value: 'panel' } })

  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({
      space: 'alpha',
      q: 'panel',
      state: ['down'],
    })
  })
  await waitFor(() => {
    expect(api.state.listCalls.some((url) => url.includes('q=panel'))).toBe(true)
  })
})

it('facet counts come from the counts endpoint', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  api.state.counts = { up: 2, degraded: 1, down: 3, unknown: 0 }
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  expect(await screen.findByText('3')).toBeDefined()
  expect(screen.getByText('2')).toBeDefined()
  expect(screen.getByText('1')).toBeDefined()
})

it('load more fetches the next page by cursor', async () => {
  installChecks(threeChecks())
  api.state.pageSize = 2
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('button', { name: 'Wczytaj więcej' }))

  expect(await screen.findByText('Serwer SMTP')).toBeDefined()
  expect(api.state.listCalls.some((url) => url.includes('cursor=2'))).toBe(true)
})

it('pausing selected checks calls the API for each and refreshes', async () => {
  installChecks([makeCheck({ id: 'c1' }), makeCheck({ id: 'c2', name: 'Panel klienta' })])
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')
  const initialCalls = api.state.listCalls.length

  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  await waitFor(() => {
    expect([...api.state.pauseCalls].sort()).toEqual(['c1', 'c2'])
  })
  await waitFor(() => {
    expect(api.state.listCalls.length).toBeGreaterThan(initialCalls)
  })
})

it('selecting all results acts on every page of the result', async () => {
  installChecks(threeChecks())
  api.state.pageSize = 2
  renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('checkbox', { name: 'Zaznacz stronę' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Zaznacz wszystkie 3 wyników' }))
  fireEvent.click(screen.getByRole('button', { name: 'Pauza' }))

  await waitFor(() => {
    expect([...api.state.pauseCalls].sort()).toEqual(['c1', 'c2', 'c3'])
  })
})

it('removing a filter clears it from the URL and the API query', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha&state=down')
  await screen.findByText('API płatności')

  fireEvent.click(screen.getByRole('checkbox', { name: 'Nie działa' }))

  await waitFor(() => {
    expect(view.router.state.location.search).not.toHaveProperty('state')
  })
  await waitFor(() => {
    expect(api.state.listCalls.some((url) => !url.includes('state='))).toBe(true)
  })
})

it('the checks page keeps the space in the URL', async () => {
  installChecks([makeCheck({ id: 'c1' })])
  const view = renderApp('/checks?space=alpha')
  await screen.findByText('API płatności')

  fireEvent.change(screen.getByLabelText('Szukaj'), { target: { value: 'payments' } })
  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({ space: 'alpha', q: 'payments' })
  })

  fireEvent.click(await screen.findByRole('checkbox', { name: 'Nie działa' }))
  await waitFor(() => {
    expect(view.router.state.location.search).toMatchObject({
      space: 'alpha',
      state: ['down'],
    })
  })
})
